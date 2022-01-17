package hs.ddif.core.config.gather;

import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * Gathers fully expanded sets of {@link Injectable}s based on the given inputs.
 *
 * <p>A distinction is made between derived and discovered injectables. Derived
 * injectables are detected by examining a given {@link Type}, for example through
 * annotations. Discovered injectables are injectables created for unresolved
 * bindings of an input or derived injectable.
 *
 * <p>Derivation always takes precedence over discovery.
 *
 * <p>Implementations of this interface can scan given types (ie. for annotations)
 * to derive further injectables that can be supplied.
 */
public interface Gatherer {

  /**
   * Given an {@link Injectable}, returns the given injectable and all injectables
   * that could be derived or discovered using the injectable as starting point.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param injectable a list of {@link Type}s, cannot be {@code null} or contain {@code null}s
   * @return a set of {@link Injectable}s, never {@code null} or contains {@code null}s
   */
  Set<Injectable> gather(Resolver<Injectable> resolver, Injectable injectable);

  /**
   * Given a list of {@link Type}s, returns injectables for each of the types
   * given and all injectables that could be derived or discovered using
   * the given types as starting point.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param types a list of {@link Type}s, cannot be {@code null} or contain {@code null}s
   * @return a set of {@link Injectable}s, never {@code null} or contains {@code null}s
   */
  Set<Injectable> gather(Resolver<Injectable> resolver, List<Type> types);

  /**
   * Given a {@link Key}, returns an injectable for the given key and all injectables
   * that could be derived or discovered using the given key as starting point.
   *
   * <p>Note that if the given {@link Key} has qualifiers that the resulting injectable
   * must be a match for these qualifiers, if not an exception is thrown.
   *
   * <p>If the given {@link Key} is already resolvable or discovery is not allowed then
   * this method will return the empty set.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param key a {@link Key}, cannot be {@code null}
   * @return a set of {@link Injectable}s, never {@code null} or contains {@code null}s, but can be empty
   * @throws DiscoveryFailure when the given key or any discovered types cannot be converted into a suitable injectable
   */
  Set<Injectable> gather(Resolver<Injectable> resolver, Key key) throws DiscoveryFailure;
}