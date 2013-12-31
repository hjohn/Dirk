package hs.ddif;

/**
 * Thrown when an Injectable is not setup correctly.  This can occur for example
 * when multiple constructors are annotated with @Inject or final fields are annotated
 * as such.
 */
public class BindingException extends DependencyException {

  public BindingException(String message) {
    super(message);
  }
}
