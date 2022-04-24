package hs.ddif.core.inject.store;

import hs.ddif.core.definition.Binding;
import hs.ddif.spi.instantiation.Key;
import hs.ddif.spi.instantiation.TypeTrait;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Keeps track of all bindings of known injectables.
 */
public interface BindingManager {

  /**
   * Adds a known {@link Binding}.
   *
   * @param binding a {@link Binding} to add, cannot be {@code null}
   */
  void addBinding(Binding binding);

  /**
   * Removes a {@link Binding}.
   *
   * @param binding a {@link Binding} to remove, cannot be {@code null}
   */
  void removeBinding(Binding binding);

  /**
   * Finds {@link Binding}s which are assignable to the given {@link Type} and qualifier
   * {@link Annotation}s.
   *
   * <p>Note: this function checks all bindings to see if they're assignable which
   * can get slow, use for debugging or exceptional cases only
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @param qualifiers a set of qualifier {@link Annotation}s, cannot be {@code null}, but can be empty
   * @return a set of {@link Binding}, never {@code null}, but can be empty
   */
  Set<Binding> findBindings(Type type, Set<Annotation> qualifiers);

  /**
   * Returns the {@link Key} required for resolving a given {@link Binding}. This
   * can differ from the binding's key depending on which extensions are configured.
   *
   * @param binding a {@link Binding}, cannot be {@code null}
   * @return a {@link Key}, never {@code null}
   */
  Key getSearchKey(Binding binding);

  /**
   * Get the {@link TypeTrait}s belonging to the given {@link Binding}.
   *
   * @param binding a {@link Binding}, cannot be {@code null}
   * @return a set of {@link TypeTrait}, never {@code null} or contains {@code null} but can be empty
   */
  Set<TypeTrait> getTypeTraits(Binding binding);
}
