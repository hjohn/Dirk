package hs.ddif.core;

public class DuplicateBeanException extends RuntimeException {

  public DuplicateBeanException(Class<?> type, Injectable injectable) {
    super(type + " already registered for: " + injectable);
  }

}
