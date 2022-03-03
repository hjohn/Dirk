package hs.ddif.core.config.standard;

import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.QualifiedType;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.instantiation.injection.ObjectFactory;
import hs.ddif.core.scope.ScopeResolver;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of {@link Injectable}.
 */
final class DefaultInjectable implements Injectable {
  private final Type ownerType;
  private final QualifiedType qualifiedType;
  private final List<Binding> bindings;
  private final ScopeResolver scopeResolver;
  private final Object discriminator;
  private final ObjectFactory objectFactory;
  private final int hashCode;

  /**
   * Constructs a new instance.
   *
   * @param ownerType a {@link Type}, cannot be {@code null}
   * @param qualifiedType a {@link QualifiedType}, cannot be {@code null}
   * @param bindings a list of {@link Binding}s, cannot be {@code null} or contain {@code null}s, but can be empty
   * @param scopeResolver a {@link ScopeResolver}, cannot be {@code null}
   * @param discriminator an object to serve as a discriminator for similar injectables, can be {@code null}
   * @param objectFactory an {@link ObjectFactory}, cannot be {@code null}
   */
  DefaultInjectable(Type ownerType, QualifiedType qualifiedType, List<Binding> bindings, ScopeResolver scopeResolver, Object discriminator, ObjectFactory objectFactory) {
    if(ownerType == null) {
      throw new IllegalArgumentException("ownerType cannot be null");
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
    if(objectFactory == null) {
      throw new IllegalArgumentException("objectFactory cannot be null");
    }

    this.ownerType = ownerType;
    this.qualifiedType = qualifiedType;
    this.bindings = Collections.unmodifiableList(new ArrayList<>(bindings));
    this.scopeResolver = scopeResolver;
    this.discriminator = discriminator;
    this.objectFactory = objectFactory;
    this.hashCode = calculateHash();
  }

  private int calculateHash() {
    return Objects.hash(qualifiedType, ownerType, discriminator);
  }

  @Override
  public QualifiedType getQualifiedType() {
    return qualifiedType;
  }

  @Override
  public List<Binding> getBindings() {
    return bindings;
  }

  @Override
  public ScopeResolver getScopeResolver() {
    return scopeResolver;
  }

  @Override
  public Object createInstance(List<Injection> injections) throws InstanceCreationFailure {
    return objectFactory.createInstance(injections);
  }

  @Override
  public void destroyInstance(Object instance, List<Injection> injections) {
    objectFactory.destroyInstance(instance, injections);
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

    return qualifiedType.equals(other.qualifiedType)
      && ownerType.equals(other.ownerType)
      && Objects.equals(discriminator, other.discriminator);
  }

  @Override
  public String toString() {
    return "Injectable[" + qualifiedType + (discriminator instanceof AccessibleObject ? " <- " + discriminator : "") + "]";
  }
}
