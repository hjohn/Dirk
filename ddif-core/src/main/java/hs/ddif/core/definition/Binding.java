package hs.ddif.core.definition;

import hs.ddif.spi.instantiation.InjectionTarget;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;

/**
 * Bindings represent targets where values can be injected into an instance. This
 * can be a field or one of the parameters of a method or constructor.
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
 */
public interface Binding extends InjectionTarget {

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
}