package hs.ddif.core.definition;

import hs.ddif.core.config.standard.DefaultInjectionContext;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.instantiation.injection.InjectionContext;

import java.util.ArrayList;
import java.util.List;

public class Bindings {

  public static InjectionContext resolve(List<Binding> bindings, Object... values) {
    List<Injection> injections = new ArrayList<>();

    for(int i = 0; i < bindings.size(); i++) {
      Binding binding = bindings.get(i);

      injections.add(new Injection(binding.getAccessibleObject(), values[i]));
    }

    return new DefaultInjectionContext(injections);
  }
}
