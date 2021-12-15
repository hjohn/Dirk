package hs.ddif.core.inject.store;

import hs.ddif.annotations.Parameter;
import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ObjectFactory;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Constructs {@link ResolvableInjectable}s for {@link Type}s which resolve to
 * concrete classes.
 */
public class ClassInjectableFactory {
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

    Map<AccessibleObject, List<Binding>> bindingsMap = BindingProvider.ofClass(injectableClass);

    /*
     * Check bindings to see if this injectable can be instantiated and injected.
     */

    int constructorCount = 0;

    for(Map.Entry<AccessibleObject, List<Binding>> entry : bindingsMap.entrySet()) {
      if(entry.getKey() instanceof Constructor) {
        constructorCount++;
      }
      else if(entry.getKey() instanceof Field) {
        Field field = (Field)entry.getKey();

        if(Modifier.isFinal(field.getModifiers())) {
          throw new BindingException("Cannot inject final field: " + field + " in: " + injectableClass);
        }
      }
    }

    if(constructorCount < 1) {
      throw new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: " + injectableClass);
    }
    if(constructorCount > 1) {
      throw new BindingException("Multiple @Inject annotated constructors found, but only one allowed: " + injectableClass);
    }

    return factory.create(
      type,
      AnnotationExtractor.extractQualifiers(injectableClass),
      bindingsMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList()),
      AnnotationExtractor.findScopeAnnotation(injectableClass),
      null,
      new ClassObjectFactory(type, bindingsMap)
    );
  }

  static class ClassObjectFactory implements ObjectFactory {
    private static final Set<Object> UNDER_CONSTRUCTION = ConcurrentHashMap.newKeySet();

    private final Type type;
    private final Map<AccessibleObject, List<Binding>> bindings;
    private final PostConstructor postConstructor;

    ClassObjectFactory(Type type, Map<AccessibleObject, List<Binding>> bindings) {
      this.type = type;
      this.bindings = bindings;
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
      Map.Entry<AccessibleObject, List<Binding>> constructorEntry = findConstructorEntry(bindings);
      Constructor<?> constructor = (Constructor<?>)constructorEntry.getKey();

      try {
        java.lang.reflect.Parameter[] parameters = constructor.getParameters();
        Object[] values = new Object[constructorEntry.getValue().size()];  // Parameters for constructor

        for(int i = 0; i < values.length; i++) {
          Binding binding = constructorEntry.getValue().get(i);

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

        return constructor.newInstance(values);
      }
      catch(Exception e) {
        throw new InstanceCreationFailure(constructor, "Exception while constructing instance", e);
      }
    }

    private void injectInstance(Instantiator instantiator, Object instance, List<NamedParameter> namedParameters) throws InstanceCreationFailure {
      for(Map.Entry<AccessibleObject, List<Binding>> entry : bindings.entrySet()) {
        try {
          AccessibleObject accessibleObject = entry.getKey();

          if(accessibleObject instanceof Field) {
            Field field = (Field)accessibleObject;
            Binding binding = entry.getValue().get(0);

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
          throw new InstanceCreationFailure(entry.getKey(), "Exception while injecting", e);
        }
      }
    }

    private static Map.Entry<AccessibleObject, List<Binding>> findConstructorEntry(Map<AccessibleObject, List<Binding>> bindings) {
      for(Map.Entry<AccessibleObject, List<Binding>> entry : bindings.entrySet()) {
        if(entry.getKey() instanceof Constructor) {
          return entry;
        }
      }

      throw new IllegalStateException("Bindings must always contain a constructor entry");
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
