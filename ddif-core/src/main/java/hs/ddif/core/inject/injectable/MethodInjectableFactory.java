package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.bind.BindingException;
import hs.ddif.core.inject.bind.BindingProvider;
import hs.ddif.core.inject.injection.Injection;
import hs.ddif.core.inject.injection.ObjectFactory;
import hs.ddif.core.inject.instantiation.InstanceCreationFailure;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Constructs {@link Injectable}s for {@link Method}s part of a specific
 * owner {@link Type}.
 */
public class MethodInjectableFactory {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);

  private final BindingProvider bindingProvider;
  private final InjectableFactory injectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param bindingProvider a {@link BindingProvider}, cannot be {@code null}
   * @param injectableFactory a {@link InjectableFactory}, cannot be {@code null}
   */
  public MethodInjectableFactory(BindingProvider bindingProvider, InjectableFactory injectableFactory) {
    this.bindingProvider = bindingProvider;
    this.injectableFactory = injectableFactory;
  }

  /**
   * Creates a new {@link Injectable}.
   *
   * @param method a {@link Method}, cannot be {@code null}
   * @param ownerType the type of the owner of the method, cannot be {@code null} and must match with {@link Method#getDeclaringClass()}
   * @return a new {@link Injectable}, never {@code null}
   */
  public Injectable create(Method method, Type ownerType) {
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
      throw new DefinitionException(method, "has unresolvable return type");
    }
    if(TypeUtils.containsTypeVariables(returnType)) {
      throw new DefinitionException(method, "has unresolvable type variables");
    }
    if(returnType == void.class) {
      throw new DefinitionException(method, "has no return type");
    }
    if(method.isAnnotationPresent(Inject.class)) {
      throw new DefinitionException(method, "cannot be annotated with Inject");
    }

    try {
      return injectableFactory.create(
        returnType,
        Annotations.findDirectlyMetaAnnotatedAnnotations(method, QUALIFIER),
        bindingProvider.ofMethod(method, ownerType),
        ScopeAnnotations.find(method),
        method,  // for proper discrimination, the exact method should also be taken into account, next to its generic type
        new MethodObjectFactory(method)
      );
    }
    catch(BindingException e) {
      throw new DefinitionException(method, "has unresolvable bindings");
    }
  }

  static class MethodObjectFactory implements ObjectFactory {
    private final Method method;

    MethodObjectFactory(Method method) {
      this.method = method;
    }

    @Override
    public Object createInstance(List<Injection> injections) throws InstanceCreationFailure {
      try {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        Object[] values = new Object[injections.size() - (isStatic ? 0 : 1)];  // Parameters for method
        Object instance = null;
        int parameterIndex = 0;

        for(Injection binding : injections) {
          if(binding.getTarget() instanceof Method) {
            values[parameterIndex++] = binding.getValue();
          }
          else {
            instance = binding.getValue();
          }
        }

        method.setAccessible(true);

        return method.invoke(instance, values);
      }
      catch(Exception e) {
        throw new InstanceCreationFailure(method, "call failed", e);
      }
    }
  }
}
