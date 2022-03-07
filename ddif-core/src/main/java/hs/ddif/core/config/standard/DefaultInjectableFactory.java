package hs.ddif.core.config.standard;

import hs.ddif.core.definition.BadQualifiedTypeException;
import hs.ddif.core.definition.DefinitionException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.InjectableFactory;
import hs.ddif.core.definition.QualifiedType;
import hs.ddif.core.definition.bind.AnnotationStrategy;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.injection.Constructable;
import hs.ddif.core.scope.ScopeResolverManager;
import hs.ddif.core.util.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An {@link InjectableFactory} which creates {@link Injectable}s given
 * a {@link Type}, an {@link AnnotatedElement}, a list of {@link Binding}s and
 * a {@link Constructable}.
 */
public class DefaultInjectableFactory implements InjectableFactory {
  private final ScopeResolverManager scopeResolverManager;
  private final AnnotationStrategy annotationStrategy;

  /**
   * Constructs a new instance.
   *
   * @param scopeResolverManager a {@link ScopeResolverManager}, cannot be {@code null}
   * @param annotationStrategy a {@link AnnotationStrategy}, cannot be {@code null}
   */
  public DefaultInjectableFactory(ScopeResolverManager scopeResolverManager, AnnotationStrategy annotationStrategy) {
    this.scopeResolverManager = Objects.requireNonNull(scopeResolverManager, "scopeResolverManager cannot be null");
    this.annotationStrategy = Objects.requireNonNull(annotationStrategy, "annotationStrategy cannot be null");
  }

  @Override
  public <T> Injectable<T> create(Type ownerType, Member member, AnnotatedElement element, List<Binding> bindings, Constructable<T> constructable) {
    try {
      Set<Annotation> scopes = annotationStrategy.getScopes(element);

      if(scopes.size() > 1) {
        throw new DefinitionException(element, "cannot have multiple scope annotations, but found: " + scopes.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()));
      }
      if(annotationStrategy.isInjectAnnotated(element)) {
        throw new DefinitionException(element, "should not have an inject annotation, but found: " + annotationStrategy.getInjectAnnotations(element).stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()));
      }

      Type type = member == null ? ownerType : extractType(ownerType, member, element);

      return new DefaultInjectable<>(
        ownerType,
        new QualifiedType(type, annotationStrategy.getQualifiers(element)),
        bindings,
        scopeResolverManager.getScopeResolver(scopes.isEmpty() ? null : scopes.iterator().next()),
        element,
        constructable
      );
    }
    catch(BadQualifiedTypeException e) {
      throw new DefinitionException(element, "has unsuitable type", e);
    }
  }

  private static Type extractType(Type ownerType, Member member, AnnotatedElement element) {
    Map<TypeVariable<?>, Type> typeArguments = Types.getTypeArguments(ownerType, member.getDeclaringClass());

    if(typeArguments == null) {
      throw new IllegalArgumentException("ownerType must be assignable to member's declaring class: " + ownerType + "; declaring class: " + member.getDeclaringClass());
    }

    Type returnType = Types.unrollVariables(typeArguments, member instanceof Method ? ((Method)member).getGenericReturnType() : ((Field)member).getGenericType());

    if(returnType == null) {
      throw new DefinitionException(element, "has unresolvable return type");
    }

    return returnType;
  }
}
