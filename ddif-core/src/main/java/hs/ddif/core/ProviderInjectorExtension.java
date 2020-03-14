package hs.ddif.core;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.core.inject.store.MethodInjectable;

import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * This extension detects if a class implements {@link Provider} and registers
 * and additional injectable for the type the provider provides.
 */
public class ProviderInjectorExtension implements BeanDefinitionStore.Extension {

  @Override
  public List<ResolvableInjectable> getDerived(ResolvableInjectable injectable) {
    Class<?> cls = TypeUtils.getRawType(injectable.getType(), null);

    try {
      return Provider.class.isAssignableFrom(cls)
        ? Collections.singletonList(new MethodInjectable(cls.getMethod("get"), injectable.getType()))
        : Collections.emptyList();
    }
    catch(NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
