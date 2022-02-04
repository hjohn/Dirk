package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.injection.Injection;

import java.util.ArrayList;
import java.util.List;

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
