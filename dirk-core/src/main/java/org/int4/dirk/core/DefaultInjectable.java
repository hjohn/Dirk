package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.core.definition.ExtendedScopeResolver;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.definition.InjectionTarget;
import org.int4.dirk.core.definition.QualifiedType;
import org.int4.dirk.core.definition.injection.Constructable;
import org.int4.dirk.core.definition.injection.Injection;

/**
 * An implementation of {@link Injectable}.
 */
final class DefaultInjectable<T> implements Injectable<T> {
  private final Type ownerType;
  private final Set<Type> types;
  private final QualifiedType qualifiedType;
  private final List<InjectionTarget> injectionTargets;
  private final ExtendedScopeResolver scopeResolver;
  private final AnnotatedElement discriminator;
  private final Constructable<T> constructable;
  private final int hashCode;

  /**
   * Constructs a new instance.
   *
   * @param ownerType a {@link Type}, cannot be {@code null}
   * @param types a set of {@link Type} of this injectable, cannot be {@code null} or contain {@code null}s
   * @param qualifiedType a {@link QualifiedType}, cannot be {@code null}
   * @param injectionTargets a list of {@link InjectionTarget}s, cannot be {@code null} or contain {@code null}s, but can be empty
   * @param scopeResolver an {@link ExtendedScopeResolver}, cannot be {@code null}
   * @param discriminator an object to serve as a discriminator for similar injectables, cannot be {@code null}
   * @param constructable a {@link Constructable}, cannot be {@code null}
   */
  DefaultInjectable(Type ownerType, Set<Type> types, QualifiedType qualifiedType, List<InjectionTarget> injectionTargets, ExtendedScopeResolver scopeResolver, AnnotatedElement discriminator, Constructable<T> constructable) {
    if(ownerType == null) {
      throw new IllegalArgumentException("ownerType");
    }
    if(types == null) {
      throw new IllegalArgumentException("types");
    }
    if(qualifiedType == null) {
      throw new IllegalArgumentException("qualifiedType");
    }
    if(injectionTargets == null) {
      throw new IllegalArgumentException("injectionTargets");
    }
    if(scopeResolver == null) {
      throw new IllegalArgumentException("scopeResolver");
    }
    if(constructable == null) {
      throw new IllegalArgumentException("constructable");
    }
    if(discriminator == null) {
      throw new IllegalArgumentException("discriminator");
    }
    if(!types.contains(qualifiedType.getType())) {
      throw new IllegalArgumentException("types must contain base type: " + qualifiedType.getType());
    }

    this.ownerType = ownerType;
    this.types = Collections.unmodifiableSet(new HashSet<>(types));
    this.qualifiedType = qualifiedType;
    this.injectionTargets = Collections.unmodifiableList(new ArrayList<>(injectionTargets));
    this.scopeResolver = scopeResolver;
    this.discriminator = discriminator;
    this.constructable = constructable;
    this.hashCode = calculateHash();
  }

  private int calculateHash() {
    return Objects.hash(qualifiedType, ownerType, discriminator);
  }

  @Override
  public Type getType() {
    return qualifiedType.getType();
  }

  @Override
  public Set<Type> getTypes() {
    return types;
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return qualifiedType.getQualifiers();
  }

  @Override
  public List<InjectionTarget> getInjectionTargets() {
    return injectionTargets;
  }

  @Override
  public ExtendedScopeResolver getScopeResolver() {
    return scopeResolver;
  }

  @Override
  public T create(List<Injection> injections) throws CreationException {
    return constructable.create(injections);
  }

  @Override
  public void destroy(T instance) {
    constructable.destroy(instance);
  }

  @Override
  public boolean needsDestroy() {
    return constructable.needsDestroy();
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    DefaultInjectable<?> other = (DefaultInjectable<?>)obj;

    return qualifiedType.equals(other.qualifiedType)
      && ownerType.equals(other.ownerType)
      && discriminator.equals(other.discriminator);
  }

  @Override
  public String toString() {
    String qualifiers = getQualifiers().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + (getQualifiers().isEmpty() ? "" : " ");

    if(discriminator instanceof AccessibleObject) {
      return "Producer [" + qualifiers + discriminator + "]";
    }
    if(discriminator instanceof Class) {
      return "Class [" + qualifiers + ((Class<?>)discriminator).getName() + "]";
    }

    return "Instance of [" + qualifiedType + " -> " + discriminator + "]";
  }
}
