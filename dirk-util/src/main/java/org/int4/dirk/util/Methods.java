package org.int4.dirk.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Support functions for {@link Method}s.
 */
public class Methods {

  /**
   * Find all methods with the given annotation.
   *
   * @param cls a {@link Class} to search, cannot be {@code null}
   * @param annotation an annotation {@link Class}, cannot be {@code null}
   * @return a list of {@link Method}s, never {@code null} or contains {@code null}, but can be empty
   */
  public static List<Method> findAnnotated(Class<?> cls, Class<? extends Annotation> annotation) {
    List<Method> methods = new ArrayList<>();

    for(Class<?> c : Types.getSuperTypes(cls)) {
      for(Method m : c.getDeclaredMethods()) {
        if(m.isAnnotationPresent(annotation)) {
          methods.add(m);
        }
      }
    }

    return methods;
  }
}
