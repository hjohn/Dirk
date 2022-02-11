package hs.ddif.core.config.standard;

import hs.ddif.core.api.CandidateRegistry;
import hs.ddif.core.config.gather.Gatherer;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.inject.store.InjectableStore;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 * An implementation of a {@link CandidateRegistry} backed by an {@link InjectableStore}.
 */
public class InjectableStoreCandidateRegistry implements CandidateRegistry {
  private final InjectableStore store;
  private final Gatherer gatherer;
  private final InstanceInjectableFactory instanceInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param store an {@link InjectableStore}, cannot be {@code null}
   * @param gatherer a {@link Gatherer}, cannot be {@code null}
   * @param instanceInjectableFactory an {@link InstanceInjectableFactory}, cannot be {@code null}
   */
  public InjectableStoreCandidateRegistry(InjectableStore store, Gatherer gatherer, InstanceInjectableFactory instanceInjectableFactory) {
    this.store = store;
    this.gatherer = gatherer;
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
    store.putAll(gatherer.gather(store, instanceInjectableFactory.create(instance, qualifiers)));
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
    store.removeAll(gatherer.gather(store, instanceInjectableFactory.create(instance)));
  }

  private void registerInternal(List<Type> types) {
    store.putAll(gatherer.gather(store, types));
  }

  private void removeInternal(List<Type> types) {
    store.removeAll(gatherer.gather(store, types));
  }
}
