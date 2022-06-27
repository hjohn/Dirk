package org.int4.dirk.core.definition;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.util.Key;
import org.int4.dirk.core.util.Resolver;
import org.int4.dirk.spi.instantiation.Resolution;
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

  /**
   * Returns how the injection target should be resolved.
   *
   * @return a {@link Resolution}, never {@code null}
   */
  Resolution getResolution();

  /**
   * Returns the {@link Key} of which individual elements of the injection target consist.
   * For simple types, this will be the same as the injection target's type. For types
   * which are provided by an injection target extension, this will be base type that
   * is looked up for injection.
   *
   * @return a {@link Key}, never {@code null}
   */
  Key getElementKey();
}