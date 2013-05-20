package hs.ddif;

/**
 * Thrown when an attempt is made to register or remove a Bean that would cause a singular
 * dependency required by another Bean to be violated (either by providing a second
 * alternative or by not providing one anymore).<p>
 *
 * For example, when there exists a Bean that requires a Database implementation and the
 * only Database implementation is removed, the requirements of this Bean can no longer be
 * met.
 */
public class ViolatesSingularDependencyException extends DependencyException {

  public ViolatesSingularDependencyException(Class<?> beanClass, Key key, boolean isRegistration) {
    // TODO would be great if the message could show which beans use this singular dependency.
    super(key + " " + (isRegistration ? "would be provided again" : "is only provided") + " by: " + beanClass);
  }
}
