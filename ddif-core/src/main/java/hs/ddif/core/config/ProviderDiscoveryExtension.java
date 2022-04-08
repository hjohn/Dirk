package hs.ddif.core.config;

import hs.ddif.core.config.standard.DiscoveryExtension;
import hs.ddif.core.util.Types;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * This extension detects if a class implements a method of a provider interface and registers
 * an additional injectable for the type the provider provides.
 */
public class ProviderDiscoveryExtension implements DiscoveryExtension {
  private final Method providerMethod;

  /**
   * Constructs a new instance.
   *
   * @param providerMethod a getter {@link Method} of a provider type, cannot be {@code null}
   */
  public ProviderDiscoveryExtension(Method providerMethod) {
    this.providerMethod = providerMethod;
  }

  @Override
  public void deriveTypes(Registry registry, Type type) {
    Class<?> cls = Types.raw(type);

    if(cls != null && providerMethod.getDeclaringClass().isAssignableFrom(cls) && !cls.isInterface()) {
      try {
        Method implementingMethod = cls.getMethod(providerMethod.getName());  // can have annotations that interface method does not have

        registry.add(implementingMethod, type);
      }
      catch(NoSuchMethodException | SecurityException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
