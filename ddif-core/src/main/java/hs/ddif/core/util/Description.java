package hs.ddif.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Utility class to describe classes as text.
 */
public class Description {

  /**
   * Describes a {@link Member}.
   *
   * @param member a {@link Member}, cannot be {@code null}
   * @return a description, never {@code null}
   */
  public static String of(Member member) {
    return (member instanceof Method ? "Method" : member instanceof Field ? "Field" : "Constructor") + " [" + member + "]";
  }
}
