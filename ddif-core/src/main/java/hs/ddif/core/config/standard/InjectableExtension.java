package hs.ddif.core.config.standard;

import hs.ddif.core.definition.Injectable;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Allows simple extensions to a {@link DefaultDiscovererFactory}.
 */
public interface InjectableExtension {

  /**
   * Returns zero or more {@link Injectable}s which are derived from the
   * given {@link Type}. For example, the given type could have special
   * annotations which supply further injectables. These in turn could require
   * dependencies (as parameters) that may need to be auto discovered first.
   *
   * @param type a {@link Type} use as base for derivation, never {@code null}
   * @return a list of {@link Injectable}, never {@code null} and never contains {@code null}s
   */
  List<Injectable> getDerived(Type type);
}