package hs.ddif.core.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeReference<T> {
  private final Type type;

  private volatile Constructor<?> constructor;

  protected TypeReference() {
    Type superclass = getClass().getGenericSuperclass();

    if(superclass instanceof Class) {
      throw new IllegalStateException("Missing type parameter.");
    }

    type = ((ParameterizedType)superclass).getActualTypeArguments()[0];
  }

  /**
   * Instantiates a new instance of {@code T} using the default, no-arg constructor.
   *
   * @return a new instanceof of {@code T}, never null
   * @throws ReflectiveOperationException when unable to instantiate the type using reflection
   */
  @SuppressWarnings("unchecked")
  public T newInstance() throws ReflectiveOperationException {
    if(constructor == null) {
      Class<?> rawType = type instanceof Class<?> ? (Class<?>)type : (Class<?>)((ParameterizedType)type).getRawType();
      constructor = rawType.getConstructor();
    }

    return (T)constructor.newInstance();
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