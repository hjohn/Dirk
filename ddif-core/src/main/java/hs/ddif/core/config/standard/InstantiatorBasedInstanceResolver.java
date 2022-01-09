package hs.ddif.core.config.standard;

import hs.ddif.core.api.InstanceResolver;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.inject.instantiation.InstanceCreationFailure;
import hs.ddif.core.inject.instantiation.InstanceResolutionFailure;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.scope.OutOfScopeException;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Implements the {@link InstanceResolver} interface by wrapping an {@link Instantiator}
 * and converting its checked exceptions to runtime ones.
 */
public class InstantiatorBasedInstanceResolver implements InstanceResolver {
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
  public synchronized <T> T getInstance(Type type, Object... criteria) {
    try {
      CriteriaParser parser = new CriteriaParser(type, criteria);

      return instantiator.getInstance(parser.getKey(), parser.getMatchers());
    }
    catch(InstanceResolutionFailure f) {
      throw f.toRuntimeException();
    }
    catch(OutOfScopeException e) {
      throw new NoSuchInstanceException(e.getMessage(), e);
    }
  }

  @Override
  public synchronized <T> T getInstance(Class<T> cls, Object... criteria) {
    return getInstance((Type)cls, criteria);
  }

  @Override
  public synchronized <T> List<T> getInstances(Type type, Object... criteria) {
    try {
      CriteriaParser parser = new CriteriaParser(type, criteria);

      return instantiator.getInstances(parser.getKey(), List.of());
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
