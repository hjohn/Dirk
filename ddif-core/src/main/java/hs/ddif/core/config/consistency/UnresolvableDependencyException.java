package hs.ddif.core.config.consistency;

import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.injectable.Injectable;

import java.util.Set;

/**
 * Thrown when not all dependencies of an injectable can be resolved.  This occurs when
 * an injectable requires a specific dependency but no such dependency is available or
 * more than one matching dependency is available.
 */
public class UnresolvableDependencyException extends InjectorStoreConsistencyException {
  private final Binding binding;
  private final Set<? extends Injectable> candidates;

  /**
   * Constructs a new instance.
   *
   * @param binding a {@link Binding}, cannot be {@code null}
   * @param candidates a set of {@link Injectable}s that were candidates, cannot be {@code null} or empty
   */
  public UnresolvableDependencyException(Binding binding, Set<? extends Injectable> candidates) {
    super(formatMessage(binding, candidates));

    this.binding = binding;
    this.candidates = candidates;
  }

  private static String formatMessage(Binding binding, Set<? extends Injectable> candidates) {
    return (candidates.isEmpty() ? "Missing" : "Multiple candidates for")
      + " dependency of type " + binding.getKey()
      + " required for " + binding
      + (candidates.isEmpty() ? "" : ": " + candidates);
  }

  /**
   * Returns the {@link Binding} involved.
   *
   * @return the {@link Binding} involved, never {@code null}
   */
  public Binding getBinding() {
    return binding;
  }

  /**
   * Returns the {@link Injectable}s that were candidates.
   *
   * @return the {@link Injectable}s that were candidates, never {@code null} or empty
   */
  public Set<? extends Injectable> getCandidates() {
    return candidates;
  }
}
