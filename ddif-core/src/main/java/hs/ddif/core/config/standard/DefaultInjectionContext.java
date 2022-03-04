package hs.ddif.core.config.standard;

import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.instantiation.injection.InjectionContext;

import java.util.List;

/**
 * Context used to create and destroy instances with injection information.
 */
public class DefaultInjectionContext implements InjectionContext {
  private final List<Injection> injections;

  /**
   * Constructs a new instance.
   *
   * @param injections a list of {@link Injection}s, cannot be {@code null} or contain {@code null} but can be empty
   */
  public DefaultInjectionContext(List<Injection> injections) {
    this.injections = injections;
  }

  @Override
  public List<Injection> getInjections() {
    return injections;
  }
}
