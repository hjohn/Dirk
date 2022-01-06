package hs.ddif.core.util;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;

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

  /**
   * Creates a new {@link WildcardType} with the given upper bounds. Note that
   * the Java specification does not allow wildcard types with multiple upper
   * bounds to be created in source code, however you can create them
   * programmatically in order to for example filter by types that must implement
   * two interfaces.
   *
   * @param upperBounds an array of {@link Type}s, cannot be {@code null} or contain {@code null}s
   * @return a {@link WildcardType}, never null
   */
  public static WildcardType wildcardExtends(Type... upperBounds) {
    return TypeUtils.wildcardType().withUpperBounds(upperBounds).build();
  }

  /**
   * Gets the raw type of a Java {@link Type}. This works for classes and parameterized
   * types. Returns {@code null} if no raw type can be derived.
   *
   * @param type a {@link Type}, cannot be null
   * @return a {@link Class} representing the raw type of the given type, can be null
   */
  public static Class<?> raw(Type type) {
    return TypeUtils.getRawType(type, null);
  }
}
