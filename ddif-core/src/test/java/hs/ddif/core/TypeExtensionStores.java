package hs.ddif.core;

import hs.ddif.core.instantiation.TypeExtensions;
import hs.ddif.spi.instantiation.TypeExtension;

import java.util.Collection;

public class TypeExtensionStores {

  public static TypeExtensionStore create(Collection<TypeExtension<?>> typeExtensions) {
    return new TypeExtensionStore(new DirectTypeExtension<>(), typeExtensions);
  }

  public static TypeExtensionStore create() {
    return create(TypeExtensions.create());
  }
}

