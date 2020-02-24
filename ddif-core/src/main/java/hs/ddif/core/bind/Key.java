package hs.ddif.core.bind;

import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Key {
  private final Set<AnnotationDescriptor> qualifiers;
  private final Type type;

  public Key(Type type, Set<AnnotationDescriptor> qualifiers) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }

    this.type = type;
    this.qualifiers = Collections.unmodifiableSet(new HashSet<>(qualifiers));
  }

  public Key(Type type, AnnotationDescriptor... qualifiers) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }

    this.type = type;
    this.qualifiers = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(qualifiers)));
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
    final int prime = 31;
    int result = 1;
    result = prime * result + qualifiers.hashCode();
    result = prime * result + type.hashCode();
    return result;
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

    if(qualifiers == null) {
      if(other.qualifiers != null) {
        return false;
      }
    }
    else if(!qualifiers.equals(other.qualifiers)) {
      return false;
    }
    if(type == null) {
      if(other.type != null) {
        return false;
      }
    }
    else if(!type.equals(other.type)) {
      return false;
    }

    return true;
  }
}