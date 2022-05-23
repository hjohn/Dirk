package hs.ddif.core;

import hs.ddif.core.definition.InjectionTargetExtensionStore;
import hs.ddif.core.instantiation.InjectionTargetExtensions;
import hs.ddif.spi.instantiation.InjectionTargetExtension;

import java.util.Collection;

public class InjectionTargetExtensionStores {

  public static InjectionTargetExtensionStore create(Collection<InjectionTargetExtension<?, ?>> extensions) {
    return new InjectionTargetExtensionStore(extensions);
  }

  public static InjectionTargetExtensionStore create() {
    return create(InjectionTargetExtensions.create());
  }
}

