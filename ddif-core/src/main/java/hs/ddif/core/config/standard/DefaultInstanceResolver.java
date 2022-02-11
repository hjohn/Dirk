package hs.ddif.core.config.standard;

import hs.ddif.core.api.InstanceResolver;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.config.gather.DiscoveryFailure;
import hs.ddif.core.config.gather.Gatherer;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.inject.store.InjectableStore;
import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.Instantiator;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.domain.InstanceResolutionFailure;
import hs.ddif.core.instantiation.domain.MultipleInstances;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.store.Key;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implements the {@link InstanceResolver} interface.
 */
public class DefaultInstanceResolver implements InstanceResolver {
  private final InjectableStore store;
  private final Gatherer gatherer;
  private final InstantiationContext instantiationContext;
  private final InstantiatorFactory instantiatorFactory;

  /**
   * Constructs a new instance.
   *
   * @param store an {@link InjectableStore}, cannot be {@code null}
   * @param gatherer a {@link Gatherer}, cannot be {@code null}
   * @param instantiationContext an {@link InstantiationContext}, cannot be {@code null}
   * @param instantiatorFactory an {@link InstantiatorFactory}, cannot be {@code null}
   */
  public DefaultInstanceResolver(InjectableStore store, Gatherer gatherer, InstantiationContext instantiationContext, InstantiatorFactory instantiatorFactory) {
    this.store = store;
    this.gatherer = gatherer;
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
  public synchronized <T> List<T> getInstances(Type type, Predicate<Type> predicate, Object... qualifiers) {
    try {
      return getInstances(KeyFactory.of(type, qualifiers), predicate);
    }
    catch(InstanceCreationFailure f) {
      throw f.toRuntimeException();
    }
  }

  @Override
  public synchronized <T> List<T> getInstances(Class<T> cls, Predicate<Type> predicate, Object... qualifiers) {
    return getInstance((Type)cls, predicate, qualifiers);
  }

  @Override
  public synchronized <T> List<T> getInstances(Type type, Object... qualifiers) {
    return getInstances(type, null, qualifiers);
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
    Instantiator<T> instanceFactory = instantiatorFactory.getInstantiator(key, null);
    Set<Injectable> gatheredInjectables = gatherer.gather(store, instanceFactory.getKey());

    if(!gatheredInjectables.isEmpty()) {
      try {
        store.putAll(gatheredInjectables);
      }
      catch(Exception e) {
        throw new DiscoveryFailure(key, "instantiation failed because auto discovery was unable to resolve all dependencies; found: " + gatheredInjectables.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()), e);
      }
    }

    try {
      return instanceFactory.getInstance(instantiationContext);
    }
    catch(Exception e) {
      if(!gatheredInjectables.isEmpty()) {
        store.removeAll(gatheredInjectables);
      }

      throw e;
    }
  }

  private <T> List<T> getInstances(Key key, Predicate<Type> typePredicate) throws InstanceCreationFailure {
    return instantiationContext.createAll(key, typePredicate);
  }
}
