package hs.ddif.core.inject.instantiator;

import hs.ddif.core.api.InstanceResolver;
import hs.ddif.core.api.NamedParameter;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Implements the {@link InstanceResolver} interface by wrapping an {@link Instantiator}
 * and converting its checked exceptions to runtime ones.
 */
public class InstantiatorBasedInstanceResolver implements InstanceResolver {
  private static final NamedParameter[] NO_PARAMETERS = new NamedParameter[] {};

  private final Instantiator instantiator;

  /**
   * Constructs a new instance.
   *
   * @param instantiator an {@link Instantiator}, cannot be null
   */
  public InstantiatorBasedInstanceResolver(Instantiator instantiator) {
    this.instantiator = instantiator;
  }

  @Override
  public synchronized <T> T getParameterizedInstance(Type type, NamedParameter[] parameters, Object... criteria) {
    try {
      return instantiator.getParameterizedInstance(type, parameters, criteria);
    }
    catch(InstanceResolutionFailure f) {
      throw f.toRuntimeException();
    }
  }

  @Override
  public synchronized <T> T getInstance(Type type, Object... criteria) {
    return getParameterizedInstance(type, NO_PARAMETERS, criteria);
  }

  @Override
  public synchronized <T> T getInstance(Class<T> cls, Object... criteria) {
    return getInstance((Type)cls, criteria);
  }

  @Override
  public synchronized <T> List<T> getInstances(Type type, Object... criteria) {
    try {
      return instantiator.getInstances(type, criteria);
    }
    catch(InstanceCreationFailure f) {
      throw f.toRuntimeException();
    }
  }

  @Override
  public synchronized <T> List<T> getInstances(Class<T> cls, Object... criteria) {
    return getInstances((Type)cls, criteria);
  }
}
