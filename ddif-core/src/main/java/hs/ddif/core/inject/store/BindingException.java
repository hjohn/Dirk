package hs.ddif.core.inject.store;

import javax.inject.Inject;

/**
 * Thrown when a {@link Class} or instance which is being registered with an Injector
 * is not setup correctly for injection.  This can occur for example when multiple constructors
 * are annotated with {@link Inject} or final fields are annotated as such.
 */
public class BindingException extends RuntimeException {

  public BindingException(String message) {
    super(message);
  }
}
