package hs.ddif.core.instantiation;

import hs.ddif.library.ListInjectionTargetExtension;
import hs.ddif.library.ProviderInjectionTargetExtension;
import hs.ddif.library.SetInjectionTargetExtension;
import hs.ddif.spi.instantiation.InjectionTargetExtension;

import java.util.List;

import jakarta.inject.Provider;

public class InjectionTargetExtensions {

  public static List<InjectionTargetExtension<?>> create() {
    return List.of(
      new ListInjectionTargetExtension<>(),
      new SetInjectionTargetExtension<>(),
      new ProviderInjectionTargetExtension<>(Provider.class, s -> s::get)
    );
  }
}

