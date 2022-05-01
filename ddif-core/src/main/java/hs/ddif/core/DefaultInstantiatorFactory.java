package hs.ddif.core;

import hs.ddif.spi.instantiation.InjectionTarget;
import hs.ddif.spi.instantiation.Instantiator;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.InjectionTargetExtension;
import hs.ddif.util.Types;

/**
 * Produces type specific {@link Instantiator}s.
 */
class DefaultInstantiatorFactory implements InstantiatorFactory {
  private final InjectionTargetExtensionStore injectionTargetExtensionStore;

  /**
   * Constructs a new instance.
   *
   * @param injectionTargetExtensionStore a {@link InjectionTargetExtensionStore}, cannot be {@code null}
   */
  DefaultInstantiatorFactory(InjectionTargetExtensionStore injectionTargetExtensionStore) {
    this.injectionTargetExtensionStore = injectionTargetExtensionStore;
  }

  @Override
  public <T> Instantiator<T> getInstantiator(InjectionTarget injectionTarget) {
    InjectionTargetExtension<T> extension = injectionTargetExtensionStore.getExtension(Types.raw(injectionTarget.getKey().getType()));

    return extension.create(this, injectionTarget);
  }
}
