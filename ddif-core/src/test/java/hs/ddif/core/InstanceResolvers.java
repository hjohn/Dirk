package hs.ddif.core;

import hs.ddif.api.instantiation.InstantiatorFactory;
import hs.ddif.core.inject.store.InjectableStore;
import hs.ddif.core.inject.store.InstantiatorBindingMap;

import java.util.List;

public class InstanceResolvers {
  private static final InjectableFactories FACTORY = new InjectableFactories();

  public static DefaultInstanceResolver create() {
    InstantiatorFactory instantiatorFactory = InstantiatorFactories.create();
    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
    InjectableStore store = new InjectableStore(instantiatorBindingMap);
    DefaultDiscovererFactory discovererFactory = new DefaultDiscovererFactory(false, List.of(), instantiatorFactory, FACTORY.forClass(), FACTORY.forMethod(), FACTORY.forField());
    DefaultInstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap);

    return new DefaultInstanceResolver(store, discovererFactory, instantiationContext, instantiatorFactory);
  }

}
