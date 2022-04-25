package hs.ddif.library;

import hs.ddif.spi.config.ProxyStrategy;

import java.util.function.Function;

/**
 * An implementation of {@link ProxyStrategy} that will always throw an exception
 * when attempting to create a proxy, effectively disabling the functionality.
 */
public class NoProxyStrategy implements ProxyStrategy {

  @Override
  public <T> Function<InstanceSupplier<T>, T> createProxy(Class<T> cls) {
    throw new UnsupportedOperationException("Proxies are unavailable");
  }
}
