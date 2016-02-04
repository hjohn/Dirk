package hs.ddif.core;

import java.lang.reflect.Type;

/**
 * Thrown when an attempt is made to register or remove an injectable that would cause a
 * singular dependency required by another injectable to be violated (either by providing
 * a second alternative or by not providing one anymore).<p>
 *
 * For example, when there exists an injectable that requires a Database implementation and
 * the only Database implementation is removed, the requirements of this injectable can no
 * longer be met.
 */
public class ViolatesSingularDependencyException extends DependencyException {

  public ViolatesSingularDependencyException(Type type, Key key, boolean isRegistration) {
    // TODO would be great if the message could show which injectables use this singular dependency.
    super(key + " " + (isRegistration ? "would be provided again" : "is only provided") + " by: " + type);
  }
}
