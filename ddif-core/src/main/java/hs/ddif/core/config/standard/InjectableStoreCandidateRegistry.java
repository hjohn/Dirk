package hs.ddif.core.config.standard;

import hs.ddif.core.api.CandidateRegistry;
import hs.ddif.core.config.gather.Gatherer;
import hs.ddif.core.inject.injectable.ClassInjectableFactory;
import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.inject.injectable.InstanceInjectableFactory;
import hs.ddif.core.store.QualifiedTypeStore;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An implementation of a {@link CandidateRegistry} backed by an {@link QualifiedTypeStore}.
 */
public class InjectableStoreCandidateRegistry implements CandidateRegistry {
  private final QualifiedTypeStore<Injectable> store;
  private final Gatherer gatherer;
  private final ClassInjectableFactory classInjectableFactory;
  private final InstanceInjectableFactory instanceInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param store an {@link QualifiedTypeStore}, cannot be {@code null}
   * @param gatherer a {@link Gatherer}, cannot be {@code null}
   * @param classInjectableFactory a {@link ClassInjectableFactory}, cannot be {@code null}
   * @param instanceInjectableFactory an {@link InstanceInjectableFactory}, cannot be {@code null}
   */
  public InjectableStoreCandidateRegistry(QualifiedTypeStore<Injectable> store, Gatherer gatherer, ClassInjectableFactory classInjectableFactory, InstanceInjectableFactory instanceInjectableFactory) {
    this.store = store;
    this.gatherer = gatherer;
    this.classInjectableFactory = classInjectableFactory;
    this.instanceInjectableFactory = instanceInjectableFactory;
  }

  @Override
  public boolean contains(Type type, Object... criteria) {
    CriteriaParser parser = new CriteriaParser(type, criteria);

    return store.contains(parser.getKey(), parser.getMatchers());
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
  public void registerInstance(Object instance, Annotation... qualifiers) {
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

  private void registerInternal(List<Injectable> injectables) {
    store.putAll(gatherer.gather(store, injectables));
  }

  private void removeInternal(List<Injectable> injectables) {
    store.removeAll(gatherer.gather(store, injectables));
  }
}
