package org.int4.dirk.core.instantiation;

import java.util.List;

import org.int4.dirk.library.ListInjectionTargetExtension;
import org.int4.dirk.library.ProviderInjectionTargetExtension;
import org.int4.dirk.library.SetInjectionTargetExtension;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;

import jakarta.inject.Provider;

public class InjectionTargetExtensions {

  public static List<InjectionTargetExtension<?, ?>> create() {
    return List.of(
      new ListInjectionTargetExtension<>(),
      new SetInjectionTargetExtension<>(),
      new ProviderInjectionTargetExtension<>(Provider.class, s -> s::get)
    );
  }
}

