package hs.ddif;

import java.lang.reflect.Type;
import java.util.Arrays;

public class NoSuchBeanException extends RuntimeException {

  public NoSuchBeanException(Type type, Object... criteria) {
    super(type + (criteria.length > 0 ? " matching criteria " + Arrays.toString(criteria) : "") + " not found");
  }

}
