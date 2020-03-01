package hs.ddif.core;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BeanDefinitionStore;

import javax.inject.Provider;

public class ProviderInjectorExtension implements BeanDefinitionStore.Extension {

  @Override
  public ResolvableInjectable getDerived(ResolvableInjectable injectable) {
    return Provider.class.isAssignableFrom((Class<?>)injectable.getType()) ? new ProvidedInjectable((Class<?>)injectable.getType()) : null;
  }
}
