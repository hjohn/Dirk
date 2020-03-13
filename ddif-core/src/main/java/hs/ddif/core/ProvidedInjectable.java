package hs.ddif.core;

import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.store.AbstractResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;

public class ProvidedInjectable extends AbstractResolvableInjectable {
  private final Type ownerType;

  public ProvidedInjectable(Type ownerType, AnnotationDescriptor... descriptors) {
    super(List.of(), null, determineProvidedType(ownerType), true, descriptors);

    this.ownerType = ownerType;
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
      Provider<?> instance = instantiator.getParameterizedInstance(ownerType, namedParameters, getQualifiers().toArray());

      return instance.get();
    }
    catch(Exception e) {
      throw new BeanResolutionException(getType(), e);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(ownerType, getQualifiers());
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

    return Objects.equals(ownerType, other.ownerType)
        && getQualifiers().equals(other.getQualifiers());
  }

  @Override
  public String toString() {
    return "Injectable-Provider(" + getType() + " + " + getQualifiers() + ")";
  }
}
