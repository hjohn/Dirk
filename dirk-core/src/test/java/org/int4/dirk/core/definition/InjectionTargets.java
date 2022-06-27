package org.int4.dirk.core.definition;

import java.util.ArrayList;
import java.util.List;

import org.int4.dirk.core.definition.injection.Injection;

public class InjectionTargets {

  public static List<Injection> resolve(List<InjectionTarget> injectionTargets, Object... values) {
    List<Injection> injections = new ArrayList<>();

    for(int i = 0; i < injectionTargets.size(); i++) {
      Binding binding = injectionTargets.get(i).getBinding();

      injections.add(new Injection(binding.getAccessibleObject(), values[i]));
    }

    return injections;
  }
}
