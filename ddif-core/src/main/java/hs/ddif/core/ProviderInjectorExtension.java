package hs.ddif.core;

import javax.inject.Provider;

public class ProviderInjectorExtension implements Injector.Extension {

  @Override
  public ScopedInjectable getDerived(Injector injector, ScopedInjectable injectable) {
    return Provider.class.isAssignableFrom(injectable.getInjectableClass()) ? new ProvidedInjectable(injectable.getInjectableClass()) : null;
  }
}
