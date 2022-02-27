package hs.ddif.core.definition.bind;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Thrown when a {@link Class} or instance which is being registered with an Injector
 * is not setup correctly for injection.  This can occur for example when multiple constructors
 * are annotated with an inject annotation or final fields are annotated as such.
 */
public class BindingException extends Exception {

  BindingException(Type ancestor, Member member, String message, Throwable cause) {
    super(describe(member) + " of [" + ancestor + "] " + message + (cause == null ? "" : ": " + cause.getMessage()), cause);
  }

  BindingException(Type ancestor, Member member, String message) {
    this(ancestor, member, message, null);
  }

  BindingException(Class<?> cls, String message) {
    super("[" + cls + "] " + message);
  }

  private static String describe(Member member) {
    return (member instanceof Method ? "Method" : member instanceof Field ? "Field" : "Constructor") + " [" + member + "]";
  }
}
