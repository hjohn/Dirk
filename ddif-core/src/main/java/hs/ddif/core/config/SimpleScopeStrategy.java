package hs.ddif.core.config;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.util.Annotations;
import hs.ddif.spi.config.ScopeStrategy;
import hs.ddif.spi.scope.ScopeResolver;

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
  private final Class<? extends Annotation> singletonAnnotationClass;
  private final Class<? extends Annotation> dependentAnnotationClass;

  /**
   * Constructs a new instance.
   *
   * @param scopeAnnotationClass an annotation {@link Class} to use for identifying scope annotations, cannot be {@code null}
   * @param singletonAnnotationClass an annotation {@link Class} which indicates singletons, cannot be {@code null}
   * @param dependentAnnotationClass an annotation {@link Class} which indicates dependent scoped objects, cannot be {@code null}
   */
  public SimpleScopeStrategy(Class<? extends Annotation> scopeAnnotationClass, Class<? extends Annotation> singletonAnnotationClass, Class<? extends Annotation> dependentAnnotationClass) {
    this.scopeAnnotationClass = Objects.requireNonNull(scopeAnnotationClass, "scopeAnnotationClass");
    this.singletonAnnotationClass = Objects.requireNonNull(singletonAnnotationClass, "singletonAnnotationClass");
    this.dependentAnnotationClass = Objects.requireNonNull(dependentAnnotationClass, "dependentAnnotationClass");
  }

  @Override
  public boolean isPseudoScope(ScopeResolver scopeResolver) {
    return scopeResolver.getAnnotationClass() == singletonAnnotationClass || scopeResolver.getAnnotationClass() == dependentAnnotationClass;
  }

  @Override
  public Class<? extends Annotation> getDependentAnnotationClass() {
    return dependentAnnotationClass;
  }

  @Override
  public Class<? extends Annotation> getSingletonAnnotationClass() {
    return singletonAnnotationClass;
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
