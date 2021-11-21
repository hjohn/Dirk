package hs.ddif.core.inject.instantiator;

import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.store.Injectable;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * An {@link Injectable} which can be resolved to an instance.
 */
public interface ResolvableInjectable extends Injectable {

  /**
   * Returns an instance of the type provided by this {@link Injectable}.
   *
   * @param instantiator the {@link Instantiator} to use to resolve dependencies, cannot be null
   * @param parameters zero or more {@link NamedParameter} required for constructing the provided instance
   * @return an instance of the type provided by this {@link Injectable}, or <code>null</code> if it could not be provided
   * @throws InstanceCreationFailure when instantiation of this injectable fails
   */
  Object getInstance(Instantiator instantiator, NamedParameter... parameters) throws InstanceCreationFailure;

  /**
   * Returns the {@link Binding}s detected.
   *
   * @return a list {@link Binding}s, never null, can be empty if no bindings are detected.
   */
  List<Binding> getBindings();

  /**
   * Returns the scope of this {@link Injectable}.
   *
   * @return the scope of this {@link Injectable}, can be <code>null</code>
   */
  Annotation getScope();
}
