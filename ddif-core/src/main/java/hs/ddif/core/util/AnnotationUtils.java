package hs.ddif.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

public class AnnotationUtils {

  /**
   * Finds all annotations placed on the given {@link AnnotatedElement} of type <code>annotationClass</code> or meta-annotated
   * with type <code>annotationClass</code>.
   *
   * @param element an element to scan
   * @param annotationClass an annotation class to find
   * @return a {@link List} of {@link Annotation} that matches, never null, can be empty
   */
  public static List<Annotation> findAnnotations(AnnotatedElement element, Class<? extends Annotation> annotationClass) {
    List<Annotation> matchingAnnotations = new ArrayList<>();

    for(Annotation annotation : element.getAnnotations()) {
      if(annotation.annotationType().equals(annotationClass) || annotation.annotationType().getAnnotation(annotationClass) != null) {  // TODO this only recurses one level
        matchingAnnotations.add(annotation);
      }
    }

    return matchingAnnotations;
  }

  /**
   * Creates an annotation of the given class.
   *
   * @param cls a {@link Class}
   * @return an {@link Annotation}, never null
   */
  public static Annotation of(final Class<? extends Annotation> cls) {
    return new AnnotationImplementation(cls);
  }

  private static final class AnnotationImplementation implements Annotation {
    private final Class<? extends Annotation> cls;

    private AnnotationImplementation(Class<? extends Annotation> cls) {
      this.cls = cls;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return cls;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }
      if(obj == null) {
        return false;
      }
      if(!cls.isInstance(obj)) {
        return false;
      }

      return true;
    }
  }
}
