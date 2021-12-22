package hs.ddif.core.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Support functions for types, classes and interfaces.
 */
public class Types {

  /**
   * Given a {@link Class}, returns a set of all classes and interfaces extended
   * or implemented.
   *
   * @param cls a {@link Class}, cannot be null
   * @return a set of all classes and interfaces extended or implemented, never null, never contains nulls and never empty
   */
  public static Set<Class<?>> getSuperTypes(Class<?> cls) {
    Deque<Class<?>> toScan = new ArrayDeque<>();
    Set<Class<?>> superTypes = new HashSet<>();

    toScan.add(cls);

    while(!toScan.isEmpty()) {
      Class<?> scanClass = toScan.remove();

      superTypes.add(scanClass);

      for(Class<?> iface : scanClass.getInterfaces()) {
        toScan.add(iface);
      }

      if(scanClass.getSuperclass() != null) {
        toScan.add(scanClass.getSuperclass());
      }
    }

    return superTypes;
  }
}
