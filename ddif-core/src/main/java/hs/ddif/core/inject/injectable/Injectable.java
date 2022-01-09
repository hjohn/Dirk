package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.injection.Injection;
import hs.ddif.core.inject.instantiation.InstanceCreationFailure;
import hs.ddif.core.store.QualifiedType;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Represents a source for an injectable dependency.  Injectables can be
 * provided in various ways, either by creating a new instance of a class,
 * by using a known instance or by asking a provider for an instance.<p>
 *
 * Implementations of this interface must provide implementations of {@link Object#equals(Object)}
 * and {@link Object#hashCode()}. Two injectables are considered equal when the
 * full generic type is equal, their qualifiers are equal and they come from the
 * exact same source (same class, same method, same field, etc.)
 */
public interface Injectable extends QualifiedType {

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

  /**
   * Creates an instance.
   *
   * @param injections a list of {@link Injection} containing values to be injected, never {@code null} but can be empty
   * @return an instance, or {@code null} if it could not be provided
   * @throws InstanceCreationFailure when instantiation fails
   */
  Object createInstance(List<Injection> injections) throws InstanceCreationFailure;
}
