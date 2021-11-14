package hs.ddif.core.store;

import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Represents a source for an injectable dependency.  Injectables can be
 * provided in various ways, either by creating a new instance of a class,
 * by using a known instance or by asking a provider for an instance.<p>
 *
 * Implementations of this interface must provide implementations of {@link Object#equals(Object)}
 * and {@link Object#hashCode()}. Two injectables are considered equal when the
 * full generic type is equal and they come from the exact same source (same class,
 * same method, same field, etc.)
 */
public interface Injectable {

  /**
   * Returns the type of the resulting instance provided by this {@link Injectable}.
   *
   * @return the type of the resulting instance provided by this {@link Injectable}, never null
   */
  Type getType();

  /**
   * Returns the qualifiers associated with this Injectable.
   *
   * @return the qualifiers associated with this Injectable, never null but can be empty
   */
  Set<AnnotationDescriptor> getQualifiers();
}
