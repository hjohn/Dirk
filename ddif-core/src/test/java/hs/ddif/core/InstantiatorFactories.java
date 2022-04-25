package hs.ddif.core;

import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.TypeExtension;

import java.util.Map;

public class InstantiatorFactories {

  public static InstantiatorFactory create() {
    return create(Map.of());
  }

  public static InstantiatorFactory create(Map<Class<?>, TypeExtension<?>> typeExtensions) {
    return new DefaultInstantiatorFactory(TypeExtensionStores.create(typeExtensions));
  }
}
