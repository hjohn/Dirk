package org.int4.dirk.library;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.spi.config.ScopeStrategy;
import org.int4.dirk.spi.scope.ScopeResolver;
import org.int4.dirk.util.Annotations;

/**
 * An implementation of {@link ScopeStrategy}.
 */
public class SimpleScopeStrategy implements ScopeStrategy {
  private final Class<? extends Annotation> scopeAnnotationClass;
  private final Annotation singletonAnnotation;
  private final Annotation dependentAnnotation;

  /**
   * Constructs a new instance.
   *
   * @param scopeAnnotationClass an annotation {@link Class} to use for identifying scope annotations, cannot be {@code null}
   * @param singletonAnnotation an annotation which indicates singletons, cannot be {@code null}
   * @param dependentAnnotation an annotation which indicates dependent scoped objects, cannot be {@code null}
   */
  public SimpleScopeStrategy(Class<? extends Annotation> scopeAnnotationClass, Annotation singletonAnnotation, Annotation dependentAnnotation) {
    this.scopeAnnotationClass = Objects.requireNonNull(scopeAnnotationClass, "scopeAnnotationClass");
    this.singletonAnnotation = Objects.requireNonNull(singletonAnnotation, "singletonAnnotation");
    this.dependentAnnotation = Objects.requireNonNull(dependentAnnotation, "dependentAnnotation");
  }

  @Override
  public boolean isPseudoScope(ScopeResolver scopeResolver) {
    return scopeResolver.getAnnotation().equals(singletonAnnotation) || scopeResolver.getAnnotation().equals(dependentAnnotation);
  }

  @Override
  public Annotation getDependentAnnotation() {
    return dependentAnnotation;
  }

  @Override
  public Annotation getSingletonAnnotation() {
    return singletonAnnotation;
  }

  @Override
  public Annotation getScope(AnnotatedElement element) throws DefinitionException {
    Set<Annotation> scopes = Annotations.findDirectlyMetaAnnotatedAnnotations(element, scopeAnnotationClass);

    if(scopes.size() > 1) {
      throw new DefinitionException(element, "cannot have multiple scope annotations, but found: " + scopes.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()));
    }

    if(scopes.isEmpty()) {
      return null;
    }

    return scopes.iterator().next();
  }
}
