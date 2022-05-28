package org.int4.dirk.library;

import java.util.function.Function;

import org.int4.dirk.spi.config.ProxyStrategy;

/**
 * An implementation of {@link ProxyStrategy} that will always throw an exception
 * when attempting to create a proxy, effectively disabling the functionality.
 */
public class NoProxyStrategy implements ProxyStrategy {

  @Override
  public <T> Function<InstanceSupplier<T>, T> createProxyFactory(Class<T> cls) {
    throw new UnsupportedOperationException("Proxies are unavailable");
  }
}
