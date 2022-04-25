package hs.ddif.core;

import hs.ddif.spi.instantiation.InjectionTarget;
import hs.ddif.spi.instantiation.Instantiator;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.TypeExtension;
import hs.ddif.util.Types;

/**
 * Produces type specific {@link Instantiator}s.
 */
class DefaultInstantiatorFactory implements InstantiatorFactory {
  private final TypeExtensionStore typeExtensionStore;

  /**
   * Constructs a new instance.
   *
   * @param typeExtensionStore a {@link TypeExtensionStore}, cannot be {@code null}
   */
  DefaultInstantiatorFactory(TypeExtensionStore typeExtensionStore) {
    this.typeExtensionStore = typeExtensionStore;
  }

  @Override
  public <T> Instantiator<T> getInstantiator(InjectionTarget injectionTarget) {
    TypeExtension<T> extension = typeExtensionStore.getExtension(Types.raw(injectionTarget.getKey().getType()));

    return extension.create(this, injectionTarget);
  }
}
