package hs.ddif.core.instantiation;

import hs.ddif.library.ListTypeExtension;
import hs.ddif.library.ProviderTypeExtension;
import hs.ddif.library.SetTypeExtension;
import hs.ddif.spi.instantiation.TypeExtension;

import java.util.List;

import jakarta.inject.Provider;

public class TypeExtensions {

  public static List<TypeExtension<?>> create() {
    return List.of(
      new ListTypeExtension<>(),
      new SetTypeExtension<>(),
      new ProviderTypeExtension<>(Provider.class, s -> s::get)
    );
  }
}

