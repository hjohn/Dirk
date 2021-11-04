package hs.ddif.core.store;

/**
 * A criteria filter for injectables.
 */
public interface Matcher {

  /**
   * Returns {@code true} if the given {@link Class} should match the criteria,
   * otherwise {@code false}.
   *
   * @param cls a {@link Class} that should be matched, never null
   * @return {@code true} if the given {@link Class} should match the criteria, otherwise {@code false}
   */
  boolean matches(Class<?> cls);
}
