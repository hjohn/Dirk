package org.int4.dirk.spi.config;

import java.util.function.Function;

/**
 * A strategy that determines how to create proxies.
 */
public interface ProxyStrategy {

  /**
   * Creates a proxy factory for the given {@link Class} and returns a function
   * which can be used to create a new proxy.
   *
   * @param <T> the class type to proxy
   * @param cls a {@link Class} to proxy, cannot be {@code null}
   * @return a function which can be used to create a new proxy, never {@code null}
   * @throws Exception when unable to create a proxy factory, including when the functionality
   *   is disabled, the given class is unsuitable or an error occurred during proxy factory creation
   */
  <T> Function<InstanceSupplier<T>, T> createProxyFactory(Class<T> cls) throws Exception;

  /**
   * Supplies instances that are wrapped by a proxy.
   *
   * @param <T> the wrapped type
   */
  interface InstanceSupplier<T> {

    /**
     * Returns an underlying instance for the proxy of type {@code T}.
     *
     * @return an underlying instance of type {@code T}, never {@code null}
     * @throws Exception when creation of the underlying instance fails
     */
    T get() throws Exception;
  }
}


