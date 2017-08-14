package hs.ddif.core.store;

public class NoSuchInjectableException extends RuntimeException {

  public NoSuchInjectableException(Injectable injectable) {
    super(injectable + " not found");
  }

}
