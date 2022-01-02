package hs.ddif.core.inject.instantiator;

import hs.ddif.core.scope.OutOfScopeException;

import java.util.ArrayList;
import java.util.List;

public class Bindings {

  public static List<Injection> resolve(Instantiator instantiator, List<Binding> bindings) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance, OutOfScopeException {
    List<Injection> injections = new ArrayList<>();

    for(Binding binding : bindings) {
      injections.add(new Injection(binding.getAccessibleObject(), binding.getValue(instantiator)));
    }

    return injections;
  }
}
