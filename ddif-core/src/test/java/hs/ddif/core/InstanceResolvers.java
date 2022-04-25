package hs.ddif.core;

import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.InstantiatorBindingMap;
import hs.ddif.spi.instantiation.InstantiatorFactory;

import java.util.List;

public class InstanceResolvers {
  private static final InjectableFactories FACTORY = new InjectableFactories();

  public static DefaultInstanceResolver create() {
    InstantiatorFactory instantiatorFactory = InstantiatorFactories.create();
    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
    InjectableStore store = new InjectableStore(instantiatorBindingMap, InjectableFactories.PROXY_STRATEGY);
    DefaultDiscovererFactory discovererFactory = new DefaultDiscovererFactory(false, List.of(), instantiatorFactory, FACTORY.forClass(), FACTORY.forMethod(), FACTORY.forField());
    DefaultInstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap, InjectableFactories.PROXY_STRATEGY);

    return new DefaultInstanceResolver(store, discovererFactory, instantiationContext, instantiatorFactory);
  }

}
