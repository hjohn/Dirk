package org.int4.dirk.core;

import java.util.List;

import org.int4.dirk.api.InstanceResolver;
import org.int4.dirk.api.TypeLiteral;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.util.Key;
import org.int4.dirk.core.util.Resolver;

/**
 * Implements the {@link InstanceResolver} interface.
 */
class DefaultInstanceResolver implements InstanceResolver {
  private final Resolver<Injectable<?>> resolver;
  private final InstantiationContextFactory instantiationContextFactory;

  /**
   * Constructs a new instance.
   *
   * @param instantiationContextFactory an {@link InstantiationContextFactory}, cannot be {@code null}
   */
  DefaultInstanceResolver(Resolver<Injectable<?>> resolver, InstantiationContextFactory instantiationContextFactory) {
    this.resolver = resolver;
    this.instantiationContextFactory = instantiationContextFactory;
  }

  @Override
  public <T> T getInstance(TypeLiteral<T> typeLiteral, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return getInstance(KeyFactory.of(typeLiteral.getType(), qualifiers));
  }

  @Override
  public <T> T getInstance(Class<T> cls, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return getInstance(KeyFactory.of(cls, qualifiers));
  }

  @Override
  public <T> List<T> getInstances(TypeLiteral<T> typeLiteral, Object... qualifiers) throws CreationException {
    return getInstances(KeyFactory.of(typeLiteral.getType(), qualifiers));
  }

  @Override
  public <T> List<T> getInstances(Class<T> cls, Object... qualifiers) throws CreationException {
    return getInstances(KeyFactory.of(cls, qualifiers));
  }

  private <T> T getInstance(Key key) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return instantiationContextFactory.<T>createContext(resolver, key, false).get();
  }

  private <T> List<T> getInstances(Key key) throws CreationException {
    return instantiationContextFactory.<T>createContext(resolver, key, false).getAll();
  }
}
