package hs.ddif.core.inject.instantiator;

import hs.ddif.core.store.Injectable;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * An {@link Injectable} with binding information.
 */
public interface DependentInjectable extends Injectable {

  /**
   * Returns the {@link Binding}s detected.
   *
   * @return a list {@link Binding}s, never null, can be empty if no bindings are detected
   */
  List<Binding> getBindings();

  /**
   * Returns the scope of this {@link Injectable}.
   *
   * @return the scope of this {@link Injectable}, can be <code>null</code>
   */
  Annotation getScope();
}
