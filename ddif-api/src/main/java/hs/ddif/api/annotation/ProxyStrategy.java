package hs.ddif.api.annotation;

import java.util.function.Function;

/**
 * A strategy that determines how to create proxies.
 */
public interface ProxyStrategy {

  /**
   * Creates a proxy template for the given {@link Class} and returns a function
   * which can be used to create a new proxy.
   *
   * @param <T> the class type to proxy
   * @param cls a {@link Class} to proxy, cannot be {@code null}
   * @return a function which can be used to create a new proxy, never {@code null}
   * @throws Exception when unable to create a proxy, including when the functionality
   *   is disabled, the given class is unsuitable or an error occurred during proxy creation
   */
  <T> Function<InstanceSupplier<T>, T> createProxy(Class<T> cls) throws Exception;

  /**
   * Supplies instances that are wrapped by a proxy.
   *
   * @param <T> the wrapped type
   */
  interface InstanceSupplier<T> {
    T get() throws Exception;
  }
}


