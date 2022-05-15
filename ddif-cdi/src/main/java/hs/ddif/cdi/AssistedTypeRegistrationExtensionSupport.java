package hs.ddif.cdi;

import hs.ddif.extensions.proxy.ByteBuddyProxyStrategy;
import hs.ddif.spi.config.ProxyStrategy;

class ByteBuddyProxyStrategySupport {
  static ProxyStrategy create() {
    return new ByteBuddyProxyStrategy();
  }
}