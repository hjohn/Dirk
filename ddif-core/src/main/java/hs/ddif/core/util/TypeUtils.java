package hs.ddif.core.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class TypeUtils {

  /**
   * Determines the {@link Class} for a {@link Type}.
   *
   * @param type a {@link Type}
   * @return a {@link Class}, never null
   */
  public static final Class<?> determineClassFromType(Type type) {
    if(type instanceof Class) {
      return (Class<?>)type;
    }
    else if(type instanceof ParameterizedType) {
      return (Class<?>)((ParameterizedType)type).getRawType();
    }
    else if(type instanceof TypeVariable) {
      return (Class<?>)((TypeVariable<?>)type).getBounds()[0];
    }

    throw new IllegalArgumentException("Unsupported type: " + type);
  }

  /**
   * Returns the type of the first generic parameter of a {@link Type}.
   *
   * @param type a {@link Type}
   * @return the {@link Type} of the first generic parameter of the given type, never null
   */
  public static Type getGenericType(Type type) {
    if(type instanceof ParameterizedType) {
      ParameterizedType genericType = (ParameterizedType)type;
      return genericType.getActualTypeArguments()[0];
    }
    else if(type instanceof Class) {
      Class<?> cls = (Class<?>)type;
      return cls.getTypeParameters()[0];
    }

    throw new IllegalStateException("Could not get generic type for: " + type);
  }
}
