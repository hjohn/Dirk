package hs.ddif.api;

import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.api.definition.DefinitionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Provides methods to manage injection candidates, like registering and removing.<p>
 *
 * Depending on the underlying store used certain invariants will be automatically
 * enforced. For example, removing a candidate which is currently required as
 * a dependency could result in an exception if the store is enforcing that all
 * known candidates must be resolvable.<p>
 *
 * Depending on which extensions are active, more candidates will be derived from
 * any given candidate when certain annotations or interfaces are implemented. For
 * example, registering a candidate which has produces annotated members will also
 * register the types produced. The same could apply to candidates
 * implementing a provider interface.
 */
public interface CandidateRegistry {

  /**
   * Returns <code>true</code> when the given type with the given qualifiers is present,
   * otherwise <code>false</code>.
   *
   * @param type a type to check for, cannot be {@code null}
   * @param qualifiers optional list of qualifier annotations, either {@link Annotation} or {@link Class}&lt;? extends Annotation&gt;
   * @return <code>true</code> when the given type with the given criteria is present, otherwise <code>false</code>
   */
  boolean contains(Type type, Object... qualifiers);

  /**
   * Registers a {@link Type}, and all its derived candidates if any, if all
   * its dependencies can be resolved and it would not cause existing registered
   * types to have ambiguous dependencies as a result of registering the given type.<p>
   *
   * If there are unresolvable dependencies, or registering this type
   * would result in ambiguous dependencies for previously registered
   * types, then this method will throw an exception.
   *
   * @param type the type to register, cannot be {@code null}
   * @throws AutoDiscoveryException when auto discovery fails to find all required types
   * @throws DefinitionException when a definition problem was encountered
   */
  void register(Type type) throws AutoDiscoveryException, DefinitionException;

  /**
   * Registers the given {@link Type}s, and all their derived candidates if any, if all
   * their dependencies can be resolved and it would not cause existing registered
   * types to have ambiguous dependencies as a result of registering the given types.<p>
   *
   * If there are unresolvable dependencies, or registering these types
   * would result in ambiguous dependencies for previously registered
   * types, then this method will throw an exception.
   *
   * @param types a list of types to register, cannot be {@code null} or contain {@code null}s
   * @throws AutoDiscoveryException when auto discovery fails to find all required types
   * @throws DefinitionException when a definition problem was encountered
   */
  void register(List<Type> types) throws AutoDiscoveryException, DefinitionException;

  /**
   * Registers an instance, and all its derived candidates if any, as a
   * singleton if it would not cause existing registered
   * types to have ambiguous dependencies as a result.<p>
   *
   * If registering this instance would result in ambiguous dependencies for
   * previously registered classes, then this method will throw an exception.
   *
   * @param instance the instance to register with the Injector
   * @param qualifiers optional list of qualifiers for this instance
   * @throws DefinitionException when a definition problem was encountered
   */
  void registerInstance(Object instance, Annotation... qualifiers) throws DefinitionException;

  /**
   * Removes the given {@link Type}, and all its derived candidates if any, if
   * doing so would not result maintains all invariants for the remaining
   * registered types.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.
   *
   * @param type the type to remove, cannot be {@code null}
   * @throws AutoDiscoveryException when auto discovery fails to find all required types
   * @throws DefinitionException when a definition problem was encountered
   */
  void remove(Type type) throws AutoDiscoveryException, DefinitionException;

  /**
   * Removes the given {@link Type}s, all their derived candidates if any, if
   * doing so would not result maintains all invariants for the remaining
   * registered types.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.
   *
   * @param types a list of types to remove, cannot be {@code null} or contain {@code null}s
   * @throws AutoDiscoveryException when auto discovery fails to find all required types
   * @throws DefinitionException when a definition problem was encountered
   */
  void remove(List<Type> types) throws AutoDiscoveryException, DefinitionException;

  /**
   * Removes an instance, and all its derived candidates if any, if doing so
   * would not result in broken dependencies in the remaining registered types.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.
   *
   * @param instance the instance to remove, cannot be {@code null}
   * @throws DefinitionException when a definition problem was encountered
   */
  void removeInstance(Object instance) throws DefinitionException;
}
