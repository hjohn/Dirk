package hs.ddif.core.inject.store;

import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * A {@link ResolvableInjectable} for creating instances based on a {@link Method}.
 */
public class MethodInjectable implements ResolvableInjectable {
  private final Method method;
  private final Type ownerType;
  private final Type injectableType;
  private final List<Binding> bindings;
  private final Set<AnnotationDescriptor> qualifiers;
  private final Annotation scopeAnnotation;

  /**
   * Creates a new {@link MethodInjectable} from the given {@link Method}.  The
   * resulting injectable could be used in an implementation of the method as
   * its {@link #getInstance(Instantiator, NamedParameter...)} would match the
   * return type of the method.
   *
   * @param method a {@link Method}, cannot be null
   * @param ownerType the type of the owner of the method, cannot be null and must match with {@link Method#getDeclaringClass()}
   */
  public MethodInjectable(Method method, Type ownerType) {
    if(method == null) {
      throw new IllegalArgumentException("method cannot be null");
    }
    if(ownerType == null) {
      throw new IllegalArgumentException("ownerType cannot be null");
    }

    Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(ownerType, method.getDeclaringClass());

    if(typeArguments == null) {
      throw new IllegalArgumentException("ownerType must be assignable to method's declaring class: " + ownerType + "; declaring class: " + method.getDeclaringClass());
    }

    Type returnType = TypeUtils.unrollVariables(typeArguments, method.getGenericReturnType());

    if(returnType == null) {
      throw new BindingException("Method has unresolved return type: " + method);
    }
    if(TypeUtils.containsTypeVariables(returnType)) {
      throw new BindingException("Method has unresolved type variables: " + method);
    }
    if(returnType == void.class) {
      throw new BindingException("Method has no return type: " + method);
    }
    if(method.isAnnotationPresent(Inject.class)) {
      throw new BindingException("Method cannot be annotated with Inject: " + method);
    }

    this.method = method;
    this.ownerType = ownerType;
    this.injectableType = returnType;
    this.qualifiers = AnnotationExtractor.extractQualifiers(method);
    this.bindings = BindingProvider.ofExecutable(method, ownerType);
    this.scopeAnnotation = AnnotationExtractor.findScopeAnnotation(method);
  }

  @Override
  public Object getInstance(Instantiator instantiator, NamedParameter... parameters) throws InstanceCreationFailure {
    if(parameters.length > 0) {
      throw new InstanceCreationFailure(method, "Superflous parameters supplied, none expected for producer method but got: " + Arrays.toString(parameters));
    }

    return constructInstance(instantiator);
  }

  private Object constructInstance(Instantiator instantiator) throws InstanceCreationFailure {
    try {
      boolean isStatic = Modifier.isStatic(method.getModifiers());
      Object obj = isStatic ? null : instantiator.getInstance(ownerType);
      Object[] values = new Object[bindings.size() - (isStatic ? 0 : 1)];  // Parameters for method

      for(int i = 0; i < values.length; i++) {
        values[i] = bindings.get(i).getValue(instantiator);
      }

      method.setAccessible(true);

      return method.invoke(obj, values);
    }
    catch(Exception e) {
      throw new InstanceCreationFailure(method, "Exception while constructing instance via Producer", e);
    }
  }

  @Override
  public List<Binding> getBindings() {
    return bindings;
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
  public int hashCode() {
    return Objects.hash(method, injectableType);
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

    // Note: method does not have generic information, type does, so compare both
    return method.equals(other.method) &&
        injectableType.equals(other.injectableType);
  }

  @Override
  public String toString() {
    return "Injectable-Method(" + injectableType.getTypeName() + " <- " + method + ")";
  }
}
