package hs.ddif.extensions.assisted;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.instantiation.CreationException;
import hs.ddif.api.util.Primitives;
import hs.ddif.api.util.Types;
import hs.ddif.core.definition.Binding;
import hs.ddif.core.definition.BindingProvider;
import hs.ddif.core.definition.factory.ClassObjectFactory;
import hs.ddif.core.definition.injection.Constructable;
import hs.ddif.core.definition.injection.Injection;
import hs.ddif.spi.config.LifeCycleCallbacksFactory;
import hs.ddif.spi.discovery.DiscoveryExtension;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
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
public class AssistedDiscoveryExtension implements DiscoveryExtension {
  private final LifeCycleCallbacksFactory lifeCycleCallbacksFactory;
  private final BindingProvider bindingProvider;
  private final AssistedAnnotationStrategy<?> strategy;

  /**
   * Constructs a new instance.
   *
   * @param bindingProvider a {@link BindingProvider}, cannot be {@code null}
   * @param lifeCycleCallbacksFactory a {@link LifeCycleCallbacksFactory}, cannot be {@code null}
   * @param strategy an {@link AssistedAnnotationStrategy}, cannot be {@code null}
   */
  public AssistedDiscoveryExtension(BindingProvider bindingProvider, LifeCycleCallbacksFactory lifeCycleCallbacksFactory, AssistedAnnotationStrategy<?> strategy) {
    this.lifeCycleCallbacksFactory = lifeCycleCallbacksFactory;
    this.bindingProvider = bindingProvider;
    this.strategy = strategy;
  }

  @Override
  public void deriveTypes(Registry registry, Type factoryType) throws DefinitionException {
    Class<?> factoryClass = Types.raw(factoryType);

    if(strategy.providerClass().equals(factoryClass)) {

      /*
       * Always reject the provider class even if its parameterized type is assist annotated as
       * the provider class is used in the generated types which would result in recursion.
       */

      return;
    }

    Method factoryMethod = findFactoryMethod(factoryClass);

    if(factoryMethod == null) {
      return;
    }

    Map<TypeVariable<?>, Type> factoryTypeArguments = Types.getTypeArguments(factoryType, factoryClass);
    Type productType = Types.resolveVariables(factoryTypeArguments, factoryMethod.getGenericReturnType());

    if(!Types.raw(productType).isAnnotationPresent(strategy.assistedAnnotationClass())) {
      return;
    }

    new ProducerInjectableFactory(productType, factoryType, factoryMethod, factoryTypeArguments)
      .register(registry);
  }

  private class ProducerInjectableFactory {
    private final Type factoryType;
    private final Method factoryMethod;
    private final Map<TypeVariable<?>, Type> factoryTypeArguments;
    private final Class<?> productClass;

    ProducerInjectableFactory(Type productType, Type factoryType, Method factoryMethod, Map<TypeVariable<?>, Type> typeArguments) throws DefinitionException {
      this.factoryType = factoryType;
      this.factoryMethod = factoryMethod;
      this.productClass = Types.raw(productType);
      this.factoryTypeArguments = typeArguments;

      if(Types.containsTypeVariables(productType)) {
        throw new DefinitionException(factoryMethod, "must not have unresolvable type variables to qualify for assisted injection");
      }

      if(Modifier.isAbstract(productClass.getModifiers())) {
        throw new DefinitionException(factoryMethod, "must return a concrete type to qualify for assisted injection");
      }
    }

    public void register(Registry registry) throws DefinitionException {
      registry.add(generateFactoryClass());
    }

    private Class<?> generateFactoryClass() throws DefinitionException {
      Constructor<?> constructor = bindingProvider.getConstructor(productClass);
      List<Binding> productBindings = bindingProvider.ofConstructorAndMembers(constructor, productClass);
      Constructable<?> constructable = new ClassObjectFactory<>(constructor, lifeCycleCallbacksFactory.create(productClass));
      Interceptor<?> interceptor = new Interceptor<>(constructable, strategy);

      /*
       * Construct ByteBuddy builder:
       */

      Builder<?> builder = new ByteBuddy()
        .subclass(factoryType, ConstructorStrategy.Default.IMITATE_SUPER_CLASS.withInheritedAnnotations())
        .annotateType(Types.raw(factoryType).getDeclaredAnnotations())
        .method(ElementMatchers.returns(productClass).and(ElementMatchers.isAbstract()))
        .intercept(MethodDelegation.to(interceptor));

      /*
       * Add a field per binding to the builder:
       */

      List<String> providerFieldNames = new ArrayList<>();
      Map<String, Binding> parameterBindings = new HashMap<>();

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
          String fieldName = "__binding" + i + "__" + toBindingString(binding) + "__";

          if(parameter != null) {
            annotations = new ArrayList<>(annotations);

            annotations.add(strategy.injectAnnotation());
          }

          providerFieldNames.add(fieldName);
          builder = builder
            .defineField(fieldName, Types.parameterize(strategy.providerClass(), binding.getKey().getType()), Visibility.PRIVATE)
            .annotateField(annotations);
        }
      }

      /*
       * Generate the factory:
       */

      Class<?> cls = load(builder.make());

      /*
       * Set the field list on the factory:
       */

      List<Field> providerFields = createProviderFields(cls, providerFieldNames);
      List<String> names = validateProducerAndReturnArgumentNames(parameterBindings);

      interceptor.initialize(parameterBindings, productBindings, providerFields, names);

      return cls;
    }

    private Class<?> load(Unloaded<?> unloaded) {
      try {
        return unloaded.load(getClass().getClassLoader(), ClassLoadingStrategy.UsingLookup.withFallback(() -> {
          try {
            return MethodHandles.privateLookupIn(productClass, MethodHandles.lookup());
          }
          catch(IllegalArgumentException | IllegalAccessException e) {
            return MethodHandles.lookup();
          }
        })).getLoaded();
      }
      catch(Exception e) {
        return unloaded.load(getClass().getClassLoader()).getLoaded();
      }
    }

    private List<String> validateProducerAndReturnArgumentNames(Map<String, Binding> argumentBindings) throws DefinitionException {
      List<String> names = new ArrayList<>();

      if(factoryMethod.getParameterCount() != argumentBindings.size()) {
        throw new DefinitionException(factoryMethod, "should have " + argumentBindings.size() + " argument(s) of types: " + argumentBindings.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getKey().getType())));
      }

      Type[] genericParameterTypes = factoryMethod.getGenericParameterTypes();
      Parameter[] parameters = factoryMethod.getParameters();
      Set<String> encounteredNames = new HashSet<>();
      boolean hasAnnotatedParameters = false;

      for(int i = 0; i < parameters.length; i++) {
        if(strategy.isArgument(parameters[i])) {
          hasAnnotatedParameters = true;
          break;
        }
      }

      try {
        for(int i = 0; i < parameters.length; i++) {
          try {
            String name = strategy.determineArgumentName(parameters[i]);

            if(!argumentBindings.containsKey(name)) {
              throw new DefinitionException(factoryMethod, "is missing required argument with name: " + name);
            }

            Type factoryArgumentType = Types.resolveVariables(factoryTypeArguments, Primitives.toBoxed(genericParameterTypes[i]));

            if(!argumentBindings.get(name).getKey().getType().equals(factoryArgumentType)) {
              throw new DefinitionException(factoryMethod, "has argument [" + parameters[i] + "] with name '" + name + "' that should be of type [" + argumentBindings.get(name).getKey().getType() + "] but was: " + factoryArgumentType);
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
      }
      catch(Exception e) {
        try {
          if(!hasAnnotatedParameters) {
            return validateProducerByTypeAndReturnArgumentNames(argumentBindings);
          }
        }
        catch(DefinitionException e2) {
          e.addSuppressed(e2);
        }

        throw e;
      }

      return names;
    }

    private List<String> validateProducerByTypeAndReturnArgumentNames(Map<String, Binding> argumentBindings) throws DefinitionException {
      List<String> names = new ArrayList<>();

      Type[] genericParameterTypes = factoryMethod.getGenericParameterTypes();
      Parameter[] parameters = factoryMethod.getParameters();

      for(int i = 0; i < parameters.length; i++) {
        Type factoryArgumentType = Primitives.toBoxed(Types.resolveVariables(factoryTypeArguments, genericParameterTypes[i]));
        List<String> matches = argumentBindings.entrySet().stream().filter(e -> e.getValue().getKey().getType().equals(factoryArgumentType)).map(Map.Entry::getKey).collect(Collectors.toList());

        if(matches.size() == 1) {
          names.add(matches.get(0));
        }
        else {
          throw new DefinitionException(factoryMethod, "parameter " + i + " could not be matched by its type: " + factoryArgumentType);
        }
      }

      return names;
    }
  }

  /**
   * Interceptor for generated subclass of assisted injection factories.
   *
   * @param <P> the provider class
   */
  public static class Interceptor<P> {
    private final Constructable<?> productConstructable;
    private final AssistedAnnotationStrategy<P> strategy;
    private final List<InjectionTemplate> templates = new ArrayList<>();

    private List<String> factoryParameterNames;

    Interceptor(Constructable<?> productConstructable, AssistedAnnotationStrategy<P> strategy) {
      this.productConstructable = productConstructable;
      this.strategy = strategy;
    }

    void initialize(Map<String, Binding> productArgumentTypes, List<Binding> bindings, List<Field> fields, List<String> factoryParameterNames) {
      Map<Binding, String> bindingNames = productArgumentTypes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

      this.factoryParameterNames = factoryParameterNames;

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
     * @throws CreationException when the product could not be created
     */
    @RuntimeType
    public Object intercept(@This Object factoryInstance, @AllArguments Object[] args) throws CreationException {
      Map<String, Object> parameters = new HashMap<>();

      for(int i = 0; i < args.length; i++) {
        parameters.put(factoryParameterNames.get(i), args[i]);
      }

      return productConstructable.create(createInjections(factoryInstance, parameters));
    }

    private List<Injection> createInjections(Object factoryInstance, Map<String, Object> parameters) {
      try {
        List<Injection> injections = new ArrayList<>();

        for(InjectionTemplate template : templates) {
          @SuppressWarnings("unchecked")
          Object value = template.field == null ? parameters.get(template.parameterName) : strategy.provision((P)template.field.get(factoryInstance));

          injections.add(new Injection(template.accessibleObject, value));
        }

        return injections;
      }
      catch(IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
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

  private static String toBindingString(Binding binding) {
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
