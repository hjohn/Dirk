package hs.ddif;

import java.util.Arrays;

public class NoSuchBeanException extends RuntimeException {

  public NoSuchBeanException(Class<?> concreteClass, Object... criteria) {
    super(concreteClass + (criteria.length > 0 ? " matching criteria " + Arrays.toString(criteria) : "") + " not found");
  }

}
