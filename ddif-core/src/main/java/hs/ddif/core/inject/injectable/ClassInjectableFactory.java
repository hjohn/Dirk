package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.bind.BindingException;

import java.lang.reflect.Type;

/**
 * Factory interface for creating {@link Injectable}s given a {@link Type}.
 */
public interface ClassInjectableFactory {

  /**
   * Attempts to create a new {@link Injectable} from the given {@link Type}.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @return a {@link Injectable}, never {@code null}
   * @throws BindingException when the given type does not meet all requirements
   */
  Injectable create(Type type);
}
