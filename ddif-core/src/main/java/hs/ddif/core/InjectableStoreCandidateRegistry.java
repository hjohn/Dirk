package hs.ddif.core;

import hs.ddif.api.CandidateRegistry;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.discovery.Discoverer;
import hs.ddif.core.discovery.DiscovererFactory;
import hs.ddif.core.inject.store.InjectableStore;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 * An implementation of a {@link CandidateRegistry} backed by an {@link InjectableStore}.
 */
class InjectableStoreCandidateRegistry implements CandidateRegistry {
  private final InjectableStore store;
  private final DiscovererFactory discovererFactory;
  private final InstanceInjectableFactory instanceInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param store an {@link InjectableStore}, cannot be {@code null}
   * @param discovererFactory a {@link DiscovererFactory}, cannot be {@code null}
   * @param instanceInjectableFactory an {@link InstanceInjectableFactory}, cannot be {@code null}
   */
  public InjectableStoreCandidateRegistry(InjectableStore store, DiscovererFactory discovererFactory, InstanceInjectableFactory instanceInjectableFactory) {
    this.store = store;
    this.discovererFactory = discovererFactory;
    this.instanceInjectableFactory = instanceInjectableFactory;
  }

  @Override
  public boolean contains(Type type, Object... qualifiers) {
    return store.contains(KeyFactory.of(type, qualifiers));
  }

  @Override
  public void register(Type type) {
    registerInternal(List.of(type));
  }

  @Override
  public void register(List<Type> types) {
    registerInternal(types);
  }

  @Override
  public void registerInstance(Object instance, Annotation... qualifiers) {
    store.putAll(discovererFactory.create(store, instanceInjectableFactory.create(instance, qualifiers)).discover());
  }

  @Override
  public void remove(Type type) {
    removeInternal(List.of(type));
  }

  @Override
  public void remove(List<Type> types) {
    removeInternal(types);
  }

  @Override
  public void removeInstance(Object instance) {
    store.removeAll(discovererFactory.create(store, instanceInjectableFactory.create(instance)).discover());
  }

  private void registerInternal(List<Type> types) {
    Discoverer discoverer = discovererFactory.create(store, types);

    try {
      store.putAll(discoverer.discover());
    }
    catch(Exception e) {
      if(!discoverer.getProblems().isEmpty()) {
        e.addSuppressed(new DiscoveryException(discoverer.getProblems()));
      }

      throw e;
    }
  }

  private void removeInternal(List<Type> types) {
    Discoverer discoverer = discovererFactory.create(store, types);

    try {
      store.removeAll(discoverer.discover());
    }
    catch(Exception e) {
      if(!discoverer.getProblems().isEmpty()) {
        e.addSuppressed(new DiscoveryException(discoverer.getProblems()));
      }

      throw e;
    }
  }
}
