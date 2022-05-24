package org.int4.dirk.util;

import java.util.Objects;

/**
 * Support functions for {@link Class}es.
 */
public class Classes {

  /**
   * Checks if the given class is available on the class path.
   *
   * @param className a class name, cannot be {@code null}
   * @return {@code true} if the class could be found and loaded, otherwise {@code false}
   */
  public static boolean isAvailable(String className) {
    try {
      Class.forName(Objects.requireNonNull(className, "className cannot be null"));

      return true;
    }
    catch(Throwable e) {  // Not available
      return false;
    }
  }
}
