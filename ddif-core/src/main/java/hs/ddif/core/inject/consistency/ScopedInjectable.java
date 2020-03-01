package hs.ddif.core.inject.consistency;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.store.Injectable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.Map;

/**
 * An {@link Injectable} with associated scope and binding information needed by
 * Injector.
 */
public interface ScopedInjectable extends Injectable {

  /**
   * Returns the {@link Binding}s required.
   *
   * @return a {@link Map} of {@link Binding}[], never null, can be empty if no bindings are needed.
   */
  Map<AccessibleObject, Binding[]> getBindings();

  /**
   * Returns the scope of this {@link Injectable}.
   *
   * @return the scope of this {@link Injectable}, can be <code>null</code>
   */
  Annotation getScope();

  /**
   * Returns <code>true</code> if the injectable can produce multiple instances of the class it
   * represents, otherwise <code>false</code>.<p>
   *
   * @return <code>true</code> if the injectable can produce multiple instances of the class it represents, otherwise <code>false</code>
   */
  boolean isTemplate();
}
