package hs.ddif.core.definition;

import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.Set;

import javax.inject.Scope;

/**
 * Utility methods related to scope annotations.
 */
public class ScopeAnnotations {
  private static final Annotation SCOPE = Annotations.of(Scope.class);

  /**
   * Finds a {@link Scope} annotation on the given {@link Member}.
   *
   * @param element an {@link Member}, cannot be {@code null}
   * @return a {@link Scope} annotation, or {@code null} if not present
   */
  public static Annotation find(AnnotatedElement element) {
    Set<Annotation> matchingAnnotations = Annotations.findDirectlyMetaAnnotatedAnnotations(element, SCOPE);

    if(matchingAnnotations.size() > 1) {
      throw new DefinitionException(element, "cannot have multiple scope annotations, but found: " + matchingAnnotations);
    }

    return matchingAnnotations.isEmpty() ? null : matchingAnnotations.iterator().next();
  }
}
