package hs.ddif.core.config;

import hs.ddif.core.config.standard.AutoDiscoveringGatherer;
import hs.ddif.core.inject.injectable.DefinitionException;
import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.inject.injectable.MethodInjectableFactory;
import hs.ddif.core.util.Types;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Provider;

/**
 * This extension detects if a class implements {@link Provider} and registers
 * and additional injectable for the type the provider provides.
 */
public class ProviderGathererExtension implements AutoDiscoveringGatherer.Extension {
  private final MethodInjectableFactory methodInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param methodInjectableFactory a {@link MethodInjectableFactory}, cannot be {@code null}
   */
  public ProviderGathererExtension(MethodInjectableFactory methodInjectableFactory) {
    this.methodInjectableFactory = methodInjectableFactory;
  }

  @Override
  public List<Injectable> getDerived(Type type) {
    Class<?> cls = Types.raw(type);

    if(Provider.class.isAssignableFrom(cls)) {
      try {
        Method method = cls.getMethod("get");
        Type providedType = method.getGenericReturnType();

        if(Types.raw(providedType) == Provider.class) {
          throw new DefinitionException(method, "cannot have a return type with a nested Provider");
        }

        return List.of(methodInjectableFactory.create(method, type));
      }
      catch(NoSuchMethodException | SecurityException e) {
        throw new IllegalStateException(e);
      }
    }

    return List.of();
  }
}
