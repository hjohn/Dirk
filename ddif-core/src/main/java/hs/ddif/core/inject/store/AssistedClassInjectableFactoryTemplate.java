package hs.ddif.core.inject.store;

import hs.ddif.annotations.Argument;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.Injection;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.ObjectFactory;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.Annotations;
import hs.ddif.core.util.Primitives;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Template to construct {@link ResolvableInjectable}s from abstract types to
 * provide support for assisted injection.
 *
 * <p>Assisted injection automatically creates a Factory which produces a Product.
 *
 * <p>Suitable candidates for products are regular concrete classes which may have
 * dependencies of their own and a number of arguments which must be supplied
 * at construction time. For example:
 *
 * <pre>
 * class Vehicle {
 *     {@literal @}Inject
 *     Vehicle(
 *         Engine engine,  // supplied by injector
 *         {@literal @}Argument int numberOfWheels  // supplied as argument
 *     ) { ... }
 * }</pre>
 *
 * <p>Suitable candidates for factories are abstract types with a single unimplemented
 * method which returns a suitable product class. Any arguments the factory method
 * accepts must exactly match the arguments the product class needs. A matching
 * factory for the above example would be:
 *
 * <pre>
 * interface VehicleFactory {
 *     Vehicle createVehicle(int numberOfWheels);
 * }</pre>
 *
 * Note that the name of the arguments can only be retrieved with reflection if
 * the classes are compiled with parameter name information. The {@link Argument}
 * annotation can be used to specify names explicitly or to override them.
 *
 * <h2>Scopes and Qualifiers</h2>
 *
 * The factory always has {@link Singleton} scope, while the product has no scope.
 * This means that each invocation of the factory method will yield a new product
 * instance.
 *
 * <p>Factories can have qualifiers at the type level. These will be taken into
 * account when injecting the factory at a target site. Products never have any
 * qualifiers.
 */
public class AssistedClassInjectableFactoryTemplate implements ClassInjectableFactoryTemplate<AssistedClassInjectableFactoryTemplate.Context> {
  private static final Map<Type, ResolvableInjectable> PRODUCER_INJECTABLES = new WeakHashMap<>();
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);
  private static final Annotation INJECT = Annotations.of(Inject.class);

  private final BindingProvider bindingProvider;
  private final ResolvableInjectableFactory injectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param bindingProvider a {@link BindingProvider}, cannot be null
   * @param injectableFactory a {@link ResolvableInjectableFactory}, cannot be null
   */
  public AssistedClassInjectableFactoryTemplate(BindingProvider bindingProvider, ResolvableInjectableFactory injectableFactory) {
    this.bindingProvider = bindingProvider;
    this.injectableFactory = injectableFactory;
  }

  @Override
  public TypeAnalysis<Context> analyze(Type type) {
    ResolvableInjectable factoryInjectable = PRODUCER_INJECTABLES.get(type);

    if(factoryInjectable != null) {
      return TypeAnalysis.positive(new Context(type, null));
    }

    Class<?> factoryClass = TypeUtils.getRawType(type, null);
    Method factoryMethod = findFactoryMethod(factoryClass);

    if(factoryMethod == null) {
      return TypeAnalysis.negative("Type must have a single abstract method to qualify for assisted injection: %1$s");
    }
    if(factoryMethod.getReturnType().isPrimitive()) {
      return TypeAnalysis.negative("Factory method cannot return a primitive type: %2$s in: %1$s", factoryMethod);
    }
    if(Modifier.isAbstract(factoryMethod.getReturnType().getModifiers())) {
      return TypeAnalysis.negative("Factory method cannot return an abstract type: %2$s in: %1$s", factoryMethod);
    }

    return TypeAnalysis.positive(new Context(type, factoryMethod));
  }

  @Override
  public ResolvableInjectable create(TypeAnalysis<Context> analysis) {
    Type type = analysis.getData().type;
    ResolvableInjectable factoryInjectable = PRODUCER_INJECTABLES.get(type);

    if(factoryInjectable != null) {
      return factoryInjectable;
    }

    Method factoryMethod = analysis.getData().factoryMethod;
    Class<?> implementedFactoryClass = generateFactoryClass(type, factoryMethod);

    Constructor<?> factoryConstructor = bindingProvider.getConstructor(implementedFactoryClass);
    List<Binding> factoryBindings = bindingProvider.ofConstructorAndMembers(factoryConstructor, implementedFactoryClass);

    factoryInjectable = injectableFactory.create(
      implementedFactoryClass,
      Annotations.findDirectlyMetaAnnotatedAnnotations(TypeUtils.getRawType(type, null), QUALIFIER),
      factoryBindings,
      Annotations.of(Singleton.class),
      null,
      new ClassObjectFactory(factoryConstructor)
    );

    PRODUCER_INJECTABLES.put(type, factoryInjectable);

    return factoryInjectable;
  }

  private Class<?> generateFactoryClass(Type type, Method factoryMethod) {
    Class<?> productType = factoryMethod.getReturnType();
    Constructor<?> productConstructor = bindingProvider.getAnnotatedConstructor(productType);
    Interceptor interceptor = new Interceptor(productConstructor, factoryMethod);

    /*
     * Construct ByteBuddy builder:
     */

    Builder<?> builder = new ByteBuddy()
      .subclass(type, ConstructorStrategy.Default.IMITATE_SUPER_CLASS.withInheritedAnnotations())
      .method(ElementMatchers.returns(productType).and(ElementMatchers.isAbstract()))
      .intercept(MethodDelegation.to(interceptor));

    /*
     * Add a field per binding to the builder:
     */

    List<String> providerFieldNames = new ArrayList<>();
    Map<String, Binding> parameterBindings = new HashMap<>();
    List<Binding> productBindings = bindingProvider.ofConstructorAndMembers(productConstructor, productType);

    for(int i = 0; i < productBindings.size(); i++) {
      Binding binding = productBindings.get(i);
      Parameter parameter = binding.getParameter();
      AccessibleObject accessibleObject = binding.getAccessibleObject();
      Argument annotation = parameter == null ? accessibleObject.getAnnotation(Argument.class) : parameter.getAnnotation(Argument.class);

      if(annotation == null) {
        List<Annotation> annotations = Arrays.asList(parameter == null ? accessibleObject.getAnnotations() : parameter.getAnnotations());
        String fieldName = "__binding" + i + "__";

        if(parameter != null) {
          annotations = new ArrayList<>(annotations);

          annotations.add(INJECT);
        }

        providerFieldNames.add(fieldName);
        builder = builder
          .defineField(fieldName, TypeUtils.parameterize(Provider.class, binding.getType()), Visibility.PRIVATE)
          .annotateField(annotations);
      }
      else {
        String name = parameter == null ? determineArgumentName(annotation, (Field)accessibleObject) : determineArgumentName(parameter);

        if(name == null) {
          throw new BindingException("Missing argument name. Name cannot be determined for [" + accessibleObject + "] parameter [" + parameter + "]; specify one with @Argument or compile classes with parameter name information");
        }

        parameterBindings.put(name, binding);
        providerFieldNames.add(null);
      }
    }

    /*
     * Generate the factory:
     */

    Class<?> factoryClass = TypeUtils.getRawType(type, null);
    Class<?> cls = builder
      .make()
      .load(getClass().getClassLoader(), ClassLoadingStrategy.UsingLookup.withFallback(() -> {
        try {
          return MethodHandles.privateLookupIn(factoryClass, MethodHandles.lookup());
        }
        catch(IllegalAccessException e) {
          return MethodHandles.lookup();
        }
      }))
      .getLoaded();

    /*
     * Set the field list on the factory:
     */

    List<Field> providerFields = createProviderFields(cls, providerFieldNames);

    interceptor.initialize(parameterBindings, productBindings, providerFields);

    return cls;
  }

  private static List<Field> createProviderFields(Class<?> cls, List<String> providerFieldNames) {
    try {
      List<Field> providerFields = new ArrayList<>();

      for(String fieldName : providerFieldNames) {
        if(fieldName == null) {
          providerFields.add(null);
        }
        else {
          Field field = cls.getDeclaredField(fieldName);

          field.setAccessible(true);
          providerFields.add(field);
        }
      }

      return providerFields;
    }
    catch(NoSuchFieldException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Interceptor for generated subclass of assisted injection factories.
   */
  public static class Interceptor {
    private final ObjectFactory objectFactory;
    private final List<InjectionTemplate> templates = new ArrayList<>();
    private final Method factoryMethod;

    private List<String> factoryParameterNames;

    Interceptor(Constructor<?> productConstructor, Method factoryMethod) {
      this.factoryMethod = factoryMethod;
      this.objectFactory = new ClassObjectFactory(productConstructor);
    }

    void initialize(Map<String, Binding> productArgumentTypes, List<Binding> bindings, List<Field> fields) {
      Map<Binding, String> bindingNames = productArgumentTypes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

      this.factoryParameterNames = validateProducerAndReturnArgumentNames(factoryMethod, productArgumentTypes);

      for(int i = 0; i < bindings.size(); i++) {
        Binding binding = bindings.get(i);

        templates.add(new InjectionTemplate(fields.get(i), binding.getAccessibleObject(), bindingNames.get(binding), binding.isDirect()));
      }
    }

    /**
     * Called when a factory method call is intercepted.
     *
     * @param factoryInstance the factory instance, cannot be null
     * @param args an array of all arguments passed to the factory method, never null
     * @return the product of the factory, never null
     * @throws InstanceCreationFailure when the product could not be created
     */
    @RuntimeType
    public Object intercept(@This Object factoryInstance, @AllArguments Object[] args) throws InstanceCreationFailure {
      try {
        Map<String, Object> parameters = new HashMap<>();

        for(int i = 0; i < args.length; i++) {
          parameters.put(factoryParameterNames.get(i), args[i]);
        }

        return objectFactory.createInstance(createInjections(factoryInstance, parameters));
      }
      catch(Exception e) {
        throw new InstanceCreationFailure(factoryMethod.getReturnType(), "Exception while creating instance", e);
      }
    }

    private List<Injection> createInjections(Object factoryInstance, Map<String, Object> parameters) throws IllegalAccessException {
      List<Injection> injections = new ArrayList<>();

      for(InjectionTemplate template : templates) {
        @SuppressWarnings("unchecked")
        Object value = template.field == null ? parameters.get(template.parameterName)
                          : template.isDirect ? ((Provider<Object>)template.field.get(factoryInstance)).get()
                                              : template.field.get(factoryInstance);

        injections.add(new Injection(template.accessibleObject, value));
      }

      return injections;
    }

    static class InjectionTemplate {
      final Field field;  // null when it's a parameter
      final AccessibleObject accessibleObject;
      final String parameterName;  // null when it's not a parameter
      final boolean isDirect;

      InjectionTemplate(Field field, AccessibleObject accessibleObject, String parameterName, boolean isDirect) {
        this.field = field;
        this.accessibleObject = accessibleObject;
        this.parameterName = parameterName;
        this.isDirect = isDirect;
      }
    }
  }

  private static Method findFactoryMethod(Class<?> producerClass) {
    Method factoryMethod = null;
    int abstractMethodCount = 0;

    for(Method method : producerClass.getMethods()) {
      if(Modifier.isAbstract(method.getModifiers())) {
        factoryMethod = method;
        abstractMethodCount++;

        if(abstractMethodCount > 1) {
          return null;
        }
      }
    }

    return factoryMethod;
  }

  private static List<String> validateProducerAndReturnArgumentNames(Method factoryMethod, Map<String, Binding> argumentBindings) {
    List<String> names = new ArrayList<>();

    if(factoryMethod.getParameterCount() != argumentBindings.size()) {
      throw new BindingException("Factory method has wrong number of arguments: [" + factoryMethod + "] should have " + argumentBindings.size() + " argument(s) of types: " + argumentBindings.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getType())));
    }

    Type[] genericParameterTypes = factoryMethod.getGenericParameterTypes();
    Parameter[] parameters = factoryMethod.getParameters();

    for(int i = 0; i < parameters.length; i++) {
      String name = determineArgumentName(parameters[i]);

      if(name == null) {
        throw new BindingException("Missing argument name: [" + factoryMethod + "] is missing name for [" + parameters[i] + "]; specify one with @Argument or compile classes with parameter name information");
      }

      if(!argumentBindings.containsKey(name)) {
        throw new BindingException("Factory method is missing required argument: [" + factoryMethod + "] is missing required argument with name: " + name);
      }

      Type factoryArgumentType = Primitives.toBoxed(genericParameterTypes[i]);

      if(!argumentBindings.get(name).getType().equals(factoryArgumentType)) {
        throw new BindingException("Factory method has argument of wrong type: [" + factoryMethod + "] has argument [" + parameters[i] + "] with name '" + name + "' that should be of type [" + argumentBindings.get(name).getType() + "] but was: " + factoryArgumentType);
      }

      names.add(name);
    }

    return names;
  }

  private static String determineArgumentName(Argument annotation, Field field) {
    return !annotation.value().isEmpty() ? annotation.value() : field.getName();
  }

  private static String determineArgumentName(Parameter parameter) {
    Argument annotation = parameter.getAnnotation(Argument.class);

    if((annotation == null || annotation.value().isEmpty()) && !parameter.isNamePresent()) {
      return null;
    }

    return annotation != null && !annotation.value().isEmpty() ? annotation.value() : parameter.getName();
  }

  static class Context {
    final Type type;
    final Method factoryMethod;

    Context(Type type, Method factoryMethod) {
      this.type = type;
      this.factoryMethod = factoryMethod;
    }
  }
}
