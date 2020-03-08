package hs.ddif.core;

import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.store.AbstractResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Objects;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;

public class ProvidedInjectable extends AbstractResolvableInjectable {
  private final Provider<?> provider;
  private final Type ownerType;

  public ProvidedInjectable(Provider<?> provider, AnnotationDescriptor... descriptors) {
    this(provider.getClass(), provider, descriptors);
  }

  public ProvidedInjectable(Type ownerType, AnnotationDescriptor... descriptors) {
    this(ownerType, null, descriptors);
  }

  private ProvidedInjectable(Type ownerType, Provider<?> provider, AnnotationDescriptor... descriptors) {
    super(Collections.emptyMap(), null, determineProvidedType(ownerType), true, descriptors);

    this.ownerType = ownerType;
    this.provider = provider;
  }

  private static Type determineProvidedType(Type ownerType) {
    if(ownerType == null) {
      throw new IllegalArgumentException("ownerType cannot be null");
    }

    return TypeUtils.getTypeArguments(ownerType, Provider.class).get(Provider.class.getTypeParameters()[0]);
  }

  @Override
  public Object getInstance(Instantiator instantiator, NamedParameter... namedParameters) throws BeanResolutionException {
    try {
      return provider == null ? ((Provider<?>)instantiator.getParameterizedInstance(ownerType, namedParameters, getQualifiers().toArray())).get() : provider.get();
    }
    catch(Exception e) {
      throw new BeanResolutionException(getType(), e);
    }
  }

  /**
   * Returns the type which implements the Provider interface.
   *
   * @return the type which implements the Provider interface, never null
   */
  public Type getOwnerType() {
    return ownerType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(provider, getQualifiers(), ownerType);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ProvidedInjectable other = (ProvidedInjectable)obj;

    return getQualifiers().equals(other.getQualifiers())
        && Objects.equals(provider, other.provider)
        && Objects.equals(ownerType, other.ownerType);
  }

  @Override
  public String toString() {
    return "Injectable-Provider(" + getType() + " + " + getQualifiers() + ")";
  }
}
