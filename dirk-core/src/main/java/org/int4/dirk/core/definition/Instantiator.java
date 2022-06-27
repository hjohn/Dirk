package org.int4.dirk.core.definition;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.util.Resolver;
import org.int4.dirk.spi.scope.CreationalContext;

/**
 * A factory for {@link CreationalContext}s of a specific type.
 *
 * @param <T> the type of instance contained in the context
 */
public interface Instantiator<T> {

  /**
   * Creates a {@link CreationalContext}.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @return a {@link CreationalContext}, never {@code null}
   * @throws CreationException when an error occurred during creation of a matching instance
   * @throws UnsatisfiedResolutionException when no matching instance was available or could be created
   * @throws AmbiguousResolutionException when multiple matching instances were available
   * @throws ScopeNotActiveException when the scope for the produced type is not active
   */
  CreationalContext<T> create(Resolver<Injectable<?>> resolver) throws CreationException, UnsatisfiedResolutionException, AmbiguousResolutionException, ScopeNotActiveException;
}