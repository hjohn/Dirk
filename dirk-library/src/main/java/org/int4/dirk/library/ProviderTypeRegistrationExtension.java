package org.int4.dirk.library;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.spi.definition.TypeRegistrationExtension;
import org.int4.dirk.util.Types;

/**
 * This extension detects if a class implements a method of a provider interface and registers
 * an additional type for the type the provider provides.
 */
public class ProviderTypeRegistrationExtension implements TypeRegistrationExtension {
  private final Method providerMethod;

  /**
   * Constructs a new instance.
   *
   * @param providerMethod a getter {@link Method} of a provider type, cannot be {@code null}
   */
  public ProviderTypeRegistrationExtension(Method providerMethod) {
    this.providerMethod = providerMethod;
  }

  @Override
  public void deriveTypes(Registry registry, Type type) throws DefinitionException {
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
