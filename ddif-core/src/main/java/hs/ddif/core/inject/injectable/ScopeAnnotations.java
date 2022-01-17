package hs.ddif.core.inject.injectable;

import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Scope;

/**
 * Utility methods related to scope annotations.
 */
public class ScopeAnnotations {
  private static final Annotation SCOPE = Annotations.of(Scope.class);

  /**
   * Finds a {@link Scope} annotation on the given {@link Class}.
   *
   * @param cls an {@link Class}, cannot be {@code null}
   * @return a {@link Scope} annotation, or {@code null} if not present
   */
  public static Annotation find(Class<?> cls)  {
    return find(cls, text -> new DefinitionException(cls, text));
  }

  /**
   * Finds a {@link Scope} annotation on the given {@link Member}.
   *
   * @param member an {@link Member}, cannot be {@code null}
   * @return a {@link Scope} annotation, or {@code null} if not present
   */
  public static Annotation find(Member member) {
    return find((AnnotatedElement)member, text -> new DefinitionException(member, text));
  }

  private static Annotation find(AnnotatedElement element, Function<String, DefinitionException> exceptionProvider) {
    Set<Annotation> matchingAnnotations = Annotations.findDirectlyMetaAnnotatedAnnotations(element, SCOPE);

    if(matchingAnnotations.size() > 1) {
      throw exceptionProvider.apply("cannot have multiple scope annotations, but found: " + matchingAnnotations);
    }

    return matchingAnnotations.isEmpty() ? null : matchingAnnotations.iterator().next();
  }
}
