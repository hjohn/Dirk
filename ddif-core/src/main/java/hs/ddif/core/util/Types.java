package hs.ddif.core.util;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
   * @param cls a {@link Class}, cannot be {@code null}
   * @return a set of all classes and interfaces extended or implemented, never {@code null}, never contains {@code null}s and never empty
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
   * @return a {@link WildcardType}, never {@code null}
   */
  public static WildcardType wildcardExtends(Type... upperBounds) {
    return TypeUtils.wildcardType().withUpperBounds(upperBounds).build();
  }

  /**
   * Gets the raw type of a Java {@link Type}. This works for classes and parameterized
   * types. Returns {@code null} if no raw type can be derived.
   *
   * @param <T> the expected type
   * @param type a {@link Type}, cannot be {@code null}
   * @return a {@link Class} representing the raw type of the given type, can be {@code null}
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> raw(Type type) {
    return (Class<T>)TypeUtils.getRawType(type, null);
  }

  /**
   * Gets the {@link Type} of a generic parameter, identified by given by the {@link TypeVariable},
   * of the given {@link Class} when resolved against the given {@link Type}.
   *
   * @param type a {@link Type} from which to determine the parameters of the given {@link Class}, cannot be {@code null}
   * @param cls a {@link Class} to determine a type parameter for, cannot be {@code null}
   * @param typeVariable a {@link TypeVariable} of the given {@link Class} to extract, cannot be {@code null}
   * @return a {@link Type}, can be {@code null} if the {@link TypeVariable} was not associated with the given {@link Class}
   */
  public static Type getTypeParameter(Type type, Class<?> cls, TypeVariable<?> typeVariable) {
    return TypeUtils.getTypeArguments(type, cls).get(typeVariable);
  }
}
