package hs.ddif.spi.instantiation;

import hs.ddif.api.instantiation.AmbiguousResolutionException;
import hs.ddif.api.instantiation.CreationException;
import hs.ddif.api.instantiation.UnsatisfiedResolutionException;
import hs.ddif.api.scope.ScopeNotActiveException;

import java.util.Set;

/**
 * Produces instances of type {@code T} using a given {@link Key}. The type produced
 * is not necessarily the same type as the key specifies but could be a wrapper that
 * requires injectables matching the key.
 *
 * <p>The instantiator is not only responsible for creating instances using injectables
 * matching a given key, but also defines how these instances can be created including
 * what restrictions must apply on the number of matching injectables.
 *
 * <p>For example, an instantiator decides whether the instance it produces is
 * allowed to be optional, in which case the {@link InstantiationContext} should resolve
 * the key associated with the instantiator to 0 or 1 injectables. In contrast, instantiators that
 * produce collections could allow any number of matching injectables.
 *
 * @param <T> the produced type
 */
public interface Instantiator<T> {

  /**
   * Returns the {@link Key} this instantiator requires to produce its instances. Depending on
   * the type of instance produced, the {@link InstantiationContext} must have the key
   * available at least, at most or exactly once, or can have it available more than once.
   *
   * @return a {@link Key}, never null
   */
  Key getKey();

  /**
   * Returns an instance of the type this instantiator produces. It is allowed to return
   * {@code null} to indicate the absence of a value. Depending on the destination where
   * the value is used this can mean {@code null} is injected (methods and constructors),
   * that the value is not injected (fields) or that an {@link UnsatisfiedResolutionException}
   * is thrown (direct instance resolver call).
   *
   * <p>Note: the type of the instance produced is not necessarily the same as the
   * key the instantiator requires, for example when a collection of instances
   * matching the key is returned.
   *
   * @param context an {@link InstantiationContext}, never null
   * @return an instance of the type this instantiator produces, can be {@code null}
   * @throws CreationException when the instance could not be created
   * @throws AmbiguousResolutionException when multiple instances matched but the instantiator required at most one
   * @throws UnsatisfiedResolutionException when no instance matched but the instantiator required at least one
   * @throws ScopeNotActiveException when the scope for the produced type is not active
   */
  T getInstance(InstantiationContext context) throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException, ScopeNotActiveException;

  /**
   * Returns the {@link TypeTrait}s of this {@link Instantiator}.
   *
   * @return a set of {@link TypeTrait}, never {@code null} or contains {@code null}, but can be empty
   */
  Set<TypeTrait> getTypeTraits();
}