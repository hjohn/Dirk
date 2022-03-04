package hs.ddif.core.config;

import hs.ddif.core.config.standard.InjectableExtension;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.util.Types;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * This extension detects if a class implements a method of a provider interface and registers
 * an additional injectable for the type the provider provides.
 */
public class ProviderInjectableExtension implements InjectableExtension {
  private final Method providerMethod;
  private final MethodInjectableFactory methodInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param providerMethod a getter {@link Method} of a provider type, cannot be {@code null}
   * @param methodInjectableFactory a {@link MethodInjectableFactory}, cannot be {@code null}
   */
  public ProviderInjectableExtension(Method providerMethod, MethodInjectableFactory methodInjectableFactory) {
    this.providerMethod = providerMethod;
    this.methodInjectableFactory = methodInjectableFactory;
  }

  @Override
  public List<Injectable<?>> getDerived(Type type) {
    Class<?> cls = Types.raw(type);

    if(cls != null && providerMethod.getDeclaringClass().isAssignableFrom(cls) && !cls.isInterface()) {
      try {
        Method implementingMethod = cls.getMethod(providerMethod.getName());  // can have annotations that interface method does not have

        return List.of(methodInjectableFactory.create(implementingMethod, type));
      }
      catch(NoSuchMethodException | SecurityException e) {
        throw new IllegalStateException(e);
      }
    }

    return List.of();
  }
}
