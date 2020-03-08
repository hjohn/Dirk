package hs.ddif.core.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeReference<T> {
  private final Type type;

  protected TypeReference() {
    Type superclass = getClass().getGenericSuperclass();

    if(superclass instanceof Class) {
      throw new IllegalStateException("Missing type parameter.");
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