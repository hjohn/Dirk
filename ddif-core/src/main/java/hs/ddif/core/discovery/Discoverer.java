package hs.ddif.core.discovery;

import hs.ddif.api.definition.DefinitionException;
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
   * @throws DefinitionException when a definition problem was encountered
   */
  Set<Injectable<?>> discover() throws DefinitionException;

  /**
   * Returns a list of problems encountered during discovery. Note that if auto discovery is off
   * this list is guaranteed to be empty. If not empty, callers would do well to include the
   * list of problems in their final exception message.
   *
   * @return a list of problems, never {@code null} or contains {@code null} but can be empty
   */
  List<String> getProblems();
}
