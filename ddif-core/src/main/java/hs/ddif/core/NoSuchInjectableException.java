package hs.ddif.core;

public class NoSuchInjectableException extends RuntimeException {

  public NoSuchInjectableException(Injectable injectable) {
    super(injectable + " not found");
  }

}
