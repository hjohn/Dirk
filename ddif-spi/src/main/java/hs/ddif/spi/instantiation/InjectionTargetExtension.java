package hs.ddif.spi.instantiation;

import hs.ddif.api.instantiation.AmbiguousResolutionException;
import hs.ddif.api.instantiation.CreationException;
import hs.ddif.api.instantiation.UnsatisfiedResolutionException;
import hs.ddif.api.scope.ScopeNotActiveException;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * An interface to allow for custom handling of injection targets of type
 * {@code T}. The target class must be an interface with at least one type variable.
 *
 * <p>Whenever an injection target of type {@code T} needs injection this
 * extension will be called to provide the actual value.
 *
 * @param <T> the type handled
 * @param <E> the element type required
 */
public interface InjectionTargetExtension<T, E> {

  /**
   * Returns the target class extended by this extension.
   *
   * @return a {@link Class}, never {@code null}
   */
  Class<?> getTargetClass();

  /**
   * Returns the element type of the given type by unwrapping the given type. Returning
   * {@code null} or the same type is not allowed.
   *
   * @param type a {@link Type} to unwrap, cannot be {@code null}
   * @return the element type of the given type by unwrapping the given type, never {@code null}
   */
  Type getElementType(Type type);

  /**
   * Returns the {@link TypeTrait}s of this extension.
   *
   * @return a set of {@link TypeTrait}, never {@code null} or contains {@code null}, but can be empty
   */
  Set<TypeTrait> getTypeTraits();

  /**
   * Creates an instance of type {@code T} using the given {@link InstantiationContext}.
   * Returning {@code null} is allowed to indicate the absence of a value. Depending on the
   * destination where the value is used this can mean {@code null} is injected (methods and
   * constructors), that the value is not injected (fields) or that an {@link UnsatisfiedResolutionException}
   * is thrown (direct instance resolver call).
   *
   * @param context an {@link InstantiationContext}, cannot be {@code null}
   * @return an instance of type {@code T}, can be {@code null}
   * @throws CreationException when the instance could not be created
   * @throws AmbiguousResolutionException when multiple instances matched but at most one was required
   * @throws UnsatisfiedResolutionException when no instance matched but at least one was required
   * @throws ScopeNotActiveException when the scope for the produced type is not active
   */
  T getInstance(InstantiationContext<E> context) throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException, ScopeNotActiveException;
}
