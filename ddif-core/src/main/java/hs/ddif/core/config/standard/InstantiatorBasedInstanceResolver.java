package hs.ddif.core.config.standard;

import hs.ddif.core.api.InstanceResolver;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.inject.instantiation.InstanceCreationFailure;
import hs.ddif.core.inject.instantiation.InstanceResolutionFailure;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.scope.OutOfScopeException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Predicate;

/**
 * Implements the {@link InstanceResolver} interface by wrapping an {@link Instantiator}
 * and converting its checked exceptions to runtime ones.
 */
public class InstantiatorBasedInstanceResolver implements InstanceResolver {
  private final Instantiator instantiator;

  /**
   * Constructs a new instance.
   *
   * @param instantiator an {@link Instantiator}, cannot be {@code null}
   */
  public InstantiatorBasedInstanceResolver(Instantiator instantiator) {
    this.instantiator = instantiator;
  }

  @Override
  public synchronized <T> T getInstance(Type type, Object... qualifiers) {
    try {
      return instantiator.getInstance(KeyFactory.of(type, qualifiers));
    }
    catch(InstanceResolutionFailure f) {
      throw f.toRuntimeException();
    }
    catch(OutOfScopeException e) {
      throw new NoSuchInstanceException(e.getMessage(), e);
    }
  }

  @Override
  public synchronized <T> T getInstance(Class<T> cls, Object... qualifiers) {
    return getInstance((Type)cls, qualifiers);
  }

  @Override
  public synchronized <T> List<T> getInstances(Type type, Predicate<Type> predicate, Object... qualifiers) {
    try {
      return instantiator.getInstances(KeyFactory.of(type, qualifiers), predicate);
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
}
