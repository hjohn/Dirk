package hs.ddif.core.inject.bind;

import hs.ddif.core.store.Key;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/**
 * Bindings represent targets where values can be injected into an instance. This
 * can be a field or one of the parameters of a method or constructor.
 *
 * <p>Bindings differ in target, time of resolution and the number of matches allowed.
 *
 * <p>Bindings must override and implement {@link Object#equals(Object)} and {@link Object#hashCode()}.
 *
 * <h2>Target</h2>
 * The target of a binding is determined by the {@link AccessibleObject} and the given {@link Parameter}, it can be:
 * <ul>
 * <li>A constructor. The parameter indicates which constructor parameter is the target.</li>
 * <li>A method. The parameter indicates which method parameter is the target.</li>
 * <li>A field. Parameter is {@code null}.</li>
 * <li>An owner class. In order to access non-static methods and fields the owner class is required as a binding. Both the parameter and the accessible object are {@code null} in this case.</li>
 * </ul>
 * <h2>Time of Resolution</h2>
 * A binding can be resolved immediately (when an object is constructed and injected) or at some later point in time (or never) if its value
 * is accessed through a {@link javax.inject.Provider}. This is indicated by the {@code direct} flag. Bindings which are not resolved immediately
 * can be used to break circular dependencies and access dependencies in a smaller scope. They can also be acquired multiple times serving as a factory.
 * <h2>Allowed matches</h2>
 * A standard binding which supplies a dependency normally requires the dependency to be available exactly once. An optional binding allows for a
 * dependency to be unavailable or available exactly once. A collection binding has no restrictions.
 * <table border="1">
 * <caption>Binding Matches</caption>
 * <tr><th>Collection?</th><th>Optional?</th><th>Allowed matches</th></tr>
 * <tr><td>{@code false}</td><td>{@code false}</td><td>1</td>
 * <tr><td>{@code false}</td><td>{@code true}</td><td>0..1</td>
 * <tr><td>{@code true}</td><td>{@code true}/{@code false}</td><td>0 or more</td>
 * </table>
 */
public interface Binding {

  /**
   * Returns the {@link Key} of this binding.
   *
   * @return the {@link Key} of this binding, never {@code null}
   */
  Key getKey();

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
   * constructor or a method. Returns @{code null} for fields.
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
   * Returns the {@link Type} of the binding.
   *
   * @return the {@link Type} of the binding, never {@code null}
   */
  default Type getType() {
    return getKey().getType();
  }
}