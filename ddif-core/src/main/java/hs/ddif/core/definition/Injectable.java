package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

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
public interface Injectable {

  /**
   * Returns the {@link QualifiedType} which is always fully resolved (no type variables)
   * and never {@code void}.
   *
   * @return a {@link QualifiedType}, never {@code null}
   */
  QualifiedType getQualifiedType();

  /**
   * Returns the {@link Type} which is always fully resolved (no type variables)
   * and never {@code void}.
   *
   * @return the {@link Type}, never {@code null}
   */
  default Type getType() {
    return getQualifiedType().getType();
  }

  /**
   * Returns an unmodifiable set of qualifier {@link Annotation}s.
   *
   * @return an unmodifiable set of qualifier {@link Annotation}s, never {@code null} and never contains {@code null}s but can be empty
   */
  default Set<Annotation> getQualifiers() {
    return getQualifiedType().getQualifiers();
  }

  /**
   * Returns the {@link Binding}s detected.
   *
   * @return a list {@link Binding}s, never {@code null}, can be empty if no bindings are detected
   */
  List<Binding> getBindings();

  /**
   * Returns the {@link ScopeResolver} of this {@link Injectable}.
   *
   * @return the {@link ScopeResolver} of this {@link Injectable}, never {@code null}
   */
  ScopeResolver getScopeResolver();

  /**
   * Creates an instance.
   *
   * @param injections a list of {@link Injection} containing values to be injected, never {@code null} or contains {@code null}s but can be empty
   * @return an instance, or {@code null} if it could not be provided
   * @throws InstanceCreationFailure when instantiation fails
   */
  Object createInstance(List<Injection> injections) throws InstanceCreationFailure;

  /**
   * Destroys an instance.
   *
   * @param instance an instance, cannot be {@code null}
   * @param injections a list of {@link Injection} used to create the instance, cannot be {@code null} or contain {@code null}s but can be empty
   */
  void destroyInstance(Object instance, List<Injection> injections);
}
