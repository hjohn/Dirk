package hs.ddif;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

public class Key {
  private final Set<Annotation> qualifiers;
  private final Class<?> type;

  public Key(Set<Annotation> qualifiers, Class<?> type) {
    this.qualifiers = qualifiers;
    this.type = type;
  }

  public Key(Class<?> type) {
    this(Collections.<Annotation>emptySet(), type);
  }

  public Set<Annotation> getQualifiers() {
    return qualifiers;
  }

  public Class<?> getType() {
    return type;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("[");

    for(Annotation annotation : getQualifiers()) {
      if(builder.length() > 1) {
        builder.append(" ");
      }
      builder.append("@");
      builder.append(annotation.annotationType().getName());
    }

    builder.append(type.getName());
    builder.append("]");

    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((qualifiers == null) ? 0 : qualifiers.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj)
      return true;
    if(obj == null)
      return false;
    if(getClass() != obj.getClass())
      return false;
    Key other = (Key)obj;
    if(qualifiers == null) {
      if(other.qualifiers != null)
        return false;
    }
    else if(!qualifiers.equals(other.qualifiers))
      return false;
    if(type == null) {
      if(other.type != null)
        return false;
    }
    else if(!type.equals(other.type))
      return false;
    return true;
  }



}