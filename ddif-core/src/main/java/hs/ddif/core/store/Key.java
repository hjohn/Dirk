package hs.ddif.core.store;

import hs.ddif.core.util.Primitives;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a {@link Type} with a set of qualifier {@link Annotation}s.
 */
public final class Key {
  private final Set<Annotation> qualifiers;
  private final Type type;

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @param qualifiers a collection of qualifier {@link Annotation}s, cannot be {@code null} or contain {@code null}s but can be empty
   */
  public Key(Type type, Collection<Annotation> qualifiers) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(qualifiers == null) {
      throw new IllegalArgumentException("qualifiers cannot be null");
    }

    this.type = Primitives.toBoxed(type);
    this.qualifiers = Collections.unmodifiableSet(new HashSet<>(qualifiers));
  }

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be {@code null}
   */
  public Key(Type type) {
    this(type, Set.of());
  }

  /**
   * Returns the {@link Type}.
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

    Key other = (Key)obj;

    if(!qualifiers.equals(other.qualifiers)) {
      return false;
    }
    if(!type.equals(other.type)) {
      return false;
    }

    return true;
  }
}