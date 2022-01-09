package hs.ddif.core.config.gather;

import hs.ddif.core.inject.injectable.ResolvableInjectable;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

/**
 * Gathers fully expanded sets of {@link ResolvableInjectable}s based on a given
 * input set or {@link Type}. Implementations of this interface can scan given
 * types (ie. for annotations) to derive further injectables that can be
 * supplied.
 */
public interface Gatherer {

  /**
   * Given a collection of {@link ResolvableInjectable}s, return a fully expanded
   * set of required injectables which are not part of the given resolver yet.
   *
   * @param resolver a {@link Resolver}, cannot be null
   * @param injectables a collection of {@link ResolvableInjectable}s, cannot be {@code null} or contain {@code null}
   * @return a fully expanded set of injectables, never {@code null} and never contains {@code null}
   */
  Set<ResolvableInjectable> gather(Resolver<ResolvableInjectable> resolver, Collection<ResolvableInjectable> injectables);

  /**
   * Given a {@link Type} and a set of criteria, return a fully expanded set of
   * required injectables which are not part of the given resolver yet.<p>
   *
   * Note, that if this gatherer is unable to automatically discover new
   * injectables, or the given type is unsuitable for automatic discovery, or
   * the type given is already resolvable then this method will return the empty set.
   *
   * @param resolver a {@link Resolver}, cannot be null
   * @param key a {@link Key}, cannot be {@code null}
   * @return a fully expanded set of injectables, never {@code null} and never contains {@code null}, but can be empty
   * @throws DiscoveryFailure when the given type cannot be converted into a suitable injectable
   */
  Set<ResolvableInjectable> gather(Resolver<ResolvableInjectable> resolver, Key key) throws DiscoveryFailure;
}