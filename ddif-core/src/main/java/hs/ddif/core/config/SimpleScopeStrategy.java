package hs.ddif.core.config;

import hs.ddif.api.annotation.ScopeStrategy;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.scope.ScopeResolver;
import hs.ddif.api.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of {@link ScopeStrategy}.
 */
public class SimpleScopeStrategy implements ScopeStrategy {
  private final Class<? extends Annotation> scopeAnnotationClass;

  /**
   * Constructs a new instance.
   *
   * @param scopeAnnotationClass an annotation {@link Class} to use for identifying scope annotations, cannot be {@code null}
   */
  public SimpleScopeStrategy(Class<? extends Annotation> scopeAnnotationClass) {
    this.scopeAnnotationClass = Objects.requireNonNull(scopeAnnotationClass, "scopeAnnotationClass");
  }

  @Override
  public Class<? extends Annotation> getScope(AnnotatedElement element) throws DefinitionException {
    Set<Annotation> scopes = Annotations.findDirectlyMetaAnnotatedAnnotations(element, scopeAnnotationClass);

    if(scopes.size() > 1) {
      throw new DefinitionException(element, "cannot have multiple scope annotations, but found: " + scopes.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()));
    }

    if(scopes.isEmpty()) {
      return null;
    }

    return scopes.iterator().next().annotationType();
  }
}
