package org.int4.dirk.core;

import java.util.Collection;

import org.int4.dirk.core.definition.InjectionTargetExtensionStore;
import org.int4.dirk.core.instantiation.InjectionTargetExtensions;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;

public class InjectionTargetExtensionStores {

  public static InjectionTargetExtensionStore create(Collection<InjectionTargetExtension<?, ?>> extensions) {
    return new InjectionTargetExtensionStore(extensions);
  }

  public static InjectionTargetExtensionStore create() {
    return create(InjectionTargetExtensions.create());
  }
}

