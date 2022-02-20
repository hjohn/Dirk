package hs.ddif.core.config.standard;

import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.InjectableFactories;
import hs.ddif.core.inject.store.InjectableStore;
import hs.ddif.core.inject.store.InstantiatorBindingMap;
import hs.ddif.core.instantiation.InstanceFactories;
import hs.ddif.core.instantiation.InstantiatorFactory;

import java.util.List;

public class InstanceResolvers {
  private static final ClassInjectableFactory FACTORY = new InjectableFactories().forClass();

  public static DefaultInstanceResolver create() {
    InstantiatorFactory instantiatorFactory = InstanceFactories.create();
    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
    InjectableStore store = new InjectableStore(instantiatorBindingMap);
    DefaultDiscovererFactory discovererFactory = new DefaultDiscovererFactory(false, List.of(), FACTORY);
    DefaultInstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap);

    return new DefaultInstanceResolver(store, discovererFactory, instantiationContext, instantiatorFactory);
  }

  public static DefaultInstanceResolver createWithConsistencyPolicy() {
    DefaultDiscovererFactory discovererFactory = new DefaultDiscovererFactory(false, List.of(), FACTORY);
    InstantiatorFactory instantiatorFactory = InstanceFactories.create();
    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
    InjectableStore store = new InjectableStore(instantiatorBindingMap);
    DefaultInstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap);

    return new DefaultInstanceResolver(store, discovererFactory, instantiationContext, instantiatorFactory);
  }
}
