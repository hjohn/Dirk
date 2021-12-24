package hs.ddif.core.inject.store;

import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ObjectFactory;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.store.Key;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Constructs {@link ResolvableInjectable}s for {@link Method}s part of a specific
 * owner {@link Type}.
 */
public class MethodInjectableFactory {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);

  private final ResolvableInjectableFactory factory;

  /**
   * Constructs a new instance.
   *
   * @param factory a {@link ResolvableInjectableFactory}, cannot be null
   */
  public MethodInjectableFactory(ResolvableInjectableFactory factory) {
    this.factory = factory;
  }

  /**
   * Creates a new {@link ResolvableInjectable}.
   *
   * @param method a {@link Method}, cannot be null
   * @param ownerType the type of the owner of the method, cannot be null and must match with {@link Method#getDeclaringClass()}
   * @return a new {@link ResolvableInjectable}, never null
   */
  public ResolvableInjectable create(Method method, Type ownerType) {
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

    List<Binding> bindings = BindingProvider.ofExecutable(method, ownerType);

    return factory.create(
      returnType,
      Annotations.findDirectlyMetaAnnotatedAnnotations(method, QUALIFIER),
      bindings,
      AnnotationExtractor.findScopeAnnotation(method),
      method,  // for proper discrimination, the exact method should also be taken into account, next to its generic type
      new MethodObjectFactory(method, new Key(ownerType), bindings)
    );
  }

  static class MethodObjectFactory implements ObjectFactory {
    private final Method method;
    private final Key ownerKey;
    private final List<Binding> bindings;

    MethodObjectFactory(Method method, Key ownerKey, List<Binding> bindings) {
      this.method = method;
      this.ownerKey = ownerKey;
      this.bindings = bindings;
    }

    @Override
    public Object createInstance(Instantiator instantiator, NamedParameter... parameters) throws InstanceCreationFailure {
      if(parameters.length > 0) {
        throw new InstanceCreationFailure(method, "Superflous parameters supplied, none expected for producer method but got: " + Arrays.toString(parameters));
      }

      return constructInstance(instantiator);
    }

    private Object constructInstance(Instantiator instantiator) throws InstanceCreationFailure {
      try {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        Object obj = isStatic ? null : instantiator.getInstance(ownerKey);
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
  }
}
