package org.int4.dirk.util;

import java.lang.reflect.TypeVariable;

/**
 * Support functions for type variables.
 */
public class TypeVariables {

  /**
   * Extracts the type variable at the given index from the given {@link Class}.
   *
   * @param <T> the type of the class
   * @param cls a {@link Class}, cannot be {@code null}
   * @param index an index, ranging from 0 to the amount of type variables - 1, cannot be negative
   * @return a {@link TypeVariable}, never {@code null}
   * @throws NullPointerException when the given class was {@code null}
   * @throws IndexOutOfBoundsException when an index was given outside the valid range
   */
  public static <T> TypeVariable<Class<T>> get(Class<?> cls, int index) {
    @SuppressWarnings("unchecked")
    Class<T> castClass = (Class<T>)cls;

    return castClass.getTypeParameters()[index];
  }
}
