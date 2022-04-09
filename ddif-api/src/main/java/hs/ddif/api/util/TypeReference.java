package hs.ddif.api.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Helper class that can be subclassed to provide full generic type information.
 *
 * @param <T> a generic type that must be provided
 */
public abstract class TypeReference<T> {
  private final Type type;

  /**
   * Constructs a new instance.
   */
  protected TypeReference() {
    Type superclass = getClass().getGenericSuperclass();

    if(superclass instanceof Class) {
      throw new IllegalStateException("Missing type parameter");
    }

    type = ((ParameterizedType)superclass).getActualTypeArguments()[0];
  }

  /**
   * Gets the referenced type.
   *
   * @return the reference type, never null
   */
  public Type getType() {
    return type;
  }
}