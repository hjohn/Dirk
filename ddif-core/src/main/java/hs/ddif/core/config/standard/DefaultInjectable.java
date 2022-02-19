package hs.ddif.core.config.standard;

import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.QualifiedType;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.instantiation.injection.ObjectFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of {@link Injectable}.
 */
public final class DefaultInjectable implements Injectable {
  private final QualifiedType qualifiedType;
  private final List<Binding> bindings;
  private final Annotation scope;
  private final Object discriminator;
  private final ObjectFactory objectFactory;
  private final int hashCode;

  /**
   * Constructs a new instance.
   *
   * @param qualifiedType a {@link QualifiedType}, cannot be {@code null}
   * @param bindings a list of {@link Binding}s, cannot be {@code null} or contain {@code null}s, but can be empty
   * @param scope a scope {@link Annotation}, can be {@code null}
   * @param discriminator an object to serve as a discriminator for similar injectables, can be {@code null}
   * @param objectFactory an {@link ObjectFactory}, cannot be {@code null}
   */
  public DefaultInjectable(QualifiedType qualifiedType, List<Binding> bindings, Annotation scope, Object discriminator, ObjectFactory objectFactory) {
    if(qualifiedType == null) {
      throw new IllegalArgumentException("qualifiedType cannot be null");
    }
    if(bindings == null) {
      throw new IllegalArgumentException("bindings cannot be null");
    }
    if(objectFactory == null) {
      throw new IllegalArgumentException("objectFactory cannot be null");
    }

    this.qualifiedType = qualifiedType;
    this.bindings = Collections.unmodifiableList(new ArrayList<>(bindings));
    this.scope = scope;
    this.discriminator = discriminator;
    this.objectFactory = objectFactory;
    this.hashCode = calculateHash();
  }

  private int calculateHash() {
    return Objects.hash(qualifiedType, discriminator);
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

    return qualifiedType.equals(other.qualifiedType)
      && Objects.equals(discriminator, other.discriminator);
  }

  @Override
  public String toString() {
    return "Injectable[" + qualifiedType + (discriminator instanceof AccessibleObject ? " <- " + discriminator : "") + "]";
  }
}
