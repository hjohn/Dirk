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

  private final ResolvableInjectableFactory factory;

  /**
   * Constructs a new instance.
   *
   * @param factory a {@link ResolvableInjectableFactory}, cannot be null
   */
  public AssistedClassInjectableFactoryTemplate(ResolvableInjectableFactory factory) {
    this.factory = factory;
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
    Class<?> factoryClass = TypeUtils.getRawType(type, null);
    Class<?> productType = factoryMethod.getReturnType();
    Constructor<?> productConstructor = BindingProvider.getAnnotatedConstructor(productType);
    List<Binding> productBindings = BindingProvider.ofConstructorAndMembers(productConstructor, productType);

    Interceptor interceptor = new Interceptor(productConstructor, factoryMethod, productBindings);
    Type implementedFactoryType = generateFactoryType(type, factoryClass, productType, productBindings, interceptor);

    Class<?> implementedFactoryClass = TypeUtils.getRawType(implementedFactoryType, null);
    Constructor<?> factoryConstructor = BindingProvider.getConstructor(implementedFactoryClass);
    List<Binding> factoryBindings = BindingProvider.ofConstructorAndMembers(factoryConstructor, implementedFactoryClass);

    interceptor.setFields(createProviderFieldList(productBindings, implementedFactoryClass));

    factoryInjectable = factory.create(
      implementedFactoryType,
      Annotations.findDirectlyMetaAnnotatedAnnotations(factoryClass, QUALIFIER),
      factoryBindings,
      Annotations.of(Singleton.class),
      null,
      new ClassObjectFactory(factoryConstructor)
    );

    PRODUCER_INJECTABLES.put(type, factoryInjectable);

    return factoryInjectable;
  }

  private Type generateFactoryType(Type type, Class<?> factoryClass, Class<?> productType, List<Binding> productBindings, Interceptor interceptor) {
    Builder<?> builder = new ByteBuddy()
      .subclass(type, ConstructorStrategy.Default.IMITATE_SUPER_CLASS.withInheritedAnnotations())
      .method(ElementMatchers.returns(productType).and(ElementMatchers.isAbstract()))
      .intercept(MethodDelegation.to(interceptor));

    for(int i = 0; i < productBindings.size(); i++) {
      Binding binding = productBindings.get(i);

      Argument annotation = binding.getParameter() == null ? binding.getAccessibleObject().getAnnotation(Argument.class) : binding.getParameter().getAnnotation(Argument.class);

      if(annotation == null) {
        List<Annotation> annotations = Arrays.asList(binding.getParameter() == null ? binding.getAccessibleObject().getAnnotations() : binding.getParameter().getAnnotations());

        if(binding.getParameter() != null) {
          annotations = new ArrayList<>(annotations);

          annotations.add(INJECT);
        }

        builder = builder
          .defineField("__binding" + i + "__", TypeUtils.parameterize(Provider.class, binding.getType()), Visibility.PRIVATE)
          .annotateField(annotations);
      }
    }

    return builder
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
  }

  private static List<Field> createProviderFieldList(List<Binding> productBindings, Class<?> implementedFactoryClass) {
    try {
      List<Field> providerFields = new ArrayList<>();

      for(int i = 0; i < productBindings.size(); i++) {
        Binding binding = productBindings.get(i);
        Argument annotation = binding.getParameter() == null ? binding.getAccessibleObject().getAnnotation(Argument.class) : binding.getParameter().getAnnotation(Argument.class);

        if(annotation == null) {
          Field field = implementedFactoryClass.getDeclaredField("__binding" + i + "__");

          field.setAccessible(true);
          providerFields.add(field);
        }
        else {
          providerFields.add(null);
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
    private final Type type;
    private final ObjectFactory objectFactory;
    private final List<Binding> bindings;
    private final List<String> factoryParameterNames;
    private final List<String> bindingParameterNames = new ArrayList<>();  // has null gaps where dependencies are

    private List<Field> fields;  // has null gaps where parameters are

    Interceptor(Constructor<?> productConstructor, Method factoryMethod, List<Binding> bindings) {
      Map<String, Binding> productArgumentTypes = createBindingNameMap(bindings);
      Map<Binding, String> bindingNames = productArgumentTypes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

      this.type = factoryMethod.getReturnType();
      this.objectFactory = new ClassObjectFactory(productConstructor);
      this.bindings = bindings;
      this.factoryParameterNames = validateProducerAndReturnArgumentNames(factoryMethod, productArgumentTypes);

      for(Binding binding : bindings) {
        bindingParameterNames.add(bindingNames.get(binding));
      }
    }

    void setFields(List<Field> fields) {
      this.fields = fields;
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
        throw new InstanceCreationFailure(type, "Exception while creating instance", e);
      }
    }

    private List<Injection> createInjections(Object factoryInstance, Map<String, Object> parameters) throws IllegalAccessException {
      List<Injection> injections = new ArrayList<>();

      for(int i = 0; i < bindings.size(); i++) {
        Binding binding = bindings.get(i);
        String name = bindingParameterNames.get(i);

        @SuppressWarnings("unchecked")
        Object value = name != null ? parameters.get(name)
               : binding.isDirect() ? ((Provider<Object>)fields.get(i).get(factoryInstance)).get()
                                    : fields.get(i).get(factoryInstance);

        injections.add(new Injection(binding.getAccessibleObject(), value));
      }

      return injections;
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

  private static Map<String, Binding> createBindingNameMap(List<Binding> bindings) {
    Map<AccessibleObject, List<Binding>> map = bindings.stream().collect(Collectors.groupingBy(Binding::getAccessibleObject));
    Map<String, Binding> parameterBindings = new HashMap<>();

    for(List<Binding> group : map.values()) {
      for(int i = 0; i < group.size(); i++) {
        Binding binding = group.get(i);
        AccessibleObject accessibleObject = binding.getAccessibleObject();
        Parameter parameter = binding.getParameter();
        Argument annotation = parameter == null ? accessibleObject.getAnnotation(Argument.class) : parameter.getAnnotation(Argument.class);

        if(annotation != null) {
          String name = parameter == null ? determineArgumentName(annotation, (Field)accessibleObject) : determineArgumentName(parameter);

          if(name == null) {
            throw new BindingException("Missing argument name. Name cannot be determined for [" + accessibleObject + "] parameter [" + parameter + "]; specify one with @Argument or compile classes with parameter name information");
          }

          parameterBindings.put(name, binding);
        }
      }
    }

    return parameterBindings;
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
