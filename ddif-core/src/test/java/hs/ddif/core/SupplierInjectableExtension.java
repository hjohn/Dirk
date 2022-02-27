package hs.ddif.core;

import hs.ddif.core.config.standard.InjectableExtension;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.util.Types;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Supplier;

/**
 * This extension detects if a class implements a provider type and registers
 * an additional injectable for the type the provider provides.
 */
public class SupplierInjectableExtension implements InjectableExtension {
  private final MethodInjectableFactory methodInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param methodInjectableFactory a {@link MethodInjectableFactory}, cannot be {@code null}
   */
  public SupplierInjectableExtension(MethodInjectableFactory methodInjectableFactory) {
    this.methodInjectableFactory = methodInjectableFactory;
  }

  @Override
  public List<Injectable> getDerived(Type type) {
    Class<?> cls = Types.raw(type);

    if(cls != null && Supplier.class.isAssignableFrom(cls) && !cls.isInterface()) {
      try {
        Method method = cls.getMethod("get");

        return List.of(methodInjectableFactory.create(method, type));
      }
      catch(NoSuchMethodException | SecurityException e) {
        throw new IllegalStateException(e);
      }
    }

    return List.of();
  }
}
