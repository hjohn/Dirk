package org.int4.dirk.core;

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

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.definition.BadQualifiedTypeException;
import org.int4.dirk.core.definition.Binding;
import org.int4.dirk.core.definition.ExtendedScopeResolver;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.definition.InjectableFactory;
import org.int4.dirk.core.definition.InjectionTarget;
import org.int4.dirk.core.definition.Instantiator;
import org.int4.dirk.core.definition.QualifiedType;
import org.int4.dirk.core.definition.injection.Constructable;
import org.int4.dirk.core.util.Key;
import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.spi.config.ScopeStrategy;
import org.int4.dirk.spi.instantiation.Resolution;
import org.int4.dirk.spi.scope.ScopeResolver;
import org.int4.dirk.util.Types;

/**
 * An {@link InjectableFactory} which creates {@link Injectable}s given
 * a {@link Type}, an {@link AnnotatedElement}, a list of {@link Binding}s and
 * a {@link Constructable}.
 */
class DefaultInjectableFactory implements InjectableFactory {
  private final ScopeResolverManager scopeResolverManager;
  private final InstanceFactory instanceFactory;
  private final AnnotationStrategy annotationStrategy;
  private final ScopeStrategy scopeStrategy;
  private final Set<Class<?>> extendedTypes;

  /**
   * Constructs a new instance.
   *
   * @param scopeResolverManager a {@link ScopeResolverManager}, cannot be {@code null}
   * @param instanceFactory an {@link InstanceFactory}, cannot be {@code null}
   * @param annotationStrategy a {@link AnnotationStrategy}, cannot be {@code null}
   * @param scopeStrategy a {@link ScopeStrategy}, cannot be {@code null}
   * @param extendedTypes a set of {@link Class} for which injection target extensions are in use, cannot be {@code null} or contain {@code null} but can be empty
   */
  DefaultInjectableFactory(ScopeResolverManager scopeResolverManager, InstanceFactory instanceFactory, AnnotationStrategy annotationStrategy, ScopeStrategy scopeStrategy, Set<Class<?>> extendedTypes) {
    this.scopeResolverManager = Objects.requireNonNull(scopeResolverManager, "scopeResolverManager");
    this.instanceFactory = Objects.requireNonNull(instanceFactory, "instanceFactory");
    this.annotationStrategy = Objects.requireNonNull(annotationStrategy, "annotationStrategy");
    this.scopeStrategy = Objects.requireNonNull(scopeStrategy, "scopeStrategy");
    this.extendedTypes = Objects.requireNonNull(extendedTypes, "extendedTypes");
  }

  @Override
  public <T> Injectable<T> create(Type ownerType, Member member, AnnotatedElement element, List<Binding> bindings, Constructable<T> constructable) throws DefinitionException {
    try {
      if(ownerType == null) {
        throw new IllegalArgumentException("ownerType cannot be null");
      }
      if(element == null) {
        throw new IllegalArgumentException("element cannot be null");
      }
      if(bindings == null) {
        throw new IllegalArgumentException("bindings cannot be null");
      }
      if(constructable == null) {
        throw new IllegalArgumentException("constructable cannot be null");
      }

      Set<Annotation> injectAnnotations = annotationStrategy.getInjectAnnotations(element);

      if(!injectAnnotations.isEmpty()) {
        throw new DefinitionException(element, "should not have an inject annotation, but found: " + injectAnnotations.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()));
      }

      for(Binding binding : bindings) {
        if(binding.getAnnotatedElement() != null && scopeStrategy.getScope(binding.getAnnotatedElement()) != null) {
          throw new DefinitionException(binding.getAnnotatedElement(), "should not have a scope annotation, but found: " + scopeStrategy.getScope(binding.getAnnotatedElement()));
        }
      }

      Type type = member == null ? ownerType : extractType(ownerType, member, element);

      if(extendedTypes.contains(Types.raw(type))) {
        throw new DefinitionException(element, "cannot be registered as it conflicts with an InjectionTargetExtension for type: " + Types.raw(type));
      }

      Annotation scope = scopeStrategy.getScope(element);
      ScopeResolver scopeResolver = scopeResolverManager.getScopeResolver(scope == null ? scopeStrategy.getDefaultAnnotation() : scope);
      boolean isPseudoScope = scopeStrategy.isPseudoScope(scopeResolver.getAnnotation());

      ExtendedScopeResolver extendedScopeResolver = new ExtendedScopeResolver(scopeResolver, isPseudoScope, scopeResolver.getAnnotation().equals(scopeStrategy.getDependentAnnotation()));

      return new DefaultInjectable<>(
        ownerType,
        Types.getGenericSuperTypes(type).stream().filter(t -> !extendedTypes.contains(Types.raw(t))).collect(Collectors.toSet()),
        new QualifiedType(type, annotationStrategy.getQualifiers(element)),
        bindings.stream().map(b -> toInjectionTarget(b, scopeResolver)).collect(Collectors.toList()),
        extendedScopeResolver,
        element,
        constructable
      );
    }
    catch(BadQualifiedTypeException e) {
      throw new DefinitionException(element, "has unsuitable type", e);
    }
  }

  private InjectionTarget toInjectionTarget(Binding binding, ScopeResolver scopeResolver) {
    Instantiator<?> instantiator = instanceFactory.createInstantiator(new Key(binding.getType(), binding.getQualifiers()), binding.isOptional(), scopeResolver.getAnnotation());

    return new InjectionTarget() {
      @Override
      public Binding getBinding() {
        return binding;
      }

      @SuppressWarnings("unchecked")
      @Override
      public <T> Instantiator<T> getInstantiator() {
        return (Instantiator<T>)instantiator;
      }

      @Override
      public Resolution getResolution() {
        return getInstantiator().getResolution();
      }

      @Override
      public Key getElementKey() {
        return getInstantiator().getElementKey();
      }

      @Override
      public String toString() {
        return binding.toString();
      }
    };
  }

  private static Type extractType(Type ownerType, Member member, AnnotatedElement element) throws DefinitionException {
    Map<TypeVariable<?>, Type> typeArguments = Types.getTypeArguments(ownerType, member.getDeclaringClass());

    if(typeArguments == null) {
      throw new IllegalArgumentException("ownerType must be assignable to member's declaring class: " + ownerType + "; declaring class: " + member.getDeclaringClass());
    }

    Type returnType = Types.resolveVariables(typeArguments, member instanceof Method ? ((Method)member).getGenericReturnType() : ((Field)member).getGenericType());

    if(returnType == null) {
      throw new DefinitionException(element, "has unresolvable return type");
    }

    return returnType;
  }
}
