package hs.ddif.core.instantiation.domain;

import hs.ddif.core.api.InstanceCreationException;
import hs.ddif.core.store.Key;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.annotation.PostConstruct;

/**
 * Thrown when during (post-)construction of a dependency a problem
 * occurs.  Constructors that throw an exception, {@link PostConstruct}s
 * which trigger further dependency construction which eventually need
 * the current object under construction (causing a loop) and so on.
 */
public class InstanceCreationFailure extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param member the constructor, method or field involved, cannot be {@code null}
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  public InstanceCreationFailure(Member member, String message, Throwable cause) {
    super(describe(member) + " " + message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  public InstanceCreationFailure(Key key, String message, Throwable cause) {
    super(key + " " + message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param type type involved, cannot be {@code null}
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  public InstanceCreationFailure(Type type, String message, Throwable cause) {
    super("[" + type + "] " + message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param type type involved, cannot be {@code null}
   * @param message a message
   */
  public InstanceCreationFailure(Type type, String message) {
    super("[" + type + "] " + message);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  protected InstanceCreationFailure(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public InstanceCreationException toRuntimeException() {
    InstanceCreationException exception = new InstanceCreationException(getMessage(), getCause());

    for(Throwable t : getSuppressed()) {
      exception.addSuppressed(t);
    }

    return exception;
  }

  private static String describe(Member member) {
    return (member instanceof Method ? "Method" : member instanceof Field ? "Field" : "Constructor") + " [" + member + "]";
  }
}
