package hs.ddif.core.scope;

import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.domain.MultipleInstances;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.instantiation.injection.Injection;

import java.util.List;

/**
 * Context used to create and destroy instances with injection information.
 */
public interface InjectionContext {

  /**
   * Returns a list of {@link Injection}s.
   *
   * @return a list of {@link Injection}s, never {@code null} or contains {@code null}s but can be empty
   * @throws InstanceCreationFailure when an instance could not be created
   * @throws MultipleInstances when multiple instances could be created but at most one was required
   * @throws NoSuchInstance when no instance could be created but at least one was required
   */
  List<Injection> getInjections() throws InstanceCreationFailure, MultipleInstances, NoSuchInstance;

  /**
   * Release this {@link InjectionContext}. Any dependent objects associated with it
   * will be destroyed.
   */
  void release();
}
