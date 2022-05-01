package hs.ddif.core;

import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.InjectionTargetExtension;

import java.util.Collection;
import java.util.List;

public class InstantiatorFactories {

  public static InstantiatorFactory create() {
    return create(List.of());
  }

  public static InstantiatorFactory create(Collection<InjectionTargetExtension<?>> extensions) {
    return new DefaultInstantiatorFactory(InjectionTargetExtensionStores.create(extensions));
  }
}
