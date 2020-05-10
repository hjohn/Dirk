package hs.ddif.core.inject.store;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.bind.Parameter;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * A {@link ResolvableInjectable} for creating instances based on a {@link Class}.
 */
public class ClassInjectable implements ResolvableInjectable {
  private final Type injectableType;
  private final Map<AccessibleObject, List<ResolvableBinding>> bindings;
  private final List<Binding> externalBindings;
  private final Set<AnnotationDescriptor> qualifiers;
  private final Annotation scopeAnnotation;
  private final PostConstructor postConstructor;

  /**
   * When true, the object is currently being constructed.  This is used to
   * detect loops in {@link PostConstruct} annotated methods, where another
   * depedency is created that depends on this object, which isn't fully
   * constructed yet (and thus not yet available as dependency, triggering
   * another creation).
   */
  private final ThreadLocal<Boolean> underConstruction = ThreadLocal.withInitial(() -> false);

  /**
   * Creates a new {@link ClassInjectable} from the given {@link Type}.
   *
   * @param type a {@link Type}, cannot be null
   * @throws BindingException if the given type is not annotated and has no public empty constructor or is incorrectly annotated
   */
  public ClassInjectable(Type type) {
    if(type == null) {
      throw new IllegalArgumentException("injectableType cannot be null");
    }

    Class<?> injectableClass = TypeUtils.getRawType(type, null);

    if(Modifier.isAbstract(injectableClass.getModifiers())) {
      throw new IllegalArgumentException("injectableType must be a concrete type: " + type);
    }

    this.injectableType = type;
    this.qualifiers = AnnotationExtractor.extractQualifiers(injectableClass);
    this.bindings = ResolvableBindingProvider.ofClass(injectableClass);
    this.scopeAnnotation = AnnotationExtractor.findScopeAnnotation(injectableClass);
    this.postConstructor = new PostConstructor(injectableClass);
    this.externalBindings = bindings.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

    /*
     * Check bindings to see if this injectable can be instantiated and injected.
     */

    int constructorCount = 0;

    for(Map.Entry<AccessibleObject, List<ResolvableBinding>> entry : bindings.entrySet()) {
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

  private static Map.Entry<AccessibleObject, List<ResolvableBinding>> findConstructorEntry(Map<AccessibleObject, List<ResolvableBinding>> bindings) {
    for(Map.Entry<AccessibleObject, List<ResolvableBinding>> entry : bindings.entrySet()) {
      if(entry.getKey() instanceof Constructor) {
        return entry;
      }
    }

    throw new IllegalStateException("Bindings must always contain a constructor entry");
  }

  @Override
  public Object getInstance(Instantiator instantiator, NamedParameter... parameters) {
    if(underConstruction.get()) {
      throw new ConstructionException("Object already under construction (dependency creation loop in @PostConstruct method!): " + injectableType);
    }

    try {
      underConstruction.set(true);

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
      underConstruction.set(false);
    }
  }

  private void injectInstance(Instantiator instantiator, Object bean, List<NamedParameter> namedParameters) {
    for(Map.Entry<AccessibleObject, List<ResolvableBinding>> entry : bindings.entrySet()) {
      try {
        AccessibleObject accessibleObject = entry.getKey();

        if(accessibleObject instanceof Field) {
          Field field = (Field)accessibleObject;
          ResolvableBinding binding = entry.getValue().get(0);

          Object valueToSet;

          if(binding.isParameter()) {
            valueToSet = findAndRemoveNamedParameterValue(field.getName(), namedParameters);
          }
          else {
            valueToSet = binding.getValue(instantiator);
          }

          if(valueToSet != null) {  // Donot set fields to null, leave default value instead
            field.set(bean, valueToSet);
          }
        }
      }
      catch(ConstructionException e) {
        throw e;
      }
      catch(Exception e) {
        throw new ConstructionException("Unable to set field [" + entry.getKey() + "] of: " + injectableType, e);
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
      Map.Entry<AccessibleObject, List<ResolvableBinding>> constructorEntry = findConstructorEntry(bindings);
      Constructor<?> constructor = (Constructor<?>)constructorEntry.getKey();
      java.lang.reflect.Parameter[] parameters = constructor.getParameters();
      Object[] values = new Object[constructorEntry.getValue().size()];  // Parameters for constructor

      for(int i = 0; i < values.length; i++) {
        ResolvableBinding binding = constructorEntry.getValue().get(i);

        if(binding.isParameter()) {
          String name = determineParameterName(parameters[i]);

          if(name == null) {
            throw new ConstructionException("Missing parameter name.  Unable to construct {" + injectableType + "}, name cannot be determined for: " + parameters[i] + "; specify one with @Parameter or compile classes with parameter name information");
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
      throw new ConstructionException("Unable to construct: " + injectableType, e);
    }
  }

  @Override
  public List<Binding> getBindings() {
    return externalBindings;
  }

  @Override
  public Annotation getScope() {
    return scopeAnnotation;
  }

  @Override
  public Type getType() {
    return injectableType;
  }

  @Override
  public Set<AnnotationDescriptor> getQualifiers() {
    return qualifiers;
  }

  @Override
  public boolean isTemplate() {
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(injectableType, qualifiers);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ClassInjectable other = (ClassInjectable)obj;

    return injectableType.equals(other.injectableType)
        && qualifiers.equals(other.qualifiers);
  }

  @Override
  public String toString() {
    return "Injectable-Class(" + injectableType + ")";
  }
}
