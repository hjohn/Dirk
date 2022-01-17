package hs.ddif.core.inject.injectable;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

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
   * @param cls a {@link Class} involved in the definition problem, cannot be {@code null}
   * @param message a message describing the problem, cannot be {@code null}
   */
  public DefinitionException(Class<?> cls, String message) {
    super("[" + cls + "] " + message, null);
  }

  /**
   * Constructs a new instance.
   *
   * @param member a {@link Member} involved in the definition problem, cannot be {@code null}
   * @param message a message describing the problem, cannot be {@code null}
   */
  public DefinitionException(Member member, String message) {
    super(describe(member) + " " + message, null);
  }

  /**
   * Constructs a new instance.
   *
   * @param throwable a {@link Throwable} that is to be converted to a definition exception, cannot be {@code null}
   */
  public DefinitionException(Throwable throwable) {
    super(throwable.getMessage(), throwable.getCause());
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message describing the problem, cannot be {@code null}
   * @param cause an underlying cause of the problem, can be {@code null}
   */
  public DefinitionException(String message, Throwable cause) {
    super(message, cause);
  }

  private static String describe(Member member) {
    return (member instanceof Method ? "Method" : member instanceof Field ? "Field" : "Constructor") + " [" + member + "]";
  }
}