package hs.ddif.core.inject.instantiator;

import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.consistency.ScopedInjectable;
import hs.ddif.core.store.Injectable;

import java.util.List;

/**
 * An {@link Injectable} which can be resolved to an instance.
 */
public interface ResolvableInjectable extends ScopedInjectable {

  /**
   * Returns an instance of the type provided by this {@link Injectable}.
   *
   * @param instantiator the {@link Instantiator} to use to resolve dependencies
   * @param parameters zero or more {@link NamedParameter} required for constructing the provided instance
   * @return an instance of the type provided by this {@link Injectable}, or <code>null</code> if the bean could not be provided
   * @throws InstantiationException when instantiation of this injectable fails
   */
  Object getInstance(Instantiator instantiator, NamedParameter... parameters) throws InstantiationException;

  @Override
  List<ResolvableBinding> getBindings();
}
