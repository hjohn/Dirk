package hs.ddif.core;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.AutoDiscoveringGatherer;
import hs.ddif.core.inject.store.MethodInjectable;

import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * This extension detects if a class implements {@link Provider} and registers
 * and additional injectable for the type the provider provides.
 */
public class ProviderGathererExtension implements AutoDiscoveringGatherer.Extension {

  @Override
  public List<ResolvableInjectable> getDerived(ResolvableInjectable injectable) {
    Class<?> cls = TypeUtils.getRawType(injectable.getType(), null);

    if(Provider.class.isAssignableFrom(cls)) {
      try {
        return List.of(new MethodInjectable(cls.getMethod("get"), injectable.getType()));
      }
      catch(NoSuchMethodException | SecurityException e) {
        throw new IllegalStateException(e);
      }
    }

    return List.of();
  }
}
