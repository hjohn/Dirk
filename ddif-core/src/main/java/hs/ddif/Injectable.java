package hs.ddif;

import java.lang.reflect.AccessibleObject;
import java.util.Map;
import java.util.Set;

/**
 * Represents a source for an injectable dependency.  Injectables can be
 * provided in various ways, either by creating a new instance of a class,
 * by using a known instance or by asking a provider for an instance.
 */
public interface Injectable {

  /**
   * Returns whether or not this Injectable requires any form of injection
   * (constructor, field or method injection).
   *
   * @return <code>true</code> if the injectable needs injection, otherwise <code>false</code>
   */
  boolean needsInjection();

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
   * @return an instance of the type provided by this {@link Injectable}, or <code>null</code> if the bean could not be provided
   */
  Object getInstance(Injector injector, Map<AccessibleObject, Binding> bindings);

  /**
   * Returns the qualifiers associated with this Injectable.
   *
   * @return the qualifiers associated with this Injectable
   */
  Set<AnnotationDescriptor> getQualifiers();
}
