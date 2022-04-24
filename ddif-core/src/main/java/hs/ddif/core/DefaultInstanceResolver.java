package hs.ddif.core;

import hs.ddif.api.InstanceResolver;
import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.api.instantiation.AmbiguousResolutionException;
import hs.ddif.api.instantiation.CreationException;
import hs.ddif.api.instantiation.UnsatisfiedResolutionException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.discovery.Discoverer;
import hs.ddif.core.discovery.DiscovererFactory;
import hs.ddif.core.inject.store.InjectableStore;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.Instantiator;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.Key;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implements the {@link InstanceResolver} interface.
 */
class DefaultInstanceResolver implements InstanceResolver {
  private final InjectableStore store;
  private final DiscovererFactory discovererFactory;
  private final InstantiationContext instantiationContext;
  private final InstantiatorFactory instantiatorFactory;

  /**
   * Constructs a new instance.
   *
   * @param store an {@link InjectableStore}, cannot be {@code null}
   * @param discovererFactory a {@link DiscovererFactory}, cannot be {@code null}
   * @param instantiationContext an {@link InstantiationContext}, cannot be {@code null}
   * @param instantiatorFactory an {@link InstantiatorFactory}, cannot be {@code null}
   */
  public DefaultInstanceResolver(InjectableStore store, DiscovererFactory discovererFactory, InstantiationContext instantiationContext, InstantiatorFactory instantiatorFactory) {
    this.store = store;
    this.discovererFactory = discovererFactory;
    this.instantiationContext = instantiationContext;
    this.instantiatorFactory = instantiatorFactory;
  }

  @Override
  public synchronized <T> T getInstance(Type type, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, AutoDiscoveryException {
    Key key = KeyFactory.of(type, qualifiers);
    T instance = getInstance(key);

    if(instance == null) {
      throw new UnsatisfiedResolutionException("No such instance: [" + key + "]");
    }

    return instance;
  }

  @Override
  public synchronized <T> T getInstance(Class<T> cls, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, AutoDiscoveryException {
    return getInstance((Type)cls, qualifiers);
  }

  @Override
  public synchronized <T> List<T> getInstances(Type type, Object... qualifiers) throws CreationException {
    return getInstances(KeyFactory.of(type, qualifiers));
  }

  @Override
  public synchronized <T> List<T> getInstances(Class<T> cls, Object... qualifiers) throws CreationException {
    return getInstances((Type)cls, qualifiers);
  }

  /**
   * Returns the underlying store this resolver uses.
   *
   * @return a {@link InjectableStore}, never null
   */
  public InjectableStore getStore() {
    return store;
  }

  private <T> T getInstance(Key key) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, AutoDiscoveryException {
    Instantiator<T> instantiator = instantiatorFactory.getInstantiator(key, null);
    Discoverer discoverer = discovererFactory.create(store, instantiator.getKey());
    Set<Injectable<?>> gatheredInjectables = Set.of();

    try {
      gatheredInjectables = discoverer.discover();

      if(!gatheredInjectables.isEmpty()) {
        store.putAll(gatheredInjectables);
      }
    }
    catch(Exception e) {
      if(gatheredInjectables.isEmpty()) {
        throw new AutoDiscoveryException("Unable to instantiate [" + key + "]" + (discoverer.getProblems().isEmpty() ? "" : discoverer.getProblems().stream().collect(Collectors.joining("\n    -> ", "\n    -> ", ""))), e);
      }

      throw new AutoDiscoveryException("[" + key + "] and the discovered types " + gatheredInjectables.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()) + " could not be registered" + (discoverer.getProblems().isEmpty() ? "" : discoverer.getProblems().stream().collect(Collectors.joining("\n    -> ", "\n    -> ", ""))), e);
    }

    try {
      return instantiator.getInstance(instantiationContext);
    }
    catch(Exception e) {
      if(!gatheredInjectables.isEmpty()) {
        try {
          store.removeAll(gatheredInjectables);
        }
        catch(Exception e2) {  // this shouldn't occur, as it is always possible to unregister what was just registered
          throw new IllegalStateException("Bad state, unable to unregister: " + gatheredInjectables);
        }
      }

      throw e;
    }
  }

  private <T> List<T> getInstances(Key key) throws CreationException {
    return instantiationContext.createAll(key);
  }
}
