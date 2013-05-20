package hs.ddif;

public class DuplicateBeanException extends RuntimeException {

  public DuplicateBeanException(Class<?> concreteClass) {
    super(concreteClass + " already registered");
  }

}
