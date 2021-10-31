package hs.ddif.core;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.MethodInjectable;
import hs.ddif.core.inject.store.ResolvableInjectableStore;
import hs.ddif.core.store.Resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * This extension detects if a class implements {@link Provider} and registers
 * and additional injectable for the type the provider provides.
 */
public class ProviderStoreExtension implements ResolvableInjectableStore.Extension {

  @Override
  public List<Supplier<ResolvableInjectable>> getDerived(Resolver<ResolvableInjectable> resolver, ResolvableInjectable injectable) {
    List<Supplier<ResolvableInjectable>> suppliers = new ArrayList<>();
    Class<?> cls = TypeUtils.getRawType(injectable.getType(), null);

    if(Provider.class.isAssignableFrom(cls)) {
      suppliers.add(() -> {
        try {
          return new MethodInjectable(cls.getMethod("get"), injectable.getType());
        }
        catch(NoSuchMethodException | SecurityException e) {
          throw new IllegalStateException(e);
        }
      });
    }

    return suppliers;
  }
}
