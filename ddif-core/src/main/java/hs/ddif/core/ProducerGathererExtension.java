package hs.ddif.core;

import hs.ddif.annotations.Parameter;
import hs.ddif.annotations.Producer;
import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.MultipleInstances;
import hs.ddif.core.inject.instantiator.NoSuchInstance;
import hs.ddif.core.inject.instantiator.ObjectFactory;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.AutoDiscoveringGatherer.Extension;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.inject.store.ResolvableInjectableFactory;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.store.Key;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * An injector extension which provider support for assisted injection through
 * the {@link Producer} and {@link Parameter} annotations.
 */
public class ProducerGathererExtension implements Extension {
  private static final Map<Class<?>, ResolvableInjectable> PRODUCER_INJECTABLES = new WeakHashMap<>();
  private static final String INSTANTIATOR_FIELD_NAME = "_instantiator_";

  private final ResolvableInjectableFactory factory;
  private final ClassInjectableFactory classInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param factory a {@link ResolvableInjectableFactory}, cannot be null
   */
  public ProducerGathererExtension(ResolvableInjectableFactory factory) {
    this.factory = factory;
    this.classInjectableFactory = new ClassInjectableFactory(this::create);
  }

  private ResolvableInjectable create(Type type, Set<Annotation> qualifiers, List<Binding> bindings, Annotation scope, Object discriminator, ObjectFactory objectFactory) {
    return factory.create(type, qualifiers, bindings, scope, discriminator, (instantiator, parameters) -> createInstance(objectFactory, instantiator, parameters));
  }

  private static Object createInstance(ObjectFactory objectFactory, Instantiator instantiator, NamedParameter[] parameters) throws InstanceCreationFailure {
    Object obj = objectFactory.createInstance(instantiator, parameters);

    if(obj instanceof InstantiatorSetter) {
      ((InstantiatorSetter)obj).setInstantiator(instantiator);
    }

    return obj;
  }

  @Override
  public List<ResolvableInjectable> getDerived(ResolvableInjectable injectable) {
    final Producer producer = (TypeUtils.getRawType(injectable.getType(), null)).getAnnotation(Producer.class);

    if(producer == null) {
      return Collections.emptyList();
    }

    ResolvableInjectable producerInjectable = PRODUCER_INJECTABLES.get(injectable.getType());

    if(producerInjectable == null) {
      String[] names = validateProducerAndReturnParameterNames(injectable, producer);

      if(names.length == 0 && !Modifier.isAbstract(producer.value().getModifiers())) {
        producerInjectable = classInjectableFactory.create(producer.value());
      }
      else {
        // Creates a subclass of type producer.value() which will serve as a Factory
        // (with assisted injection) for the type annotated with @Producer. The
        // factory class should have an abstract method which returns this type. A
        // new method is created that overrides it and when intercepted will construct
        // the type.

        producerInjectable = classInjectableFactory.create(new ByteBuddy()
          .subclass(producer.value())
          .annotateType(AnnotationDescription.Builder.ofType(Singleton.class).build())  // It is a singleton, avoids scope problems
          .defineField(INSTANTIATOR_FIELD_NAME, Instantiator.class, Visibility.PRIVATE)
          .implement(InstantiatorSetter.class)
          .intercept(FieldAccessor.ofField(INSTANTIATOR_FIELD_NAME))
          .method(ElementMatchers.returns((Class<?>)injectable.getType()).and(ElementMatchers.isAbstract()))
          .intercept(MethodDelegation.to(new Interceptor(injectable.getType(), names)))
          .make()
          .load(getClass().getClassLoader())
          .getLoaded()
        );
      }

      PRODUCER_INJECTABLES.put((Class<?>)injectable.getType(), producerInjectable);
    }

    return List.of(producerInjectable);
  }

  /**
   * Interface used to set the {@link Instantiator}. Internal use only, public to avoid an
   * {@code IllegalAccessException}.
   */
  public interface InstantiatorSetter {
    void setInstantiator(Instantiator instantiator);
  }

  /**
   * Interceptor for the implemented factory method. Internal use only, public to avoid an
   * {@code IllegalAccessException}.
   */
  public static class Interceptor {
    private final Key key;
    private final String[] names;

    Interceptor(Type type, String[] names) {
      this.key = new Key(type);
      this.names = names;
    }

    @RuntimeType
    public Object intercept(@FieldValue(INSTANTIATOR_FIELD_NAME) Instantiator instantiator, @AllArguments Object[] args) throws InstanceCreationFailure, NoSuchInstance, MultipleInstances, OutOfScopeException {
      NamedParameter[] namedParameters = new NamedParameter[args.length];

      for(int i = 0; i < args.length; i++) {
        namedParameters[i] = new NamedParameter(names[i], args[i]);
      }

      return instantiator.getInstance(key, namedParameters);
    }
  }

  // Does validation on an optional Producer class, checking if has only a single abstract
  // method.  This method furthermore must exactly match the required injections annotated
  // with @Parameter and must return an instance of this injectable's class.
  private static String[] validateProducerAndReturnParameterNames(ResolvableInjectable injectable, Producer producer) {
    Class<?> producerClass = producer.value();
    Method factoryMethod = null;
    int abstractMethodCount = 0;

    for(Method method : producerClass.getMethods()) {
      if(Modifier.isAbstract(method.getModifiers())) {
        factoryMethod = method;
        abstractMethodCount++;

        if(abstractMethodCount > 1) {
          throw new BindingException("Too many abstract factory methods.  Producer {" + producerClass + "} has multiple abstract methods where only one is allowed");
        }
      }
    }

    if(factoryMethod == null) {
      return new String[] {};
    }

    Class<?> injectableClass = (Class<?>)injectable.getType();
    Map<String, Type> parameterBindings = createBindingNameMap(injectable);
    String[] names = new String[parameterBindings.size()];

    if(!factoryMethod.getReturnType().equals(injectableClass)) {
      throw new BindingException("Factory method has wrong return type.  Producer {" + producerClass + "} has an abstract method with return type that does not match {" + injectableClass + "}: " + factoryMethod);
    }

    if(factoryMethod.getParameterCount() != parameterBindings.size()) {
      throw new BindingException("Factory method has wrong number of parameters.  Producer {" + producerClass + "} method {" + factoryMethod + "} should have " + parameterBindings.size() + " parameter(s) of types: " + parameterBindings);
    }

    Type[] genericParameterTypes = factoryMethod.getGenericParameterTypes();
    java.lang.reflect.Parameter[] parameters = factoryMethod.getParameters();

    for(int i = 0; i < parameters.length; i++) {
      String name = determineParameterName(parameters[i]);

      if(name == null) {
        throw new BindingException("Missing parameter name.  Producer {" + injectableClass + "} method {" + factoryMethod + "} is missing name for {" + parameters[i] + "}; specify one with @Parameter or compile classes with parameter name information");
      }

      if(!parameterBindings.containsKey(name)) {
        throw new BindingException("Factory method is missing required parameter.  Producer {" + producerClass + "} method {" + factoryMethod + "} is missing required parameter with name: " + name);
      }

      if(!parameterBindings.get(name).equals(genericParameterTypes[i])) {
        throw new BindingException("Factory method has parameter of wrong type.  Producer {" + producerClass + "} with method {" + factoryMethod + "} has parameter {" + parameters[i] + "} with name '" + name + "' that should be of type {" + parameterBindings.get(name) + "} but was: " + genericParameterTypes[i]);
      }

      names[i] = name;
    }

    return names;
  }

  private static Map<String, Type> createBindingNameMap(ResolvableInjectable injectable) {
    Class<?> injectableClass = (Class<?>)injectable.getType();
    Map<AccessibleObject, List<Binding>> bindings = injectable.getBindings().stream().collect(Collectors.groupingBy(Binding::getAccessibleObject));
    Map<String, Type> parameterBindings = new HashMap<>();

    for(Entry<AccessibleObject, List<Binding>> entry : bindings.entrySet()) {
      Constructor<?> constructor = entry.getKey() instanceof Constructor ? (Constructor<?>)entry.getKey() : null;
      java.lang.reflect.Parameter[] parameters = constructor == null ? null : constructor.getParameters();
      List<Binding> value = entry.getValue();

      for(int i = 0; i < value.size(); i++) {
        Binding binding = value.get(i);

        if(binding.isParameter()) {
          String name = parameters == null ? ((Field)entry.getKey()).getName() : determineParameterName(parameters[i]);

          if(name == null) {
            throw new BindingException("Missing parameter name.  Name cannot be determined for {" + injectableClass + "} constructor parameter " + i + "; specify one with @Parameter or compile classes with parameter name information");
          }

          parameterBindings.put(name, binding.getType());
        }
      }
    }

    return parameterBindings;
  }

  private static String determineParameterName(java.lang.reflect.Parameter parameter) {
    Parameter parameterAnnotation = parameter.getAnnotation(Parameter.class);

    if((parameterAnnotation == null || parameterAnnotation.value().isEmpty()) && !parameter.isNamePresent()) {
      return null;
    }

    return parameterAnnotation != null && !parameterAnnotation.value().isEmpty() ? parameterAnnotation.value() : parameter.getName();
  }
}
