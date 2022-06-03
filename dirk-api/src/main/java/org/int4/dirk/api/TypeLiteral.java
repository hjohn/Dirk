package org.int4.dirk.api;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Allows creation of parameterized types by sub-classing this class.
 *
 * <p>For example, to create a type {@code List<? extends String>} write:
 *
 * <pre>Type type = new TypeLiteral&lt;List&lt;? extends String>>() {}.getType()</pre>
 *
 * @param <T> the type represented
 */
public abstract class TypeLiteral<T> {
  private final Type type;

  /**
   * Constructs a new instance.
   */
  protected TypeLiteral() {
    Type superclass = getClass().getGenericSuperclass();

    if(superclass instanceof Class) {
      throw new IllegalStateException("Missing type parameter");
    }

    type = ((ParameterizedType)superclass).getActualTypeArguments()[0];
  }

  /**
   * Returns the type represented.
   *
   * @return the type represented, never {@code null}
   */
  public final Type getType() {
    return type;
  }

  /**
   * Returns the raw type.
   *
   * @return the raw type, never {@code null}
   */
  @SuppressWarnings("unchecked")
  public final Class<T> getRawType() {
    if(type instanceof Class) {
      return (Class<T>)type;
    }
    if(type instanceof ParameterizedType) {
      return (Class<T>)((ParameterizedType)type).getRawType();
    }
    if(type instanceof GenericArrayType) {
      return (Class<T>)Object[].class;
    }

    throw new IllegalStateException("Unsupported type: " + type);
  }
}
