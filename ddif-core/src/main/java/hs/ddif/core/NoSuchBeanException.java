package hs.ddif.core;

import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Thrown when no matching bean could be found.
 */
public class NoSuchBeanException extends RuntimeException {

  public NoSuchBeanException(Type type, Throwable cause, Object... criteria) {
    super(type + (criteria.length > 0 ? " matching criteria " + Arrays.toString(criteria) : "") + " not found", cause);
  }

  public NoSuchBeanException(Type type, Object... criteria) {
    super(type + (criteria.length > 0 ? " matching criteria " + Arrays.toString(criteria) : "") + " not found");
  }

}
