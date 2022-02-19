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

import javax.inject.Qualifier;

/**
 * Represents a {@link Type} with a set of qualifier {@link Annotation}s.
 */
public class Key implements QualifiedType {
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
    if(!qualifiers.stream().allMatch(Key::isQualifier)) {
      throw new IllegalArgumentException("qualifiers must all be annotations annotated with @Qualifier: " + qualifiers);
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

  @Override
  public Set<Annotation> getQualifiers() {
    return qualifiers;
  }

  @Override
  public Type getType() {
    return type;
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

  private static boolean isQualifier(Annotation annotation) {
    return annotation.annotationType().getAnnotation(Qualifier.class) != null;
  }
}