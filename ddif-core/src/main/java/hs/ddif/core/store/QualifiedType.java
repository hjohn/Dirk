package hs.ddif.core.store;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * A combination of {@link Type} and a set of {@link javax.inject.Qualifier} {@link Annotation}s.
 */
public interface QualifiedType {

  /**
   * Returns the {@link Type}.
   *
   * @return the {@link Type}, never {@code null}
   */
  Type getType();

  /**
   * Returns an unmodifiable set of qualifier {@link Annotation}s.
   *
   * @return an unmodifiable set of qualifier {@link Annotation}s, never {@code null} and never contains {@code null}s but can be empty
   */
  Set<Annotation> getQualifiers();
}
