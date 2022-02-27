package hs.ddif.core.config.standard;

import hs.ddif.annotations.Argument;
import hs.ddif.core.definition.InjectableFactory;
import hs.ddif.core.definition.ClassInjectableFactoryTemplate;
import hs.ddif.core.definition.DefinitionException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.definition.bind.BindingException;
import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.factory.ClassObjectFactory;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.instantiation.injection.ObjectFactory;
import hs.ddif.core.util.Primitives;
import hs.ddif.core.util.Types;

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
import java.util.function.Function;
import java.util.stream.Collectors;

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
 * Template to construct {@link Injectable}s from abstract types to
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
 * The factory method can be annotated with a scope, while the product always has no scope.
 * This means that each invocation of the factory method will yield a new product
 * instance.
 *
 * <p>Factories can have qualifiers at the type level. These will be taken into
 * account when injecting the factory at a target site. Products never have any
 * qualifiers.
 */
public class AssistedClassInjectableFactoryTemplate implements ClassInjectableFactoryTemplate<AssistedClassInjectableFactoryTemplate.Context> {
  private static final Map<Type, Injectable> PRODUCER_INJECTABLES = new WeakHashMap<>();

  private final Annotation inject;
  private final Class<?> providerClass;
  private final Function<Object, Object> providerGetter;
  private final BindingProvider bindingProvider;
  private final InjectableFactory injectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param <P> the type of the provider class used
   * @param bindingProvider a {@link BindingProvider}, cannot be {@code null}
   * @param injectableFactory a {@link InjectableFactory}, cannot be {@code null}
   * @param inject an inject {@link Annotation} to use for generated classes, cannot be {@code null}
   * @param providerClass a provider {@link Class} to subclass for generated classes, cannot be {@code null}
   * @param providerGetter a getter {@link Function} of the given provider class, cannot be {@code null}
   */
  @SuppressWarnings("unchecked")
  public <P> AssistedClassInjectableFactoryTemplate(BindingProvider bindingProvider, InjectableFactory injectableFactory, Annotation inject, Class<P> providerClass, Function<P, Object> providerGetter) {
    this.inject = inject;
    this.providerClass = providerClass;
    this.providerGetter = (Function<Object, Object>)providerGetter;
    this.bindingProvider = bindingProvider;
    this.injectableFactory = injectableFactory;
  }

  @Override
  public TypeAnalysis<Context> analyze(Type type) {
    Injectable factoryInjectable = PRODUCER_INJECTABLES.get(type);

    if(factoryInjectable != null) {
      return TypeAnalysis.positive(new Context(type, null));
    }

    Class<?> factoryClass = Types.raw(type);
    Method factoryMethod = findFactoryMethod(factoryClass);

    if(factoryMethod == null) {
      return TypeAnalysis.negative("Type must have a single abstract method to qualify for assisted injection: %1$s");
    }
    if(factoryMethod.getParameterCount() == 0) {
      return TypeAnalysis.negative("Factory method must have at least one parameter to qualify for assisted injection: %1$s");
    }

    Type returnType = TypeUtils.unrollVariables(TypeUtils.getTypeArguments(type, factoryMethod.getDeclaringClass()), factoryMethod.getGenericReturnType());
    Class<?> returnClass = Types.raw(returnType);

    if(returnClass.isPrimitive()) {
      return TypeAnalysis.negative("Factory method cannot return a primitive type: %2$s in: %1$s", factoryMethod);
    }
    if(Modifier.isAbstract(returnClass.getModifiers())) {
      return TypeAnalysis.negative("Factory method cannot return an abstract type: %2$s in: %1$s", factoryMethod);
    }

    return TypeAnalysis.positive(new Context(type, factoryMethod));
  }

  @Override
  public Injectable create(TypeAnalysis<Context> analysis) throws BindingException {
    Type type = analysis.getData().type;
    Injectable factoryInjectable = PRODUCER_INJECTABLES.get(type);

    if(factoryInjectable != null) {
      return factoryInjectable;
    }

    Method factoryMethod = analysis.getData().factoryMethod;
    Class<?> implementedFactoryClass = generateFactoryClass(type, factoryMethod);

    Constructor<?> factoryConstructor = bindingProvider.getConstructor(implementedFactoryClass);
    List<Binding> factoryBindings = bindingProvider.ofConstructorAndMembers(factoryConstructor, implementedFactoryClass);

    factoryInjectable = injectableFactory.create(implementedFactoryClass, implementedFactoryClass, factoryBindings, new ClassObjectFactory(factoryConstructor));

    PRODUCER_INJECTABLES.put(type, factoryInjectable);

    return factoryInjectable;
  }

  private Class<?> generateFactoryClass(Type type, Method factoryMethod) throws BindingException {
    Class<?> productType = factoryMethod.getReturnType();
    Constructor<?> productConstructor = bindingProvider.getAnnotatedConstructor(productType);
    Interceptor interceptor = new Interceptor(productConstructor, factoryMethod, providerGetter);

    /*
     * Construct ByteBuddy builder:
     */

    Builder<?> builder = new ByteBuddy()
      .subclass(type, ConstructorStrategy.Default.IMITATE_SUPER_CLASS.withInheritedAnnotations())
      .annotateType(Types.raw(type).getDeclaredAnnotations())
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

          annotations.add(inject);
        }

        providerFieldNames.add(fieldName);
        builder = builder
          .defineField(fieldName, TypeUtils.parameterize(providerClass, binding.getType()), Visibility.PRIVATE)
          .annotateField(annotations);
      }
      else {
        String name = parameter == null ? determineArgumentName(annotation, (Field)accessibleObject) : determineArgumentName(parameter);

        if(name == null) {
          throw new DefinitionException(accessibleObject, "unable to determine argument name for parameter [" + parameter + "]; specify one with @Argument or compile classes with parameter name information");
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
    private final Function<Object, Object> providerGetter;

    private List<String> factoryParameterNames;

    Interceptor(Constructor<?> productConstructor, Method factoryMethod, Function<Object, Object> providerGetter) {
      this.factoryMethod = factoryMethod;
      this.providerGetter = providerGetter;
      this.objectFactory = new ClassObjectFactory(productConstructor);
    }

    void initialize(Map<String, Binding> productArgumentTypes, List<Binding> bindings, List<Field> fields) {
      Map<Binding, String> bindingNames = productArgumentTypes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

      this.factoryParameterNames = validateProducerAndReturnArgumentNames(factoryMethod, productArgumentTypes);

      for(int i = 0; i < bindings.size(); i++) {
        Binding binding = bindings.get(i);

        templates.add(new InjectionTemplate(fields.get(i), binding.getAccessibleObject(), bindingNames.get(binding)));
      }
    }

    /**
     * Called when a factory method call is intercepted.
     *
     * @param factoryInstance the factory instance, cannot be {@code null}
     * @param args an array of all arguments passed to the factory method, never {@code null}
     * @return the product of the factory, never {@code null}
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
        Object value = template.field == null ? parameters.get(template.parameterName) : providerGetter.apply(template.field.get(factoryInstance));

        injections.add(new Injection(template.accessibleObject, value));
      }

      return injections;
    }

    static class InjectionTemplate {
      final Field field;  // null when it's a parameter
      final AccessibleObject accessibleObject;
      final String parameterName;  // null when it's not a parameter

      InjectionTemplate(Field field, AccessibleObject accessibleObject, String parameterName) {
        this.field = field;
        this.accessibleObject = accessibleObject;
        this.parameterName = parameterName;
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
      throw new DefinitionException(factoryMethod, "should have " + argumentBindings.size() + " argument(s) of types: " + argumentBindings.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getType())));
    }

    Type[] genericParameterTypes = factoryMethod.getGenericParameterTypes();
    Parameter[] parameters = factoryMethod.getParameters();

    for(int i = 0; i < parameters.length; i++) {
      String name = determineArgumentName(parameters[i]);

      if(name == null) {
        throw new DefinitionException(factoryMethod, "is missing argument name for [" + parameters[i] + "]; specify one with @Argument or compile classes with parameter name information");
      }

      if(!argumentBindings.containsKey(name)) {
        throw new DefinitionException(factoryMethod, "is missing required argument with name: " + name);
      }

      Type factoryArgumentType = Primitives.toBoxed(genericParameterTypes[i]);

      if(!Types.raw(argumentBindings.get(name).getType()).equals(Types.raw(factoryArgumentType))) {
        throw new DefinitionException(factoryMethod, "has argument [" + parameters[i] + "] with name '" + name + "' that should be of type [" + argumentBindings.get(name).getType() + "] but was: " + factoryArgumentType);
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
