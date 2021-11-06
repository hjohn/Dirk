package hs.ddif.core.inject.instantiator;

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
   * set of injectables.
   *
   * @param injectables a collection of {@link ResolvableInjectable}s, cannot be {@code null} or contain {@code null}
   * @return a fully expanded set of injectables, never {@code null} and never contains {@code null}
   */
  Set<ResolvableInjectable> gather(Collection<ResolvableInjectable> injectables);

  /**
   * Given a {@link Type}, return a fully expanded set of injectables.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @return a fully expanded set of injectables, never {@code null} or empty and never contains {@code null}
   * @throws DiscoveryException when the given type cannot be converted into a suitable injectable
   */
  Set<ResolvableInjectable> gather(Type type) throws DiscoveryException;
}