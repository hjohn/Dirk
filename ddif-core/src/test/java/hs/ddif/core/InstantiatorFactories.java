package hs.ddif.core;

import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.TypeExtension;

import java.util.Collection;
import java.util.List;

public class InstantiatorFactories {

  public static InstantiatorFactory create() {
    return create(List.of());
  }

  public static InstantiatorFactory create(Collection<TypeExtension<?>> typeExtensions) {
    return new DefaultInstantiatorFactory(TypeExtensionStores.create(typeExtensions));
  }
}
