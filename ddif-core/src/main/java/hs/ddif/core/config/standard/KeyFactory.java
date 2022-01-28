package hs.ddif.core.config.standard;

import hs.ddif.core.store.Key;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Qualifier;

/**
 * Creates {@link Key}s based on a {@link Type} and an array of untyped qualifier annotations.
 */
public class KeyFactory {

  /**
   * Constructs a new instance. This takes an optional array of qualifiers. Qualifiers
   * can be {@link Annotation} instances or {@link Class}&lt;? extends Annotation&gt; instances.
   *
   * <p>Annotations which are not meta-annotated with {@link Qualifier}s will be rejected,
   * as will any parameter that cannot be converted to an annotation.
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
