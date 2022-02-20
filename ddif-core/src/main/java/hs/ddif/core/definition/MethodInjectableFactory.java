package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.instantiation.factory.MethodObjectFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Constructs {@link Injectable}s for {@link Method}s part of a specific
 * owner {@link Type}.
 */
public class MethodInjectableFactory {
  private final BindingProvider bindingProvider;
  private final AnnotatedInjectableFactory injectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param bindingProvider a {@link BindingProvider}, cannot be {@code null}
   * @param injectableFactory a {@link AnnotatedInjectableFactory}, cannot be {@code null}
   */
  public MethodInjectableFactory(BindingProvider bindingProvider, AnnotatedInjectableFactory injectableFactory) {
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
    if(method.isAnnotationPresent(Inject.class)) {
      throw new DefinitionException(method, "cannot be annotated with Inject");
    }

    return injectableFactory.create(returnType, method, bindingProvider.ofMethod(method, ownerType), new MethodObjectFactory(method));
  }
}
