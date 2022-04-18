package hs.ddif.core;

import hs.ddif.api.CandidateRegistry;
import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.discovery.Discoverer;
import hs.ddif.core.discovery.DiscovererFactory;
import hs.ddif.core.inject.store.InjectableStore;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

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
  public void register(Type type) throws AutoDiscoveryException, DefinitionException {
    registerInternal(List.of(type));
  }

  @Override
  public void register(List<Type> types) throws AutoDiscoveryException, DefinitionException {
    registerInternal(types);
  }

  @Override
  public void registerInstance(Object instance, Annotation... qualifiers) throws DefinitionException {
    store.putAll(discovererFactory.create(store, instanceInjectableFactory.create(instance, qualifiers)).discover());
  }

  @Override
  public void remove(Type type) throws AutoDiscoveryException, DefinitionException {
    removeInternal(List.of(type));
  }

  @Override
  public void remove(List<Type> types) throws AutoDiscoveryException, DefinitionException {
    removeInternal(types);
  }

  @Override
  public void removeInstance(Object instance) throws DefinitionException {
    store.removeAll(discovererFactory.create(store, instanceInjectableFactory.create(instance)).discover());
  }

  private void registerInternal(List<Type> types) throws AutoDiscoveryException, DefinitionException {
    Discoverer discoverer = discovererFactory.create(store, types);

    try {
      store.putAll(discoverer.discover());
    }
    catch(Exception e) {
      if(discoverer.getProblems().isEmpty()) {
        throw e;
      }

      throw new AutoDiscoveryException("Unable to register " + types + discoverer.getProblems().stream().collect(Collectors.joining("\n    -> ", "\n    -> ", "")), e);
    }
  }

  private void removeInternal(List<Type> types) throws AutoDiscoveryException, DefinitionException {
    Discoverer discoverer = discovererFactory.create(store, types);

    try {
      store.removeAll(discoverer.discover());
    }
    catch(Exception e) {
      if(discoverer.getProblems().isEmpty()) {
        throw e;
      }

      throw new AutoDiscoveryException("Unable to register " + types + discoverer.getProblems().stream().collect(Collectors.joining("\n    -> ", "\n    -> ", "")), e);
    }
  }
}
