package hs.ddif.core.config.discovery;

import hs.ddif.core.definition.Injectable;

import java.util.List;
import java.util.Set;

/**
 * Potentially discovers additional {@link Injectable}s. Any problems that may be
 * of interest after discovery are reported via {@link #getProblems()}.
 */
public interface Discoverer {

  /**
   * Discovers additional {@link Injectable}s.
   *
   * @return a set of {@link Injectable} that were discovered, never {@code null} or contains {@code null} but can be empty
   */
  Set<Injectable> discover();

  /**
   * Returns a list of problems encountered during discovery.
   *
   * @return a list of problems, never {@code null} or contains {@code null} but can be empty
   */
  List<String> getProblems();
}
