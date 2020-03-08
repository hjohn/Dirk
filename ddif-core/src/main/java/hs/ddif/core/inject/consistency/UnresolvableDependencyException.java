package hs.ddif.core.inject.consistency;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.store.Injectable;

import java.util.Set;

/**
 * Thrown when not all dependencies of an injectable can be resolved.  This occurs when
 * an injectable requires a specific dependency but no such dependency is available or
 * more than one matching dependency is available.
 */
public class UnresolvableDependencyException extends InjectorStoreConsistencyException {
  private final Binding binding;
  private final Set<? extends Injectable> candidates;

  public UnresolvableDependencyException(Binding binding, Set<? extends Injectable> candidates) {
    super(formatMessage(binding, candidates));

    this.binding = binding;
    this.candidates = candidates;
  }

  private static String formatMessage(Binding binding, Set<? extends Injectable> candidates) {
    return (candidates.isEmpty() ? "Missing" : "Multiple candidates for")
      + " dependency of type " + binding.getRequiredKey()
      + " required for " + binding
      + (candidates.isEmpty() ? "" : ": " + candidates);
  }

  public Binding getBinding() {
    return binding;
  }

  public Set<? extends Injectable> getCandidates() {
    return candidates;
  }
}
