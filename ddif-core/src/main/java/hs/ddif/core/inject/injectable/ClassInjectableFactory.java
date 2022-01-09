package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.bind.BindingException;

import java.lang.reflect.Type;

public interface ClassInjectableFactory {

  /**
   * Attempts to create a new {@link ResolvableInjectable} from the given {@link Type}.
   *
   * @param type a {@link Type}, cannot be null
   * @return a {@link ResolvableInjectable}, never null
   * @throws BindingException when the given type does not meet all requirements
   */
  ResolvableInjectable create(Type type);
}
