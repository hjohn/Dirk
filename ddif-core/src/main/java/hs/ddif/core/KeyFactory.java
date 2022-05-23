package hs.ddif.core;

import hs.ddif.core.definition.Key;
import hs.ddif.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates {@link Key}s based on a {@link Type} and an array of untyped qualifier annotations.
 */
class KeyFactory {

  /**
   * Constructs a new instance. This takes an optional array of qualifiers. Qualifiers
   * can be {@link Annotation} instances or {@link Class}&lt;? extends Annotation&gt; instances.
   *
   * <p>Parameters that cannot be converted to an annotation will be rejected.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @param qualifiers an optional array of parameters that can be converted to qualifiers
   * @return a {@link Key}, never null
   */
  public static Key of(Type type, Object... qualifiers) {
    List<Annotation> qualifierAnnotations = new ArrayList<>();

    for(Object qualifier : qualifiers) {
      if(qualifier instanceof Annotation) {
        qualifierAnnotations.add((Annotation)qualifier);
      }
      else {
        if(qualifier instanceof Class) {
          Class<?> cls = (Class<?>)qualifier;

          if(cls.isAnnotation()) {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)cls;

            qualifierAnnotations.add(Annotations.of(annotationClass));
            continue;
          }
        }

        throw new IllegalArgumentException("Unsupported qualifier, must be Class<? extends Annotation> or Annotation: " + qualifier);
      }
    }

    return new Key(type, qualifierAnnotations);
  }
}
