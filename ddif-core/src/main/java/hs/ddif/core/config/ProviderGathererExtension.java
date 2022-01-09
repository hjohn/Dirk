package hs.ddif.core.config;

import hs.ddif.core.config.standard.AutoDiscoveringGatherer;
import hs.ddif.core.inject.bind.BindingException;
import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.inject.injectable.MethodInjectableFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;

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
  public List<Injectable> getDerived(Injectable injectable) {
    Class<?> cls = TypeUtils.getRawType(injectable.getType(), null);

    if(Provider.class.isAssignableFrom(cls)) {
      try {
        Method method = cls.getMethod("get");
        Type providedType = method.getGenericReturnType();

        if(TypeUtils.getRawType(providedType, null) == Provider.class) {
          throw new BindingException("Nested Provider not allowed in: " + method);
        }

        return List.of(methodInjectableFactory.create(method, injectable.getType()));
      }
      catch(NoSuchMethodException | SecurityException e) {
        throw new IllegalStateException(e);
      }
    }

    return List.of();
  }
}
