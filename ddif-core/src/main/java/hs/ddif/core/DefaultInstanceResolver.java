package hs.ddif.core;

import hs.ddif.api.InstanceResolver;
import hs.ddif.api.instantiation.InstantiationContext;
import hs.ddif.api.instantiation.Instantiator;
import hs.ddif.api.instantiation.InstantiatorFactory;
import hs.ddif.api.instantiation.domain.InstanceCreationFailure;
import hs.ddif.api.instantiation.domain.InstanceResolutionFailure;
import hs.ddif.api.instantiation.domain.Key;
import hs.ddif.api.instantiation.domain.MultipleInstances;
import hs.ddif.api.instantiation.domain.NoSuchInstance;
import hs.ddif.api.instantiation.domain.NoSuchInstanceException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.discovery.Discoverer;
import hs.ddif.core.discovery.DiscovererFactory;
import hs.ddif.core.discovery.DiscoveryFailure;
import hs.ddif.core.inject.store.InjectableStore;

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
  public synchronized <T> T getInstance(Type type, Object... qualifiers) {
    try {
      Key key = KeyFactory.of(type, qualifiers);
      T instance = getInstance(key);

      if(instance == null) {
        throw new NoSuchInstanceException("No such instance: " + key, null);
      }

      return instance;
    }
    catch(InstanceResolutionFailure f) {
      throw f.toRuntimeException();
    }
  }

  @Override
  public synchronized <T> T getInstance(Class<T> cls, Object... qualifiers) {
    return getInstance((Type)cls, qualifiers);
  }

  @Override
  public synchronized <T> List<T> getInstances(Type type, Object... qualifiers) {
    try {
      return getInstances(KeyFactory.of(type, qualifiers));
    }
    catch(InstanceCreationFailure f) {
      throw f.toRuntimeException();
    }
  }

  @Override
  public synchronized <T> List<T> getInstances(Class<T> cls, Object... qualifiers) {
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

  private <T> T getInstance(Key key) throws NoSuchInstance, MultipleInstances, InstanceCreationFailure {
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
      DiscoveryFailure f = new DiscoveryFailure(key, "instantiation failed because auto discovery was unable to resolve all dependencies; found: " + gatheredInjectables.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()), e);

      if(!discoverer.getProblems().isEmpty()) {
        f.addSuppressed(new DiscoveryException(discoverer.getProblems()));
      }

      throw f;
    }

    try {
      return instantiator.getInstance(instantiationContext);
    }
    catch(Exception e) {
      if(!gatheredInjectables.isEmpty()) {
        store.removeAll(gatheredInjectables);
      }

      throw e;
    }
  }

  private <T> List<T> getInstances(Key key) throws InstanceCreationFailure {
    return instantiationContext.createAll(key);
  }
}
