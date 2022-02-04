package hs.ddif.core.instantiation;

import hs.ddif.core.config.ListTypeExtension;
import hs.ddif.core.config.ProviderTypeExtension;
import hs.ddif.core.config.SetTypeExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

public class InstanceFactories {

  public static InstantiatorFactory create() {
    Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

    typeExtensions.put(List.class, new ListTypeExtension<>());
    typeExtensions.put(Set.class, new SetTypeExtension<>());
    typeExtensions.put(Provider.class, new ProviderTypeExtension<>());

    return new InstantiatorFactory(typeExtensions);
  }
}

