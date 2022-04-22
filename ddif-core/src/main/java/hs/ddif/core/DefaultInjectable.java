package hs.ddif.core;

import hs.ddif.api.instantiation.InstanceCreationException;
import hs.ddif.core.definition.Binding;
import hs.ddif.core.definition.ExtendedScopeResolver;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.QualifiedType;
import hs.ddif.core.definition.injection.Constructable;
import hs.ddif.core.definition.injection.Injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An implementation of {@link Injectable}.
 */
final class DefaultInjectable<T> implements Injectable<T> {
  private final Type ownerType;
  private final Set<Type> types;
  private final QualifiedType qualifiedType;
  private final List<Binding> bindings;
  private final ExtendedScopeResolver scopeResolver;
  private final Object discriminator;
  private final Constructable<T> constructable;
  private final int hashCode;

  /**
   * Constructs a new instance.
   *
   * @param ownerType a {@link Type}, cannot be {@code null}
   * @param types a set of {@link Type} of this injectable, cannot be {@code null} or contain {@code null}s
   * @param qualifiedType a {@link QualifiedType}, cannot be {@code null}
   * @param bindings a list of {@link Binding}s, cannot be {@code null} or contain {@code null}s, but can be empty
   * @param scopeResolver an {@link ExtendedScopeResolver}, cannot be {@code null}
   * @param discriminator an object to serve as a discriminator for similar injectables, can be {@code null}
   * @param constructable a {@link Constructable}, cannot be {@code null}
   */
  DefaultInjectable(Type ownerType, Set<Type> types, QualifiedType qualifiedType, List<Binding> bindings, ExtendedScopeResolver scopeResolver, Object discriminator, Constructable<T> constructable) {
    if(ownerType == null) {
      throw new IllegalArgumentException("ownerType cannot be null");
    }
    if(types == null) {
      throw new IllegalArgumentException("types cannot be null");
    }
    if(qualifiedType == null) {
      throw new IllegalArgumentException("qualifiedType cannot be null");
    }
    if(bindings == null) {
      throw new IllegalArgumentException("bindings cannot be null");
    }
    if(scopeResolver == null) {
      throw new IllegalArgumentException("scopeResolver cannot be null");
    }
    if(constructable == null) {
      throw new IllegalArgumentException("constructable cannot be null");
    }
    if(!types.contains(qualifiedType.getType())) {
      throw new IllegalArgumentException("types must contain base type: " + qualifiedType.getType());
    }

    this.ownerType = ownerType;
    this.types = Collections.unmodifiableSet(new HashSet<>(types));
    this.qualifiedType = qualifiedType;
    this.bindings = Collections.unmodifiableList(new ArrayList<>(bindings));
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
  public List<Binding> getBindings() {
    return bindings;
  }

  @Override
  public ExtendedScopeResolver getScopeResolver() {
    return scopeResolver;
  }

  @Override
  public T create(List<Injection> injections) throws InstanceCreationException {
    return constructable.create(injections);
  }

  @Override
  public void destroy(T instance) {
    constructable.destroy(instance);
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
      && Objects.equals(discriminator, other.discriminator);
  }

  @Override
  public String toString() {
    return "Injectable[" + qualifiedType + (discriminator instanceof AccessibleObject ? " <- " + discriminator : "") + "]";
  }
}
