package hs.ddif;

import java.lang.reflect.AccessibleObject;
import java.util.Map;

/**
 * Represents a source for an injectable dependency.  Injectables can be
 * provided in various ways, either by creating a new instance of a class,
 * by using a known instance or by asking a provider for an instance.
 */
public interface Injectable {

  /**
   * Checks whether this injectable can be instantiated.  Depending on
   * the type of {@link Injectable} this may never fail.
   *
   * @param bindings the bindings to check for possible ways to instantiate the injectable
   * @return <code>true</code> if the injectable can be provided, otherwise <code>false</code>
   */
  boolean canBeInstantiated(Map<AccessibleObject, Binding> bindings);

  /**
   * Returns the type of the resulting instance provided by this {@link Injectable}.
   *
   * @return the type of the resulting instance provided by this {@link Injectable}
   */
  Class<?> getInjectableClass();

  /**
   * Returns an instance of the type provided by this {@link Injectable}.
   *
   * @param injector the injector to use to resolve dependencies
   * @param bindings the bindings to use
   * @return an instance of the type provided by this {@link Injectable}
   */
  Object getInstance(Injector injector, Map<AccessibleObject, Binding> bindings);
}
