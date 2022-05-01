package hs.ddif.core.discovery;

import hs.ddif.core.definition.Injectable;
import hs.ddif.core.store.Resolver;
import hs.ddif.spi.instantiation.Key;

import java.lang.reflect.Type;
import java.util.Collection;

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
public interface DiscovererFactory {

  /**
   * Given an {@link Injectable}, returns a {@link Discoverer} which will produce the
   * given injectable and all injectables that could be derived or discovered using
   * the injectable as starting point.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param injectable a list of {@link Type}s, cannot be {@code null} or contain {@code null}s
   * @return a {@link Discoverer}, never {@code null}
   */
  Discoverer create(Resolver<Injectable<?>> resolver, Injectable<?> injectable);

  /**
   * Given a list of {@link Type}s, returns a {@link Discoverer} which will produce
   * injectables for each of the types given and all injectables that could be derived
   * or discovered using the given types as starting point.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param types a collection of {@link Type}s, cannot be {@code null} or contain {@code null}s
   * @return a {@link Discoverer}, never {@code null}
   */
  Discoverer create(Resolver<Injectable<?>> resolver, Collection<Type> types);

  /**
   * Given a {@link Key}, returns a {@link Discoverer} which will produce an injectable for
   * the given key and all injectables that could be derived or discovered using the given
   * key as starting point.
   *
   * <p>Note that if the given {@link Key} has qualifiers that the resulting injectable
   * must be a match for these qualifiers, if not an exception is thrown.
   *
   * <p>If the given {@link Key} is already resolvable or discovery is not allowed then
   * the {@link Discoverer} will return the empty set.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param key a {@link Key}, cannot be {@code null}
   * @return a {@link Discoverer}, never {@code null}
   */
  Discoverer create(Resolver<Injectable<?>> resolver, Key key);
}