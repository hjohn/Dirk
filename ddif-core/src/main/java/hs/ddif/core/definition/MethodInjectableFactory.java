package hs.ddif.core.definition;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.core.definition.factory.MethodObjectFactory;

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
   * @param <T> the type of the given method's return type
   * @param method a {@link Method}, cannot be {@code null}
   * @param ownerType the type of the owner of the method, cannot be {@code null} and must match with {@link Method#getDeclaringClass()}
   * @return a new {@link Injectable}, never {@code null}
   * @throws DefinitionException when a definition problem was encountered
   */
  public <T> Injectable<T> create(Method method, Type ownerType) throws DefinitionException {
    if(method == null) {
      throw new IllegalArgumentException("method cannot be null");
    }
    if(ownerType == null) {
      throw new IllegalArgumentException("ownerType cannot be null");
    }

    return injectableFactory.create(ownerType, method, method, bindingProvider.ofMethod(method, ownerType), new MethodObjectFactory<>(method));
  }
}
