package hs.ddif.core.inject.store;

import hs.ddif.core.api.CandidateRegistry;
import hs.ddif.core.inject.instantiator.Gatherer;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An implementation of a {@link CandidateRegistry} backed by an {@link InjectableStore}.
 */
public class InjectableStoreCandidateRegistry implements CandidateRegistry {
  private final InjectableStore<ResolvableInjectable> store;
  private final Gatherer gatherer;
  private final ClassInjectableFactory classInjectableFactory;
  private final InstanceInjectableFactory instanceInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param store an {@link InjectableStore}, cannot be null
   * @param gatherer a {@link Gatherer}, cannot be null
   * @param classInjectableFactory a {@link ClassInjectableFactory}, cannot be null
   * @param instanceInjectableFactory an {@link InstanceInjectableFactory}, cannot be null
   */
  public InjectableStoreCandidateRegistry(InjectableStore<ResolvableInjectable> store, Gatherer gatherer, ClassInjectableFactory classInjectableFactory, InstanceInjectableFactory instanceInjectableFactory) {
    this.store = store;
    this.gatherer = gatherer;
    this.classInjectableFactory = classInjectableFactory;
    this.instanceInjectableFactory = instanceInjectableFactory;
  }

  @Override
  public boolean contains(Type type, Object... criteria) {
    return store.contains(type, criteria);
  }

  @Override
  public void register(Type concreteType) {
    registerInternal(List.of(classInjectableFactory.create(concreteType)));
  }

  @Override
  public void register(List<Type> concreteTypes) {
    registerInternal(concreteTypes.stream().map(classInjectableFactory::create).collect(Collectors.toList()));
  }

  @Override
  public void registerInstance(Object instance, AnnotationDescriptor... qualifiers) {
    registerInternal(List.of(instanceInjectableFactory.create(instance, qualifiers)));
  }

  @Override
  public void remove(Type concreteType) {
    removeInternal(List.of(classInjectableFactory.create(concreteType)));
  }

  @Override
  public void remove(List<Type> concreteTypes) {
    removeInternal(concreteTypes.stream().map(classInjectableFactory::create).collect(Collectors.toList()));
  }

  @Override
  public void removeInstance(Object instance) {
    removeInternal(List.of(instanceInjectableFactory.create(instance)));
  }

  private void registerInternal(List<ResolvableInjectable> injectables) {
    store.putAll(gatherer.gather(injectables));
  }

  private void removeInternal(List<ResolvableInjectable> injectables) {
    store.removeAll(gatherer.gather(injectables));
  }
}
