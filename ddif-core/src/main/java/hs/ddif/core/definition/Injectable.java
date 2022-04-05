package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.injection.Constructable;
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
 *
 * @param <T> the type of the instances produced
 */
public interface Injectable<T> extends Constructable<T> {

  /**
   * Returns the {@link Type} which is always fully resolved (no type variables)
   * and never {@code void}.
   *
   * @return the {@link Type}, never {@code null}
   */
  Type getType();

  /**
   * Returns the {@link Type}s of this injectable.
   * 
   * @return the {@link Type}s of this injectable, never {@code null}, empty or contains {@code null}
   */
  Set<Type> getTypes();

  /**
   * Returns an unmodifiable set of qualifier {@link Annotation}s.
   *
   * @return an unmodifiable set of qualifier {@link Annotation}s, never {@code null} and never contains {@code null}s but can be empty
   */
  Set<Annotation> getQualifiers();

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
}
