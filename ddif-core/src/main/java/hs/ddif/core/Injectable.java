package hs.ddif.core;

import java.lang.annotation.Annotation;
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
   * Returns the {@link Binding}s required.
   *
   * @return a {@link Map} of {@link Binding}[], never null, can be empty if no bindings are needed.
   */
  Map<AccessibleObject, Binding[]> getBindings();

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
   * @return an instance of the type provided by this {@link Injectable}, or <code>null</code> if the bean could not be provided
   */
  Object getInstance(Injector injector);

  /**
   * Returns the qualifiers associated with this Injectable.
   *
   * @return the qualifiers associated with this Injectable
   */
  Set<AnnotationDescriptor> getQualifiers();

  /**
   * Returns the scope of this {@link Injectable}.
   *
   * @return the scope of this {@link Injectable}
   */
  Annotation getScope();
}
