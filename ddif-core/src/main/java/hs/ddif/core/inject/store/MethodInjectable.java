package hs.ddif.core.inject.store;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link ResolvableInjectable} for creating instances based on a {@link Method}.
 */
public class MethodInjectable implements ResolvableInjectable {
  private final Method method;
  private final Type injectableType;
  private final Map<AccessibleObject, List<Binding>> externalBindings;
  private final List<ResolvableBinding> bindings;
  private final Set<AnnotationDescriptor> qualifiers;
  private final Annotation scopeAnnotation;

  /**
   * Creates a new {@link MethodInjectable} from the given {@link Method}.  The
   * resulting injectable could be used in an implementation of the method as
   * its {@link #getInstance(Instantiator, NamedParameter...)} would match the
   * return type of the method.
   *
   * @param method a {@link Method}, cannot be null
   * @throws BindingException if the given type is not annotated and has no public empty constructor or is incorrectly annotated
   */
  public MethodInjectable(Method method) {
    if(method == null) {
      throw new IllegalArgumentException("method cannot be null");
    }

    this.method = method;
    this.injectableType = method.getGenericReturnType();
    this.qualifiers = AnnotationExtractor.extractQualifiers(method);
    this.bindings = ResolvableBindingProvider.ofExecutable(method);
    this.scopeAnnotation = AnnotationExtractor.findScopeAnnotation(method);

    @SuppressWarnings("unchecked")
    Map<AccessibleObject, List<Binding>> externalBindings = Map.of(method, (List<Binding>)(List<?>)bindings);

    this.externalBindings = externalBindings;
  }

  @Override
  public Object getInstance(Instantiator instantiator, NamedParameter... parameters) {
    if(parameters.length > 0) {
      throw new ConstructionException("Superflous parameters supplied, none expected for Produces method but got: " + Arrays.toString(parameters));
    }

    return constructInstance(instantiator);
  }

  private Object constructInstance(Instantiator instantiator) {
    try {
      Object obj = instantiator.getInstance(method.getDeclaringClass());
      Object[] values = new Object[bindings.size()];  // Parameters for method

      for(int i = 0; i < values.length; i++) {
        values[i] = bindings.get(i).getValue(instantiator);
      }

      method.setAccessible(true);

      return method.invoke(obj, values);
    }
    catch(Exception e) {
      throw new ConstructionException("Unable to construct: " + injectableType, e);
    }
  }

  @Override
  public Map<AccessibleObject, List<Binding>> getBindings() {
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

    MethodInjectable other = (MethodInjectable)obj;

    return injectableType.equals(other.injectableType)
        && qualifiers.equals(other.qualifiers);
  }

  @Override
  public String toString() {
    return "Injectable-Method(" + injectableType + ")";
  }
}
