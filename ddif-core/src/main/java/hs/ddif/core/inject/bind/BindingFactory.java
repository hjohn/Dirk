package hs.ddif.core.inject.bind;

import hs.ddif.core.inject.instantiation.ValueFactory;
import hs.ddif.core.store.Key;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Parameter;

/**
 * Creates {@link Binding}s.
 */
public interface BindingFactory {

  /**
   * Creates a {@link Binding}.
   *
   * @param key a {@link Key}, cannot be {@code null}
   * @param accessibleObject an {@link AccessibleObject}, can be {@code null}
   * @param parameter a {@link Parameter}, cannot be {@code null} for {@link java.lang.reflect.Executable}s and must be {@code null} otherwise
   * @param isCollection {@code true} if this binding represents a collection, otherwise {@code false}
   * @param isDirect {@code true} if this binding represents a dependency without indirection (not wrapped in a provider), otherwise {@code false}
   * @param isOptional {@code true} if this binding is optional, otherwise {@code false}
   * @param valueFactory a {@link ValueFactory}, cannot be null
   * @return a {@link Binding}, never null
   */
  Binding create(Key key, AccessibleObject accessibleObject, Parameter parameter, boolean isCollection, boolean isDirect, boolean isOptional, ValueFactory valueFactory);
}
