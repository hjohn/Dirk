package hs.ddif.core;

import hs.ddif.core.store.Injectable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.Map;

/**
 * An {@link Injectable} with associated scope and binding information needed by
 * {@link Injector}.
 */
public interface ScopedInjectable extends Injectable {

  /**
   * Returns an instance of the type provided by this {@link Injectable}.
   *
   * @param injector the injector to use to resolve dependencies
   * @return an instance of the type provided by this {@link Injectable}, or <code>null</code> if the bean could not be provided
   */
  Object getInstance(Injector injector);

  /**
   * Returns the {@link Binding}s required.
   *
   * @return a {@link Map} of {@link Binding}[], never null, can be empty if no bindings are needed.
   */
  Map<AccessibleObject, Binding[]> getBindings();

  /**
   * Returns the scope of this {@link Injectable}.
   *
   * @return the scope of this {@link Injectable}
   */
  Annotation getScope();
}
