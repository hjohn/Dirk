package hs.ddif.core;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

public class ProducerInjectorExtension implements Injector.Extension {
  private static final Map<Class<?>, ClassInjectable> PRODUCER_INJECTABLES = new WeakHashMap<>();

  @Override
  public ScopedInjectable getDerived(final Injector injector, final ScopedInjectable injectable) {
    final Producer producer = injectable.getInjectableClass().getAnnotation(Producer.class);

    if(producer == null) {
      return null;
    }

    ClassInjectable producerInjectable = PRODUCER_INJECTABLES.get(injectable.getInjectableClass());

    if(producerInjectable == null) {
      String[] names = validateProducerAndReturnParameterNames(injectable, producer);

      if(names.length == 0 && !Modifier.isAbstract(producer.value().getModifiers())) {
        producerInjectable = new ClassInjectable(producer.value());
      }
      else {
        producerInjectable = new ClassInjectable(new ByteBuddy()
          .subclass(producer.value())
          .method(ElementMatchers.returns(injectable.getInjectableClass()).and(ElementMatchers.isAbstract()))
          .intercept(MethodDelegation.to(new Interceptor(injector, injectable, names)))
          .make()
          .load(getClass().getClassLoader())
          .getLoaded()
        );
      }

      PRODUCER_INJECTABLES.put(injectable.getInjectableClass(), producerInjectable);
    }

    return producerInjectable;
  }

  public static class Interceptor {
    private final Injector injector;
    private final ScopedInjectable injectable;
    private final String[] names;

    Interceptor(Injector injector, ScopedInjectable injectable, String[] names) {
      this.injector = injector;
      this.injectable = injectable;
      this.names = names;
    }

    @RuntimeType
    public Object intercept(@AllArguments Object[] args) {
      NamedParameter[] namedParameters = new NamedParameter[args.length];

      for(int i = 0; i < args.length; i++) {
        namedParameters[i] = new NamedParameter(names[i], args[i]);
      }

      return injector.getParameterizedInstance((Type)injectable.getInjectableClass(), namedParameters);
    }
  }

  // Does validation on an optional Producer class, checking if has only a single abstract
  // method.  This method furthermore must exactly match the required injections annotated
  // with @Parameter and must return an instance of this injectable's class.
  private static String[] validateProducerAndReturnParameterNames(ScopedInjectable injectable, Producer producer) {
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

    Class<?> injectableClass = injectable.getInjectableClass();
    Map<String, Binding> parameterBindings = createBindingNameMap(injectable);
    String[] names = new String[parameterBindings.size()];

    if(!factoryMethod.getReturnType().equals(injectableClass)) {
      throw new BindingException("Factory method has wrong return type.  Producer {" + producerClass + "} has an abstract method with return type that does not match {" + injectableClass + "}: " + factoryMethod);
    }

    if(factoryMethod.getParameterCount() != parameterBindings.size()) {
      throw new BindingException("Factory method has wrong number of parameters.  Producer {" + producerClass + "} method {" + factoryMethod + "} should have " + parameterBindings.size() + " parameter(s) of types: " + parameterBindings);
    }

    java.lang.reflect.Parameter[] parameters = factoryMethod.getParameters();

    for(int i = 0; i < parameters.length; i++) {
      String name = determineParameterName(parameters[i]);

      if(name == null) {
        throw new BindingException("Missing parameter name.  Producer {" + injectableClass + "} method {" + factoryMethod + "} is missing name for {" + parameters[i] + "}; specify one with @Parameter or compile classes with parameter name information");
      }

      if(!parameterBindings.containsKey(name)) {
        throw new BindingException("Factory method is missing required parameter.  Producer {" + producerClass + "} method {" + factoryMethod + "} is missing required parameter with name: " + name);
      }

      if(!parameterBindings.get(name).getType().equals(parameters[i].getType())) {
        throw new BindingException("Factory method has parameter of wrong type.  Producer {" + producerClass + "} with method {" + factoryMethod + "} has parameter {" + parameters[i] + "} with name '" + name + "' that should be of type {" + parameterBindings.get(name).getType() + "} but was: " + parameters[i].getType());
      }

      names[i] = name;
    }

    return names;
  }

  private static Map<String, Binding> createBindingNameMap(ScopedInjectable injectable) {
    Class<?> injectableClass = injectable.getInjectableClass();
    Map<AccessibleObject, Binding[]> bindings = injectable.getBindings();
    Map<String, Binding> parameterBindings = new HashMap<>();

    for(Entry<AccessibleObject, Binding[]> entry : bindings.entrySet()) {
      Constructor<?> constructor = entry.getKey() instanceof Constructor ? (Constructor<?>)entry.getKey() : null;
      java.lang.reflect.Parameter[] parameters = constructor == null ? null : constructor.getParameters();
      Binding[] value = entry.getValue();

      for(int i = 0; i < value.length; i++) {
        Binding binding = value[i];

        if(binding.isParameter()) {
          String name = parameters == null ? ((Field)entry.getKey()).getName() : determineParameterName(parameters[i]);

          if(name == null) {
            throw new BindingException("Missing parameter name.  Name cannot be determined for {" + injectableClass + "} constructor parameter " + i + "; specify one with @Parameter or compile classes with parameter name information");
          }

          parameterBindings.put(name, binding);
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
