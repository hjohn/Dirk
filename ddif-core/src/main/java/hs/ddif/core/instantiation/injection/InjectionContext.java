package hs.ddif.core.instantiation.injection;

import java.util.List;

/**
 * Context used to create and destroy instances with injection information.
 */
public interface InjectionContext {

  /**
   * Returns a list of {@link Injection}s.
   *
   * @return a list of {@link Injection}s, never {@code null} or contains {@code null}s but can be empty
   */
  List<Injection> getInjections();
}
