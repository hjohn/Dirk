package org.int4.dirk.core;

import org.int4.dirk.core.definition.InjectionTargetExtensionStore;
import org.int4.dirk.core.instantiation.InjectionTargetExtensions;
import org.int4.dirk.core.store.InjectableStore;

public class InstanceResolvers {

  public static DefaultInstanceResolver create() {
    InjectionTargetExtensionStore injectionTargetExtensionStore = InjectionTargetExtensionStores.create(InjectionTargetExtensions.create());
    InjectableStore store = new InjectableStore(InjectableFactories.PROXY_STRATEGY);
    InstanceFactory instanceFactory = new InstanceFactory(InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.PROXY_STRATEGY, injectionTargetExtensionStore);

    return new DefaultInstanceResolver(store, instanceFactory);
  }

}
