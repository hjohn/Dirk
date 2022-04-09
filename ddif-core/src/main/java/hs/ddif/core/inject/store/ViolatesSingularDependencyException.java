package hs.ddif.core.inject.store;

import hs.ddif.api.instantiation.domain.Key;

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
public class ViolatesSingularDependencyException extends InjectorStoreConsistencyException {

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @param key a {@link Key}, cannot be {@code null}
   * @param isRegistration {@code true} if this occurred during registration, {@code false} if it occurred during removal
   */
  public ViolatesSingularDependencyException(Type type, Key key, boolean isRegistration) {
    // TODO would be great if the message could show which injectables use this singular dependency.
    super("[" + key + "] " + (isRegistration ? "would be provided again" : "is only provided") + " by: " + type);
  }
}
