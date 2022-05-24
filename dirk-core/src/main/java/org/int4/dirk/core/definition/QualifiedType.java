package org.int4.dirk.core.definition;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.int4.dirk.util.Primitives;
import org.int4.dirk.util.Types;

/**
 * Represents a fully resolved {@link Type} with a set of qualifier {@link Annotation}s.
 */
public final class QualifiedType {
  private final Type type;
  private final Set<Annotation> qualifiers;

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @param qualifiers a collection of qualifier {@link Annotation}s, cannot be {@code null} or contain {@code null}s but can be empty
   * @throws BadQualifiedTypeException when the given type or qualifiers are not suitable for injection
   */
  public QualifiedType(Type type, Collection<Annotation> qualifiers) throws BadQualifiedTypeException {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(qualifiers == null) {
      throw new IllegalArgumentException("qualifiers cannot be null");
    }

    this.type = Primitives.toBoxed(type);
    this.qualifiers = Collections.unmodifiableSet(new HashSet<>(qualifiers));

    if((!(type instanceof Class) && !(type instanceof ParameterizedType)) || Types.containsTypeVariables(type)) {
      throw new BadQualifiedTypeException("[" + this + "] cannot have unresolvable type variables or wild cards");
    }
    if(type == void.class || type == Void.class) {
      throw new BadQualifiedTypeException("[" + this + "] cannot be void or Void");
    }
  }

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @throws BadQualifiedTypeException when the given type or qualifiers are not suitable for injection
   */
  public QualifiedType(Type type) throws BadQualifiedTypeException {
    this(type, Set.of());
  }

  /**
   * Returns the {@link Type}. The type is always fully resolved (no type variables)
   * and never {@code void}.
   *
   * @return the {@link Type}, never {@code null}
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns an immutable set of qualifier {@link Annotation}s.
   *
   * @return an immutable set of qualifier {@link Annotation}s, never {@code null} and never contains {@code null}s but can be empty
   */
  public Set<Annotation> getQualifiers() {
    return qualifiers;
  }

  @Override
  public String toString() {
    return (qualifiers.isEmpty() ? "" : qualifiers.stream().map(Object::toString).sorted().collect(Collectors.joining(" ")) + " ") + type.getTypeName();
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, qualifiers);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    QualifiedType other = (QualifiedType)obj;

    if(!type.equals(other.type)) {
      return false;
    }
    if(!qualifiers.equals(other.qualifiers)) {
      return false;
    }

    return true;
  }
}