package hs.ddif.extensions.assisted;

import hs.ddif.core.config.standard.InjectableExtension;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.DefinitionException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.util.Primitives;
import hs.ddif.core.util.Types;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

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
 * Extension which provides implementations of abstract classes or interfaces annotated with
 * the configured assist annotation. The types must have a single abstract method with a
 * concrete, non primitive return type.
 */
public class AssistedInjectableExtension implements InjectableExtension {
  private static final Map<Type, Injectable<?>> PRODUCER_INJECTABLES = new WeakHashMap<>();

  private final ClassInjectableFactory injectableFactory;
  private final AssistedAnnotationStrategy<?> strategy;

  /**
   * Constructs a new instance.
   *
   * @param injectableFactory a {@link ClassInjectableFactory}, cannot be {@code null}
   * @param strategy an {@link AssistedAnnotationStrategy}, cannot be {@code null}
   */
  public AssistedInjectableExtension(ClassInjectableFactory injectableFactory, AssistedAnnotationStrategy<?> strategy) {
    this.injectableFactory = injectableFactory;
    this.strategy = strategy;
  }

  @Override
  public List<Injectable<?>> getDerived(Type type) {
    Injectable<?> injectable = PRODUCER_INJECTABLES.get(type);

    if(injectable != null) {
      return List.of(injectable);
    }

    Class<?> factoryClass = Types.raw(type);

    if(!factoryClass.isAnnotationPresent(strategy.assistedAnnotationClass())) {
      return List.of();
    }

    Method factoryMethod = findFactoryMethod(factoryClass);

    if(factoryMethod == null) {
      throw new DefinitionException(factoryClass, "must have a single abstract method to qualify for assisted injection");
    }

    Type returnType = Types.resolveVariables(Types.getTypeArguments(type, factoryMethod.getDeclaringClass()), factoryMethod.getGenericReturnType());

    if(returnType == null) {
      throw new DefinitionException(factoryMethod, "must not have unresolvable type variables to qualify for assisted injection: " + Arrays.toString(factoryClass.getTypeParameters()));
    }

    Class<?> returnClass = Types.raw(returnType);

    if(returnClass.isPrimitive()) {
      throw new DefinitionException(factoryMethod, "must not return a primitive type to qualify for assisted injection");
    }
    if(Modifier.isAbstract(returnClass.getModifiers())) {
      throw new DefinitionException(factoryMethod, "must return a concrete type to qualify for assisted injection");
    }

    return List.of(create(type, factoryMethod));
  }

  public Injectable<?> create(Type type, Method factoryMethod) {
    Injectable<?> factoryInjectable = injectableFactory.create(generateFactoryClass(type, factoryMethod));

    PRODUCER_INJECTABLES.put(type, factoryInjectable);

    return factoryInjectable;
  }

  private Class<?> generateFactoryClass(Type type, Method factoryMethod) {
    Class<?> productType = factoryMethod.getReturnType();
    Injectable<?> productInjectable = injectableFactory.create(productType);
    Interceptor<?> interceptor = new Interceptor<>(productInjectable, factoryMethod, strategy);

    /*
     * Construct ByteBuddy builder:
     */

    List<Annotation> declaredAnnotations = Arrays.stream(Types.raw(type).getDeclaredAnnotations()).filter(a -> !a.annotationType().equals(strategy.assistedAnnotationClass())).collect(Collectors.toList());

    Builder<?> builder = new ByteBuddy()
      .subclass(type, ConstructorStrategy.Default.IMITATE_SUPER_CLASS.withInheritedAnnotations())
      .annotateType(declaredAnnotations)
      .method(ElementMatchers.returns(productType).and(ElementMatchers.isAbstract()))
      .intercept(MethodDelegation.to(interceptor));

    /*
     * Add a field per binding to the builder:
     */

    List<String> providerFieldNames = new ArrayList<>();
    Map<String, Binding> parameterBindings = new HashMap<>();
    List<Binding> productBindings = productInjectable.getBindings();

    for(int i = 0; i < productBindings.size(); i++) {
      Binding binding = productBindings.get(i);
      Parameter parameter = binding.getParameter();
      AccessibleObject accessibleObject = binding.getAccessibleObject();

      if(strategy.isArgument(binding.getAnnotatedElement())) {
        try {
          String name = parameter == null ? strategy.determineArgumentName(accessibleObject) : strategy.determineArgumentName(parameter);

          if(parameterBindings.put(name, binding) != null) {
            throw new DefinitionException(accessibleObject, "has a duplicate argument name: " + name);
          }

          providerFieldNames.add(null);
        }
        catch(MissingArgumentException e) {
          throw new DefinitionException(accessibleObject, "unable to determine argument name" + (parameter == null ? "" : " for parameter [" + parameter + "]"), e);
        }
      }
      else {
        List<Annotation> annotations = Arrays.asList(binding.getAnnotatedElement().getAnnotations());
        String fieldName = "__binding" + i + "__" + toString(binding) + "__";

        if(parameter != null) {
          annotations = new ArrayList<>(annotations);

          annotations.add(strategy.injectAnnotation());
        }

        providerFieldNames.add(fieldName);
        builder = builder
          .defineField(fieldName, Types.parameterize(strategy.providerClass(), binding.getType()), Visibility.PRIVATE)
          .annotateField(annotations);
      }
    }

    /*
     * Generate the factory:
     */

    Class<?> factoryClass = Types.raw(type);
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

  public String toString(Binding binding) {
    AccessibleObject accessibleObject = binding.getAccessibleObject();
    Parameter parameter = binding.getParameter();

    if(accessibleObject instanceof Executable) {
      int index = Arrays.asList(((Executable)accessibleObject).getParameters()).indexOf(parameter);
      String name = accessibleObject instanceof Method ? "Method_" + ((Method)accessibleObject).getName() : "Constructor";

      return name + "_Parameter_" + index;
    }
    else if(accessibleObject instanceof Field) {
      return "Field_" + ((Field)accessibleObject).getName();
    }

    return "OwnerClass";
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
   *
   * @param <P> the provider class
   */
  public static class Interceptor<P> {
    private final Injectable<?> productInjectable;
    private final List<InjectionTemplate> templates = new ArrayList<>();
    private final Method factoryMethod;
    private final AssistedAnnotationStrategy<P> strategy;

    private List<String> factoryParameterNames;

    Interceptor(Injectable<?> productInjectable, Method factoryMethod, AssistedAnnotationStrategy<P> strategy) {
      this.productInjectable = productInjectable;
      this.factoryMethod = factoryMethod;
      this.strategy = strategy;
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

        return productInjectable.create(createInjections(factoryInstance, parameters));
      }
      catch(Exception e) {
        throw new InstanceCreationFailure(factoryMethod.getReturnType(), "Exception while creating instance", e);
      }
    }

    private List<Injection> createInjections(Object factoryInstance, Map<String, Object> parameters) throws IllegalAccessException {
      List<Injection> injections = new ArrayList<>();

      for(InjectionTemplate template : templates) {
        @SuppressWarnings("unchecked")
        Object value = template.field == null ? parameters.get(template.parameterName) : strategy.provision((P)template.field.get(factoryInstance));

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

    private List<String> validateProducerAndReturnArgumentNames(Method factoryMethod, Map<String, Binding> argumentBindings) {
      List<String> names = new ArrayList<>();

      if(factoryMethod.getParameterCount() != argumentBindings.size()) {
        throw new DefinitionException(factoryMethod, "should have " + argumentBindings.size() + " argument(s) of types: " + argumentBindings.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getType())));
      }

      Type[] genericParameterTypes = factoryMethod.getGenericParameterTypes();
      Parameter[] parameters = factoryMethod.getParameters();
      Set<String> encounteredNames = new HashSet<>();

      for(int i = 0; i < parameters.length; i++) {
        try {
          String name = strategy.determineArgumentName(parameters[i]);

          if(!argumentBindings.containsKey(name)) {
            throw new DefinitionException(factoryMethod, "is missing required argument with name: " + name);
          }

          Type factoryArgumentType = Primitives.toBoxed(genericParameterTypes[i]);

          if(!Types.raw(argumentBindings.get(name).getType()).equals(Types.raw(factoryArgumentType))) {
            throw new DefinitionException(factoryMethod, "has argument [" + parameters[i] + "] with name '" + name + "' that should be of type [" + argumentBindings.get(name).getType() + "] but was: " + factoryArgumentType);
          }

          if(!encounteredNames.add(name)) {
            throw new DefinitionException(factoryMethod, "has a duplicate argument name: " + name);
          }

          names.add(name);
        }
        catch(MissingArgumentException e) {
          throw new DefinitionException(factoryMethod, "is missing argument name for [" + parameters[i] + "]", e);
        }
      }

      return names;
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
}
