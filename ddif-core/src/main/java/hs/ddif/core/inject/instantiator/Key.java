package hs.ddif.core.inject.instantiator;

import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Key {
  private final Set<AnnotationDescriptor> qualifiers;
  private final Type type;

  public Key(Type type, Set<AnnotationDescriptor> qualifiers) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(qualifiers == null) {
      throw new IllegalArgumentException("qualifiers cannot be null");
    }

    this.type = type;
    this.qualifiers = Collections.unmodifiableSet(new HashSet<>(qualifiers));
  }

  public Set<AnnotationDescriptor> getQualifiers() {
    return qualifiers;
  }

  public AnnotationDescriptor[] getQualifiersAsArray() {
    return qualifiers.toArray(new AnnotationDescriptor[qualifiers.size()]);
  }

  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("[");

    for(AnnotationDescriptor qualifier : getQualifiersAsArray()) {
      if(builder.length() > 1) {
        builder.append(" ");
      }
      builder.append(qualifier);
    }

    if(builder.length() > 1) {
      builder.append(" ");
    }
    builder.append(type);
    builder.append("]");

    return builder.toString();
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