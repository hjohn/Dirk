package org.int4.dirk.di;

import org.int4.dirk.extensions.proxy.ByteBuddyProxyStrategy;
import org.int4.dirk.spi.config.ProxyStrategy;

class ByteBuddyProxyStrategySupport {
  static ProxyStrategy create() {
    return new ByteBuddyProxyStrategy();
  }
}