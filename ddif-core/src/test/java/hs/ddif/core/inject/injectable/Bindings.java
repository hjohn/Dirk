package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.injection.Injection;
import hs.ddif.core.inject.instantiation.InstanceCreationFailure;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.inject.instantiation.MultipleInstances;
import hs.ddif.core.inject.instantiation.NoSuchInstance;
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
