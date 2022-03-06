package hs.ddif.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Support functions for {@link Method}s.
 */
public class Fields {

  /**
   * Find all fields with the given annotation.
   *
   * @param cls a {@link Class} to search, cannot be {@code null}
   * @param annotation an annotation {@link Class}, cannot be {@code null}
   * @return a list of {@link Field}s, never {@code null} or contains {@code null}, but can be empty
   */
  public static List<Field> findAnnotated(Class<?> cls, Class<? extends Annotation> annotation) {
    List<Field> fields = new ArrayList<>();

    for(Class<?> c : Types.getSuperTypes(cls)) {
      for(Field f : c.getDeclaredFields()) {
        if(f.isAnnotationPresent(annotation)) {
          fields.add(f);
        }
      }
    }

    return fields;
  }
}
