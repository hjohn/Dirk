package hs.ddif.api.definition;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Thrown during registration of types or instances when the type is incorrectly
 * annotated, cannot be constructed or cannot be injected. Retrying the
 * registration will not solve problems of this type and generally points to
 * annotation, visibility or type definition problems. For example:
 *
 * <ul>
 * <li>No suitable or ambiguous constructor, for example when no constructors
 * or multiple constructors are inject annotated</li>
 * <li>Injection annotation on a final field or setter without parameters</li>
 * <li>Conflicting scope annotations</li>
 * <li>Registering an abstract type or a generic type with unresolvable type variables</li>
 * <li>Using non-qualifier annotations when registering an instance</li>
 * </ul>
 *
 * <p>Definition problems can generally be solved by changing the types
 * involved, like adding or removing annotations, changing visibility of
 * fields or methods, etc.; reconfiguring the injector with different
 * annotations or extensions may also solve definition problems.
 *
 * <p>Note: this exception specifically does not signal context sensitive
 * problems that could be solved by registering or unregistering additional
 * types; these are considered resolution problems, not definition problems.
 */
public class DefinitionException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param element an {@link AnnotatedElement} involved in the definition problem, cannot be {@code null}
   * @param message a message describing the problem, cannot be {@code null}
   * @param cause an underlying cause of the problem, can be {@code null}
   */
  public DefinitionException(AnnotatedElement element, String message, Throwable cause) {
    super(describe(Objects.requireNonNull(element)) + " " + Objects.requireNonNull(message), cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param element an {@link AnnotatedElement} involved in the definition problem, cannot be {@code null}
   * @param message a message describing the problem, cannot be {@code null}
   */
  public DefinitionException(AnnotatedElement element, String message) {
    this(element, message, null);
  }

  private static String describe(AnnotatedElement element) {
    return (element instanceof Method ? "Method "
      : element instanceof Field ? "Field "
      : element instanceof Class ? ""
      : "Constructor ") + "[" + element + "]";
  }
}
