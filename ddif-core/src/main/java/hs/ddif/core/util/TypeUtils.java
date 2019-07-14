package hs.ddif.core.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.Set;

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

  /**
   * Returns the concrete class of a generic parameter of a specific implemented type.
   *
   * @param type a type to examine
   * @param implementedType the implemented type's generic parameter of interest
   * @return a concrete class if found, otherwise <code>null</code>
   */
  public static Class<?> determineTypeOfImplementedType(Type type, Class<?> implementedType) {
    if(implementedType.getTypeParameters().length == 0) {
      throw new IllegalArgumentException("implementedType must be generic");
    }
    if(implementedType.getTypeParameters().length > 1) {
      throw new IllegalArgumentException("implementedType must have exactly one type parameter");
    }

    return determineTypeOfImplementedType(type, implementedType, new HashSet<Type>());
  }

  private static Class<?> determineTypeOfImplementedType(Type type, Class<?> implementedType, Set<Type> visitedTypes) {
    if(type instanceof Class) {
      Class<?> cls = (Class<?>)type;

      for(Type iface : cls.getGenericInterfaces()) {
        Class<?> resultType = determineTypeOfImplementedType(iface, implementedType, visitedTypes);

        if(resultType != null) {
          return resultType;
        }
      }
    }
    else if(type instanceof ParameterizedType) {
      if(visitedTypes.add(type)) {
        ParameterizedType parameterizedType = (ParameterizedType)type;

        if(parameterizedType.getRawType() instanceof Class && implementedType.isAssignableFrom((Class<?>)parameterizedType.getRawType())) {
          Type finalType = parameterizedType.getActualTypeArguments()[0];

          if(finalType instanceof Class) {
            return (Class<?>)parameterizedType.getActualTypeArguments()[0];
          }
          else {
            return null;
          }
        }

        for(Type arg : parameterizedType.getActualTypeArguments()) {
          Class<?> resultType = determineTypeOfImplementedType(arg, implementedType, visitedTypes);

          if(resultType != null) {
            return resultType;
          }
        }
      }
    }

    return null;
  }
}
