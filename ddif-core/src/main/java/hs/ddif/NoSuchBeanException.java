package hs.ddif;

public class NoSuchBeanException extends RuntimeException {

  public NoSuchBeanException(Key key) {
    super(key + " not found");
  }

  public NoSuchBeanException(Class<?> concreteClass) {
    super(concreteClass + " not found");
  }

}
