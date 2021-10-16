package hs.ddif.core.inject.instantiator;

import hs.ddif.core.Injector;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Interface for automatically discovering new {@link ResolvableInjectable}s when the
 * {@link Injector} is queried for a unknown type.<p>
 *
 * Implementations should provide one or more {@link ResolvableInjectable} which
 * can be added to the {@link Injector}s store which will then allow the initially
 * requested type to be instantiated.
 */
public interface InjectableDiscoverer {

  /**
   * Discover new {@link ResolvableInjectable}s which can be added to the
   * {@link Injector}s store which will then allow the requested type to be
   * instantiated, or throw {@link DiscoveryException} when discovery failed.
   *
   * @param resolver a {@link Resolver} which can be used to find and resolve
   *   types already known to the {@link Injector}, never null
   * @param type a {@link Type} to discover {@link ResolvableInjectable}s for, never null
   * @return a list of {@link ResolvableInjectable}, never null or empty and never contains nulls
   * @throws DiscoveryException when discovery encountered a situation it cannot resolve
   */
  List<ResolvableInjectable> discover(Resolver<ResolvableInjectable> resolver, Class<?> type) throws DiscoveryException;
}
