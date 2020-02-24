package hs.ddif.core.inject.store;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.bind.Parameter;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;

/**
 * A {@link ResolvableInjectable} for creating instances based on a {@link Class}.<p>
 */
public class ClassInjectable implements ResolvableInjectable {
  private final Class<?> injectableClass;
  private final Map<AccessibleObject, Binding[]> externalBindings;
  private final Map<AccessibleObject, ClassInjectableBinding[]> bindings;
  private final Annotation scopeAnnotation;
  private final PostConstructor postConstructor;

  /**
   * When true, the object is currently being constructed.  This is used to
   * detect loops in {@link PostConstruct} annotated methods, where another
   * depedency is created that depends on this object, which isn't fully
   * constructed yet (and thus not yet available as dependency, triggering
   * another creation).
   */
  private boolean underConstruction;

  /**
   * Constructs a new instance.
   *
   * @param injectableClass the {@link Class}, cannot be null, cannot be an interface or be abstract
   * @throws BindingException if the given class is not annotated and has no public empty constructor or is incorrectly annotated
   */
  @SuppressWarnings("unchecked")
  public ClassInjectable(Class<?> injectableClass) {
    if(injectableClass == null) {
      throw new IllegalArgumentException("injectableClass cannot be null");
    }
    if(Modifier.isAbstract(injectableClass.getModifiers())) {
      throw new IllegalArgumentException("injectableClass must be a concrete class: " + injectableClass);
    }

    this.injectableClass = injectableClass;
    this.bindings = ClassInjectableBindingProvider.resolve(injectableClass);
    this.externalBindings = (Map<AccessibleObject, Binding[]>)(Map<?, ?>)Collections.unmodifiableMap(bindings);
    this.scopeAnnotation = findScopeAnnotation(injectableClass);
    this.postConstructor = new PostConstructor(injectableClass);

    /*
     * Check bindings to see if this injectable can be instantiated and injected.
     */

    int constructorCount = 0;

    for(Map.Entry<AccessibleObject, ClassInjectableBinding[]> entry : bindings.entrySet()) {
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
  }

  private static String determineParameterName(java.lang.reflect.Parameter parameter) {
    Parameter parameterAnnotation = parameter.getAnnotation(Parameter.class);

    if((parameterAnnotation == null || parameterAnnotation.value().isEmpty()) && !parameter.isNamePresent()) {
      return null;
    }

    return parameterAnnotation != null && !parameterAnnotation.value().isEmpty() ? parameterAnnotation.value() : parameter.getName();
  }

  private static Map.Entry<AccessibleObject, ClassInjectableBinding[]> findConstructorEntry(Map<AccessibleObject, ClassInjectableBinding[]> bindings) {
    for(Map.Entry<AccessibleObject, ClassInjectableBinding[]> entry : bindings.entrySet()) {
      if(entry.getKey() instanceof Constructor) {
        return entry;
      }
    }

    throw new IllegalStateException("Bindings must always contain a constructor entry");
  }

  @Override
  public Object getInstance(Instantiator instantiator, NamedParameter... parameters) {
    if(underConstruction) {
      throw new ConstructionException("Object already under construction (dependency creation loop in @PostConstruct method!): " + injectableClass);
    }

    try {
      underConstruction = true;

      List<NamedParameter> namedParameters = new ArrayList<>(Arrays.asList(parameters));

      Object bean = constructInstance(instantiator, namedParameters);

      injectInstance(instantiator, bean, namedParameters);

      if(!namedParameters.isEmpty()) {
        throw new ConstructionException("Superflous parameters supplied, expected " + (parameters.length - namedParameters.size()) + " but got: " + parameters.length);
      }

      postConstructor.call(bean);

      return bean;
    }
    finally {
      underConstruction = false;
    }
  }

  private void injectInstance(Instantiator instantiator, Object bean, List<NamedParameter> namedParameters) {
    for(Map.Entry<AccessibleObject, ClassInjectableBinding[]> entry : bindings.entrySet()) {
      try {
        AccessibleObject accessibleObject = entry.getKey();

        if(accessibleObject instanceof Field) {
          Field field = (Field)accessibleObject;
          ClassInjectableBinding binding = entry.getValue()[0];

          Object valueToSet;

          if(binding.isParameter()) {
            valueToSet = findAndRemoveNamedParameterValue(field.getName(), namedParameters);
          }
          else {
            valueToSet = binding.getValue(instantiator);
          }

          if(valueToSet != null) {  // Donot set fields to null, leave default value instead
            field.setAccessible(true);
            field.set(bean, valueToSet);
          }
        }
      }
      catch(ConstructionException e) {
        throw e;
      }
      catch(Exception e) {
        throw new ConstructionException("Unable to set field [" + entry.getKey() + "] of: " + injectableClass, e);
      }
    }
  }

  private static Object findAndRemoveNamedParameterValue(String name, List<NamedParameter> namedParameters) {
    for(int i = 0; i < namedParameters.size(); i++) {
      if(namedParameters.get(i).getName().equals(name)) {
        return namedParameters.remove(i).getValue();
      }
    }

    throw new ConstructionException("Parameter '" + name + "' was not supplied");
  }

  private Object constructInstance(Instantiator instantiator, List<NamedParameter> namedParameters) {
    try {
      Map.Entry<AccessibleObject, ClassInjectableBinding[]> constructorEntry = findConstructorEntry(bindings);
      Constructor<?> constructor = (Constructor<?>)constructorEntry.getKey();
      java.lang.reflect.Parameter[] parameters = constructor.getParameters();
      Object[] values = new Object[constructorEntry.getValue().length];  // Parameters for constructor

      for(int i = 0; i < values.length; i++) {
        ClassInjectableBinding binding = constructorEntry.getValue()[i];

        if(binding.isParameter()) {
          String name = determineParameterName(parameters[i]);

          if(name == null) {
            throw new ConstructionException("Missing parameter name.  Unable to construct {" + injectableClass + "}, name cannot be determined for: " + parameters[i] + "; specify one with @Parameter or compile classes with parameter name information");
          }

          values[i] = findAndRemoveNamedParameterValue(name, namedParameters);
        }
        else {
          values[i] = binding.getValue(instantiator);
        }
      }

      return constructor.newInstance(values);
    }
    catch(ConstructionException e) {
      throw e;
    }
    catch(Exception e) {
      throw new ConstructionException("Unable to construct: " + injectableClass, e);
    }
  }

  private static Annotation findScopeAnnotation(Class<?> cls) {
    List<Annotation> matchingAnnotations = AnnotationUtils.findAnnotations(cls, javax.inject.Scope.class);

    if(matchingAnnotations.size() > 1) {
      throw new BindingException("Multiple scope annotations found, but only one allowed: " + cls + ", found: " + matchingAnnotations);
    }

    return matchingAnnotations.isEmpty() ? null : matchingAnnotations.get(0);
  }

  @Override
  public Map<AccessibleObject, Binding[]> getBindings() {
    return externalBindings;
  }

  @Override
  public Annotation getScope() {
    return scopeAnnotation;
  }

  @Override
  public Class<?> getInjectableClass() {
    return injectableClass;
  }

  @Override
  public Set<AnnotationDescriptor> getQualifiers() {
    return AnnotationDescriptor.extractQualifiers(injectableClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(injectableClass);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    return injectableClass.equals(((ClassInjectable)obj).injectableClass);
  }

  @Override
  public String toString() {
    return "Injectable-Class(" + injectableClass + ")";
  }
}
