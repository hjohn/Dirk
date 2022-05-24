package org.int4.dirk.core;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.int4.dirk.core.test.scope.Dependent;
import org.int4.dirk.library.SingletonScopeResolver;
import org.int4.dirk.spi.scope.ScopeResolver;

import jakarta.inject.Singleton;

public class ScopeResolverManagers {

  public static ScopeResolverManager create(ScopeResolver... scopeResolvers) {
    ScopeResolver[] standardScopeResolvers = new ScopeResolver[] {new SingletonScopeResolver(Singleton.class)};
    List<ScopeResolver> extendedScopeResolvers = Stream.of(scopeResolvers, standardScopeResolvers).flatMap(Stream::of).collect(Collectors.toList());

    return new ScopeResolverManager(extendedScopeResolvers, Dependent.class);
  }
}

