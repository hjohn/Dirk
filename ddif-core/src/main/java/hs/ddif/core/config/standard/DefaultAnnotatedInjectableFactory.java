package hs.ddif.core.config.standard;

import hs.ddif.core.definition.AnnotatedInjectableFactory;
import hs.ddif.core.definition.BadQualifiedTypeException;
import hs.ddif.core.definition.DefinitionException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.QualifiedType;
import hs.ddif.core.definition.ScopeAnnotations;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.injection.ObjectFactory;
import hs.ddif.core.scope.ScopeResolverManager;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import javax.inject.Qualifier;

/**
 * An {@link AnnotatedInjectableFactory} which creates {@link Injectable}s given
 * a {@link Type}, an {@link AnnotatedElement}, a list of {@link Binding}s and
 * an {@link ObjectFactory}.
 */
public class DefaultAnnotatedInjectableFactory implements AnnotatedInjectableFactory {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);

  private final ScopeResolverManager scopeResolverManager;

  /**
   * Constructs a new instance.
   *
   * @param scopeResolverManager a {@link ScopeResolverManager}, cannot be {@code null}
   */
  public DefaultAnnotatedInjectableFactory(ScopeResolverManager scopeResolverManager) {
    this.scopeResolverManager = Objects.requireNonNull(scopeResolverManager, "scopeResolverManager cannot be null");
  }

  @Override
  public Injectable create(Type type, AnnotatedElement element, List<Binding> bindings, ObjectFactory objectFactory) {
    try {
      return new DefaultInjectable(
        new QualifiedType(type, Annotations.findDirectlyMetaAnnotatedAnnotations(element, QUALIFIER)),
        bindings,
        scopeResolverManager.getScopeResolver(ScopeAnnotations.find(element)),
        element,
        objectFactory
      );
    }
    catch(BadQualifiedTypeException e) {
      throw new DefinitionException(element, "has unsuitable type", e);
    }
  }
}
