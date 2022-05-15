package hs.ddif.core;

import hs.ddif.api.InstanceResolver;
import hs.ddif.api.instantiation.AmbiguousResolutionException;
import hs.ddif.api.instantiation.CreationException;
import hs.ddif.api.instantiation.UnsatisfiedResolutionException;
import hs.ddif.api.scope.ScopeNotActiveException;
import hs.ddif.spi.instantiation.InjectionTarget;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.Key;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Implements the {@link InstanceResolver} interface.
 */
class DefaultInstanceResolver implements InstanceResolver {
  private final InstantiationContext instantiationContext;
  private final InstantiatorFactory instantiatorFactory;

  /**
   * Constructs a new instance.
   *
   * @param instantiationContext an {@link InstantiationContext}, cannot be {@code null}
   * @param instantiatorFactory an {@link InstantiatorFactory}, cannot be {@code null}
   */
  public DefaultInstanceResolver(InstantiationContext instantiationContext, InstantiatorFactory instantiatorFactory) {
    this.instantiationContext = instantiationContext;
    this.instantiatorFactory = instantiatorFactory;
  }

  @Override
  public synchronized <T> T getInstance(Type type, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    Key key = KeyFactory.of(type, qualifiers);
    T instance = getInstance(key);

    if(instance == null) {
      throw new UnsatisfiedResolutionException("No such instance: [" + key + "]");
    }

    return instance;
  }

  @Override
  public synchronized <T> T getInstance(Class<T> cls, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
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

  private <T> T getInstance(Key key) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException {
    return instantiatorFactory.<T>getInstantiator(new InternalInjectionTarget(key)).getInstance(instantiationContext);
  }

  private <T> List<T> getInstances(Key key) throws CreationException {
    return instantiationContext.createAll(key);
  }

  private static class InternalInjectionTarget implements InjectionTarget {
    private final Key key;

    public InternalInjectionTarget(Key key) {
      this.key = key;
    }

    @Override
    public Key getKey() {
      return key;
    }

    @Override
    public boolean isOptional() {
      return false;
    }
  }
}
