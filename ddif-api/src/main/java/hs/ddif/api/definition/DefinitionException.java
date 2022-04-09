package hs.ddif.api.definition;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Thrown during registration of types or instances when these types are
 * unsuitable for injection. This signals problems that can occur during
 * development of an application like attempting to register types for which
 * the framework has no means of creation or for which creation is ambiguous.
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

  /**
   * Constructs a new instance.
   *
   * @param throwable a {@link Throwable} that is to be converted to a definition exception, cannot be {@code null}
   */
  public DefinitionException(Throwable throwable) {
    super(throwable.getMessage(), throwable);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message describing the problem, cannot be {@code null}
   * @param cause an underlying cause of the problem, can be {@code null}
   */
  public DefinitionException(String message, Throwable cause) {
    super(Objects.requireNonNull(message), cause);
  }

  private static String describe(AnnotatedElement element) {
    return (element instanceof Method ? "Method "
      : element instanceof Field ? "Field "
      : element instanceof Class ? ""
      : "Constructor ") + "[" + element + "]";
  }
}
