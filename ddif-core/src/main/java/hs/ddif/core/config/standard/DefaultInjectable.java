package hs.ddif.core.config.standard;

import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.inject.injection.Injection;
import hs.ddif.core.inject.injection.ObjectFactory;
import hs.ddif.core.inject.instantiation.InstanceCreationFailure;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of {@link Injectable}.
 */
public class DefaultInjectable implements Injectable {
  private final Type type;
  private final Set<Annotation> qualifiers;
  private final List<Binding> bindings;
  private final Annotation scope;
  private final Object discriminator;
  private final ObjectFactory objectFactory;
  private final int hashCode;

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be null
   * @param qualifiers a set of qualifier {@link Annotation}s, cannot be null or contain nulls, but can be empty
   * @param bindings a list of {@link Binding}s, cannot be null or contain nulls, but can be empty
   * @param scope a scope {@link Annotation}, can be null
   * @param discriminator an object to serve as a discriminator for similar injectables, can be null
   * @param objectFactory an {@link ObjectFactory}, cannot be null
   */
  public DefaultInjectable(Type type, Set<Annotation> qualifiers, List<Binding> bindings, Annotation scope, Object discriminator, ObjectFactory objectFactory) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(qualifiers == null) {
      throw new IllegalArgumentException("qualifiers cannot be null");
    }
    if(bindings == null) {
      throw new IllegalArgumentException("bindings cannot be null");
    }
    if(objectFactory == null) {
      throw new IllegalArgumentException("objectFactory cannot be null");
    }

    this.type = type;
    this.qualifiers = Collections.unmodifiableSet(new HashSet<>(qualifiers));
    this.bindings = Collections.unmodifiableList(new ArrayList<>(bindings));
    this.scope = scope;
    this.discriminator = discriminator;
    this.objectFactory = objectFactory;
    this.hashCode = Objects.hash(type, discriminator, getQualifiers());
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return qualifiers;
  }

  @Override
  public List<Binding> getBindings() {
    return bindings;
  }

  @Override
  public Annotation getScope() {
    return scope;
  }

  @Override
  public Object createInstance(List<Injection> injections) throws InstanceCreationFailure {
    return objectFactory.createInstance(injections);
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

    DefaultInjectable other = (DefaultInjectable)obj;

    return type.equals(other.type)
      && Objects.equals(discriminator, other.discriminator)
      && getQualifiers().equals(other.getQualifiers());
  }

  @Override
  public String toString() {
    return "Injectable[" + (qualifiers.isEmpty() ? "" : qualifiers.stream().map(Object::toString).collect(Collectors.joining(" ")) + " ") + type.getTypeName() + (discriminator instanceof AccessibleObject ? " <- " + discriminator : "") + "]";
  }
}
