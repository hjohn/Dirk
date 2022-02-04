package hs.ddif.core.inject.store;

import hs.ddif.core.inject.injectable.Injectable;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when not all dependencies of a class can be resolved.  This occurs when
 * a class requires a specific dependency but no such dependency is available or
 * more than one matching dependency is available.
 */
public class CyclicDependencyException extends InjectorStoreConsistencyException {
  private final List<? extends Injectable> cycle;

  /**
   * Constructs a new instance.
   *
   * @param cycle a list of {@link Injectable} which make up the cycle, cannot be {@code null} or empty
   */
  public CyclicDependencyException(List<? extends Injectable> cycle) {
    super("Cyclic dependency detected in chain:\n" + format(cycle));

    this.cycle = Collections.unmodifiableList(cycle);
  }

  /**
   * Returns a list of {@link Injectable} which make up the cycle.
   *
   * @return a list of {@link Injectable} which make up the cycle, never {@code null} or empty
   */
  public List<? extends Injectable> getCycle() {
    return cycle;
  }

  private static String format(List<? extends Injectable> cycle) {
    StringBuilder b = new StringBuilder();

    b.append("     -----\n");
    b.append("    |     |\n");

    for(Injectable i : cycle) {
      b.append("    |     V\n");
      b.append("    | " + i + "\n");
      b.append("    |     |\n");
    }

    b.append("     -----\n");

    return b.toString();
  }
}
