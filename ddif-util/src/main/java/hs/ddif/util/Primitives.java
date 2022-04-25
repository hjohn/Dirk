package hs.ddif.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods dealing with primitives.
 */
public class Primitives {
  private static final Map<Class<?>, Class<?>> WRAPPER_CLASS_BY_PRIMITIVE_CLASS = new HashMap<>();

  static {
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(boolean.class, Boolean.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(byte.class, Byte.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(short.class, Short.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(char.class, Character.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(int.class, Integer.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(long.class, Long.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(float.class, Float.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(double.class, Double.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(void.class, Void.class);
  }

  /**
   * Converts a {@link Type} if it is primitive to its box, otherwise returns the
   * original type.
   *
   * @param type a type to convert, cannot be {@code null}
   * @return a boxed {@link Type} if it was a primitive type, otherwise returns the original type, never {@code null}
   */
  public static Type toBoxed(Type type) {
    return type instanceof Class && ((Class<?>)type).isPrimitive() ? WRAPPER_CLASS_BY_PRIMITIVE_CLASS.get(type) : type;
  }
}
