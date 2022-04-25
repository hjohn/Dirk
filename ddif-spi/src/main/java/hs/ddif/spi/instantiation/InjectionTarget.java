package hs.ddif.spi.instantiation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Represents a target for injection, a field or a parameter of a method or constructor.
 */
public interface InjectionTarget {

  /**
   * Returns the {@link Key} of the injection target, defining the required {@link Type}
   * and qualifier {@link Annotation}s.
   *
   * @return the {@link Key} of the injection target, never {@code null}
   */
  Key getKey();

  /**
   * Returns whether this target accepts {@code null} as an injection value. Normally
   * {@code null} is rejected with {@link hs.ddif.api.instantiation.UnsatisfiedResolutionException},
   * but optional targets treat {@code null} differently. If the target is a method
   * or constructor parameters, {@code null} is simply provided, leaving it up to
   * the receiver to deal with the {@code null}. For fields, the injection is skipped
   * leaving its default value intact.
   *
   * @return {@code true} if the target is optional, otherwise {@code false}
   */
  boolean isOptional();

}
