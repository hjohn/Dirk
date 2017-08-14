package hs.ddif.core.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class TypeUtils {
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
}
