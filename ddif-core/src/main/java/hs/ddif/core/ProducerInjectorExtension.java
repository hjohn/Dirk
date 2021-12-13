package hs.ddif.core;

import hs.ddif.annotations.Parameter;
import hs.ddif.annotations.Producer;
import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.MultipleInstances;
import hs.ddif.core.inject.instantiator.NoSuchInstance;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ClassInjectableFactory;

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
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * An injector extension which provider support for assisted injection through
 * the {@link Producer} and {@link Parameter} annotations.
 */
public class ProducerInjectorExtension implements Injector.Extension {
  private static final Map<Class<?>, ResolvableInjectable> PRODUCER_INJECTABLES = new WeakHashMap<>();

  private final ClassInjectableFactory classInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param classInjectableFactory a {@link ClassInjectableFactory}, cannot be null
   */
  public ProducerInjectorExtension(ClassInjectableFactory classInjectableFactory) {
    this.classInjectableFactory = classInjectableFactory;
  }

  @Override
  public List<ResolvableInjectable> getDerived(final Instantiator instantiator, final ResolvableInjectable injectable) {
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

        // TODO provide instantiator in a different way here?
        //
        // We're basically creating a ClassInjectable here for a factory.  The factory
        // gets injected and has a method that has some additional required parameters.
        // When called, this method takes the parameters and does a #getInstance call
        // on the Instantiator.
        //
        // But... the factory class is just another class, albeit generated with ByteBuddy.
        // If we generate a private field with @Inject annotation for the Instantiator, then
        // ddif should be able to inject this, and it would then be accessible?
        //
        // The ClassInjectable code should see this annotation (or constructor parameter)
        // and create a Key for it. The Injector can then provide it. As Instantiator is
        // not always registered, we may need to do some additional trickage or always
        // pre-register it.

        producerInjectable = classInjectableFactory.create(new ByteBuddy()
          .subclass(producer.value())
          .annotateType(AnnotationDescription.Builder.ofType(Singleton.class).build())  // It is a singleton, avoids scope problems
          .method(ElementMatchers.returns((Class<?>)injectable.getType()).and(ElementMatchers.isAbstract()))
          .intercept(MethodDelegation.to(new Interceptor(instantiator, injectable.getType(), names)))
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
   * Interceptor for the implemented factory method. Internal use only, public to avoid an
   * {@code IllegalAccessException}.
   */
  public static class Interceptor {
    private final Instantiator instantiator;
    private final Type type;
    private final String[] names;

    Interceptor(Instantiator instantiator, Type type, String[] names) {
      this.instantiator = instantiator;
      this.type = type;
      this.names = names;
    }

    @RuntimeType
    public Object intercept(@AllArguments Object[] args) throws InstanceCreationFailure, NoSuchInstance, MultipleInstances {
      NamedParameter[] namedParameters = new NamedParameter[args.length];

      for(int i = 0; i < args.length; i++) {
        namedParameters[i] = new NamedParameter(names[i], args[i]);
      }

      return instantiator.getParameterizedInstance(type, namedParameters);
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
