package hs.ddif.core;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BeanDefinitionStore;

import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;

public class ProviderInjectorExtension implements BeanDefinitionStore.Extension {

  @Override
  public List<ResolvableInjectable> getDerived(ResolvableInjectable injectable) {
    return Provider.class.isAssignableFrom(TypeUtils.getRawType(injectable.getType(), null))
      ? Collections.<ResolvableInjectable>singletonList(new ProvidedInjectable(injectable.getType()))
      : Collections.<ResolvableInjectable>emptyList();
  }
}
