package org.int4.dirk.core.definition;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Supplier;

import org.int4.dirk.core.util.Key;
import org.int4.dirk.spi.instantiation.TypeTrait;

/**
 * Bindings represent targets where values can be injected into an instance. This
 * can be a field or one of the parameters of a method or constructor.
 *
 * <h2>Target</h2>
 * The target of a binding is determined by the {@link AccessibleObject} and the given {@link Parameter}, it can be:
 * <ul>
 * <li>A constructor. The parameter indicates which constructor parameter is the target.</li>
 * <li>A method. The parameter indicates which method parameter is the target.</li>
 * <li>A field. Parameter is {@code null}.</li>
 * <li>An owner class. In order to access non-static methods and fields the owner class is required as a binding. Both the parameter and the accessible object are {@code null} in this case.</li>
 * </ul>
 */
public interface Binding {

  /**
   * Returns the {@link Type} of the injection target.
   *
   * @return the {@link Type} of the injection target, never {@code null}
   */
  Type getType();

  /**
   * Returns the qualifiers on the injection target.
   *
   * @return a set of qualifier annotations, never {@code null} and never contains {@code null}
   */
  default Set<Annotation> getQualifiers() {
    return getElementKey().getQualifiers();
  }

  /**
   * Returns whether this target accepts {@code null} as an injection value. Normally
   * {@code null} is rejected with {@link org.int4.dirk.api.instantiation.UnsatisfiedResolutionException},
   * but optional targets treat {@code null} differently. If the target is a method
   * or constructor parameters, {@code null} is simply provided, leaving it up to
   * the receiver to deal with the {@code null}. For fields, the injection is skipped
   * leaving its default value intact.
   *
   * @return {@code true} if the target is optional, otherwise {@code false}
   */
  boolean isOptional();

  /**
   * Returns the {@link Key} of which individual elements of the injection target consist.
   * For simple types, this will be the same as the injection target's type. For types
   * which are provided by an injection target extension, this will be base type that
   * is looked up for injection.
   *
   * @return a {@link Key}, never {@code null}
   */
  Key getElementKey();

  /**
   * Returns the target {@link AccessibleObject} for the binding. This is {@code null}
   * when the binding refers to the declaring class which is required to access a
   * non-static field or method.
   *
   * @return the target @link AccessibleObject} for the binding, can be {@code null}
   */
  AccessibleObject getAccessibleObject();

  /**
   * Returns the {@link Parameter} when the {@link AccessibleObject} is a
   * constructor or a method. Returns {@code null} for fields.
   *
   * @return a {@link Parameter}, can be {@code null}
   */
  Parameter getParameter();

  /**
   * Returns the associated {@link AnnotatedElement} for this binding. This is
   * {@code null} for owner bindings.
   *
   * @return the associated {@link AnnotatedElement} for this binding, can be {@code null}
   */
  default AnnotatedElement getAnnotatedElement() {
    Parameter parameter = getParameter();

    return parameter == null ? getAccessibleObject() : parameter;
  }

  /**
   * Returns the {@link TypeTrait}s of this binding.
   *
   * @return a set of {@link TypeTrait}, never {@code null}
   */
  Set<TypeTrait> getTypeTraits();

  /**
   * Associate extra data with this binding. Currently only used for the
   * instantiation contexts. Using binding in a WeakHashMap as key will too easily
   * lead to bindings not being GC'd as these refer to type, which in turn is used
   * in another WeakHashMap (see DefaultDiscoveryFactory). Likely to be replaced
   * with a better solution later.
   *
   * @param <T> type of data to associate
   * @param key a string key, cannot be {@code null}
   * @param valueSupplier a {@link Supplier} for the value if the key does not exist yet, cannot be {@code null}
   * @return the current associated value, can be {@code null} if value supplied was {@code null}
   */
  <T> T associateIfAbsent(String key, Supplier<T> valueSupplier);
}