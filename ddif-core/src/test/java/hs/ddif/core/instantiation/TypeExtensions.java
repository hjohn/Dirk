package hs.ddif.core.instantiation;

import hs.ddif.core.config.ListTypeExtension;
import hs.ddif.core.config.ProviderTypeExtension;
import hs.ddif.core.config.SetTypeExtension;
import hs.ddif.spi.instantiation.TypeExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Provider;

public class TypeExtensions {

  public static Map<Class<?>, TypeExtension<?>> create() {
    Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

    typeExtensions.put(List.class, new ListTypeExtension<>());
    typeExtensions.put(Set.class, new SetTypeExtension<>());
    typeExtensions.put(Provider.class, new ProviderTypeExtension<>(Provider.class, s -> s::get));

    return typeExtensions;
  }
}

