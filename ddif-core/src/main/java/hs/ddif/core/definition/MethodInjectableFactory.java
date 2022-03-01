package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.instantiation.factory.MethodObjectFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Constructs {@link Injectable}s for {@link Method}s part of a specific
 * owner {@link Type}.
 */
public class MethodInjectableFactory {
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

    return injectableFactory.create(ownerType, method, method, bindingProvider.ofMethod(method, ownerType), new MethodObjectFactory(method));
  }
}
