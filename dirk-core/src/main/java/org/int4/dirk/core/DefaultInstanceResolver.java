package org.int4.dirk.core;

import java.util.List;

import org.int4.dirk.api.InstanceResolver;
import org.int4.dirk.api.TypeLiteral;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.definition.Key;

/**
 * Implements the {@link InstanceResolver} interface.
 */
class DefaultInstanceResolver implements InstanceResolver {
  private final InstantiationContextFactory instantiationContextFactory;

  /**
   * Constructs a new instance.
   *
   * @param instantiationContextFactory an {@link InstantiationContextFactory}, cannot be {@code null}
   */
  DefaultInstanceResolver(InstantiationContextFactory instantiationContextFactory) {
    this.instantiationContextFactory = instantiationContextFactory;
  }

  @Override
  public synchronized <T> T getInstance(TypeLiteral<T> typeLiteral, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return getInstance(KeyFactory.of(typeLiteral.getType(), qualifiers));
  }

  @Override
  public synchronized <T> T getInstance(Class<T> cls, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return getInstance(KeyFactory.of(cls, qualifiers));
  }

  @Override
  public synchronized <T> List<T> getInstances(TypeLiteral<T> typeLiteral, Object... qualifiers) throws CreationException {
    return getInstances(KeyFactory.of(typeLiteral.getType(), qualifiers));
  }

  @Override
  public synchronized <T> List<T> getInstances(Class<T> cls, Object... qualifiers) throws CreationException {
    return getInstances(KeyFactory.of(cls, qualifiers));
  }

  private <T> T getInstance(Key key) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return instantiationContextFactory.<T>createContext(key, false).create();
  }

  private <T> List<T> getInstances(Key key) throws CreationException {
    return instantiationContextFactory.<T>createContext(key, false).createAll();
  }
}
