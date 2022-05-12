package hs.ddif.core;

import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.InstantiatorBindingMap;
import hs.ddif.spi.instantiation.InstantiatorFactory;

public class InstanceResolvers {

  public static DefaultInstanceResolver create() {
    InstantiatorFactory instantiatorFactory = InstantiatorFactories.create();
    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
    InjectableStore store = new InjectableStore(instantiatorBindingMap, InjectableFactories.PROXY_STRATEGY);
    DefaultInstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap, InjectableFactories.PROXY_STRATEGY);

    return new DefaultInstanceResolver(instantiationContext, instantiatorFactory);
  }

}
