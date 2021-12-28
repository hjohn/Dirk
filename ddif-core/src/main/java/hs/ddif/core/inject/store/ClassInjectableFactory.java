package hs.ddif.core.inject.store;

import hs.ddif.annotations.Parameter;
import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ObjectFactory;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Constructs {@link ResolvableInjectable}s for {@link Type}s which resolve to
 * concrete classes.
 */
public class ClassInjectableFactory {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);

  private final ResolvableInjectableFactory factory;

  /**
   * Constructs a new instance.
   *
   * @param factory a {@link ResolvableInjectableFactory}, cannot be null
   */
  public ClassInjectableFactory(ResolvableInjectableFactory factory) {
    this.factory = factory;
  }

  /**
   * Creates a new {@link ResolvableInjectable} from the given {@link Type}.
   *
   * @param type a {@link Type}, cannot be null
   * @return a {@link ResolvableInjectable}, never null
   * @throws BindingException if the given type is not annotated and has no public empty constructor or is incorrectly annotated
   */
  public ResolvableInjectable create(Type type) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }

    Class<?> injectableClass = TypeUtils.getRawType(type, null);

    if(Modifier.isAbstract(injectableClass.getModifiers())) {
      throw new BindingException("Type cannot be abstract: " + type);
    }

    if(TypeUtils.containsTypeVariables(type)) {
      throw new BindingException("Unresolved type variables in " + type + " are not allowed: " + Arrays.toString(injectableClass.getTypeParameters()));
    }

    List<Binding> bindings = BindingProvider.ofClass(injectableClass);

    /*
     * Check bindings to see if this injectable can be instantiated and injected.
     */

    bindings.stream()
      .map(Binding::getAccessibleObject)
      .filter(Field.class::isInstance)
      .map(Field.class::cast)
      .filter(f -> Modifier.isFinal(f.getModifiers()))
      .findFirst()
      .ifPresent(f -> {
        throw new BindingException("Cannot inject final field: " + f + " in: " + injectableClass);
      });

    Set<Constructor<?>> constructors = bindings.stream()
      .map(Binding::getAccessibleObject)
      .filter(Constructor.class::isInstance)
      .map(c -> (Constructor<?>)c)  // Constructor.class::cast works with ECJ but not JavaC
      .collect(Collectors.toSet());

    getPublicEmptyConstructor(injectableClass).ifPresent(c -> {
      if(constructors.isEmpty() || c.isAnnotationPresent(Inject.class)) {
        constructors.add(c);
      }
    });

    if(constructors.size() < 1) {
      throw new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: " + injectableClass);
    }
    if(constructors.size() > 1) {
      throw new BindingException("Multiple @Inject annotated constructors found, but only one allowed: " + injectableClass);
    }

    return factory.create(
      type,
      Annotations.findDirectlyMetaAnnotatedAnnotations(injectableClass, QUALIFIER),
      bindings,
      AnnotationExtractor.findScopeAnnotation(injectableClass),
      null,
      new ClassObjectFactory(type, bindings, constructors.iterator().next())
    );
  }

  private static <T> Optional<Constructor<T>> getPublicEmptyConstructor(Class<T> cls) {
    try {
      return Optional.of(cls.getConstructor());
    }
    catch(NoSuchMethodException e) {
      return Optional.empty();
    }
  }

  static class ClassObjectFactory implements ObjectFactory {
    private static final Set<Object> UNDER_CONSTRUCTION = ConcurrentHashMap.newKeySet();

    private final Type type;
    private final List<Binding> bindings;
    private final Constructor<?> constructor;
    private final PostConstructor postConstructor;

    ClassObjectFactory(Type type, List<Binding> bindings, Constructor<?> constructor) {
      this.type = type;
      this.bindings = bindings;
      this.constructor = constructor;
      this.postConstructor = new PostConstructor(TypeUtils.getRawType(type, null));
    }

    @Override
    public Object createInstance(Instantiator instantiator, NamedParameter... parameters) throws InstanceCreationFailure {
      if(UNDER_CONSTRUCTION.contains(this)) {
        throw new InstanceCreationFailure(type, "Already under construction (dependency creation loop in @PostConstruct method!)");
      }

      try {
        UNDER_CONSTRUCTION.add(this);

        List<NamedParameter> namedParameters = new ArrayList<>(Arrays.asList(parameters));

        Object instance = constructInstance(instantiator, namedParameters);

        injectInstance(instantiator, instance, namedParameters);

        if(!namedParameters.isEmpty()) {
          throw new InstanceCreationFailure(type, "Superflous parameters supplied, expected " + (parameters.length - namedParameters.size()) + " but got: " + parameters.length);
        }

        postConstructor.call(instance);

        return instance;
      }
      finally {
        UNDER_CONSTRUCTION.remove(this);
      }
    }

    private Object constructInstance(Instantiator instantiator, List<NamedParameter> namedParameters) throws InstanceCreationFailure {
      try {
        java.lang.reflect.Parameter[] parameters = constructor.getParameters();
        Object[] values = new Object[constructor.getParameterCount()];  // Parameters for constructor

        for(Binding binding : bindings) {
          if(binding.getAccessibleObject() == constructor) {
            int i = binding.getIndex();

            if(binding.isParameter()) {
              String name = determineParameterName(parameters[i]);

              if(name == null) {
                throw new IllegalArgumentException("Missing parameter name for: " + parameters[i] + "; specify one with @Parameter or compile classes with parameter name information");
              }

              values[i] = findAndRemoveNamedParameterValue(name, namedParameters);
            }
            else {
              values[i] = binding.getValue(instantiator);
            }
          }
        }

        return constructor.newInstance(values);
      }
      catch(Exception e) {
        throw new InstanceCreationFailure(constructor, "Exception while constructing instance", e);
      }
    }

    private void injectInstance(Instantiator instantiator, Object instance, List<NamedParameter> namedParameters) throws InstanceCreationFailure {
      for(Binding binding : bindings) {
        AccessibleObject accessibleObject = binding.getAccessibleObject();

        try {
          if(accessibleObject instanceof Field) {
            Field field = (Field)accessibleObject;
            Object valueToSet;

            if(binding.isParameter()) {
              valueToSet = findAndRemoveNamedParameterValue(field.getName(), namedParameters);
            }
            else {
              valueToSet = binding.getValue(instantiator);
            }

            if(valueToSet != null) {  // Do not set fields to null, leave default value instead
              field.set(instance, valueToSet);
            }
          }
        }
        catch(Exception e) {
          throw new InstanceCreationFailure(accessibleObject, "Exception while injecting", e);
        }
      }
    }

    private static String determineParameterName(java.lang.reflect.Parameter parameter) {
      Parameter parameterAnnotation = parameter.getAnnotation(Parameter.class);

      if((parameterAnnotation == null || parameterAnnotation.value().isEmpty()) && !parameter.isNamePresent()) {
        return null;
      }

      return parameterAnnotation != null && !parameterAnnotation.value().isEmpty() ? parameterAnnotation.value() : parameter.getName();
    }

    private static Object findAndRemoveNamedParameterValue(String name, List<NamedParameter> namedParameters) {
      for(int i = 0; i < namedParameters.size(); i++) {
        if(namedParameters.get(i).getName().equals(name)) {
          return namedParameters.remove(i).getValue();
        }
      }

      throw new NoSuchElementException("Parameter '" + name + "' was not supplied");
    }
  }
}
