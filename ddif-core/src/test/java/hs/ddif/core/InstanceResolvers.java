package hs.ddif.core;

import hs.ddif.core.definition.InjectionTargetExtensionStore;
import hs.ddif.core.instantiation.InjectionTargetExtensions;
import hs.ddif.core.store.InjectableStore;

public class InstanceResolvers {

  public static DefaultInstanceResolver create() {
    InjectionTargetExtensionStore injectionTargetExtensionStore = InjectionTargetExtensionStores.create(InjectionTargetExtensions.create());
    InjectableStore store = new InjectableStore(InjectableFactories.PROXY_STRATEGY);
    InstantiationContextFactory instantiationContextFactory = new InstantiationContextFactory(store, InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.PROXY_STRATEGY, injectionTargetExtensionStore);

    return new DefaultInstanceResolver(instantiationContextFactory);
  }

}
