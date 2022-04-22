package hs.ddif.spi.instantiation;

import hs.ddif.api.instantiation.Key;

import java.lang.reflect.AnnotatedElement;

/**
 * Produces type specific {@link Instantiator}s.
 */
public interface InstantiatorFactory {

  /**
   * Gets an {@link Instantiator} for the given {@link Key} and using annotations
   * found on the optional given {@link AnnotatedElement}.
   *
   * @param <T> the type the {@link Instantiator} produces
   * @param key a {@link Key}, cannot be {@code null}
   * @param element an {@link AnnotatedElement}, can be {@code null}
   * @return an {@link Instantiator}, never {@code null}
   */
  <T> Instantiator<T> getInstantiator(Key key, AnnotatedElement element);
}
