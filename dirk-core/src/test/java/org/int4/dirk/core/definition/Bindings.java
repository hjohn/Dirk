package org.int4.dirk.core.definition;

import java.util.ArrayList;
import java.util.List;

import org.int4.dirk.core.definition.injection.Injection;

public class Bindings {

  public static List<Injection> resolve(List<Binding> bindings, Object... values) {
    List<Injection> injections = new ArrayList<>();

    for(int i = 0; i < bindings.size(); i++) {
      Binding binding = bindings.get(i);

      injections.add(new Injection(binding.getAccessibleObject(), values[i]));
    }

    return injections;
  }
}
