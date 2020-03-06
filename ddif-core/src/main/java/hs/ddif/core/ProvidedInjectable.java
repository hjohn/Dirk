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
  private final Class<?> classImplementingProvider;

  public ProvidedInjectable(Provider<?> provider, AnnotationDescriptor... descriptors) {
    this(provider.getClass(), provider, descriptors);
  }

  public ProvidedInjectable(Class<?> classImplementingProvider, AnnotationDescriptor... descriptors) {
    this(classImplementingProvider, null, descriptors);
  }

  private ProvidedInjectable(Class<?> classImplementingProvider, Provider<?> provider, AnnotationDescriptor... descriptors) {
    super(Collections.emptyMap(), null, determineProvidedClass(classImplementingProvider), true, descriptors);

    this.classImplementingProvider = classImplementingProvider;
    this.provider = provider;
  }

  private static Class<?> determineProvidedClass(Class<?> classImplementingProvider) {
    if(classImplementingProvider == null) {
      throw new IllegalArgumentException("classImplementingProvider cannot be null");
    }

    Type type = TypeUtils.getTypeArguments(classImplementingProvider, Provider.class).get(Provider.class.getTypeParameters()[0]);

    return TypeUtils.getRawType(type, null);
  }

  @Override
  public Object getInstance(Instantiator instantiator, NamedParameter... namedParameters) throws BeanResolutionException {
    try {
      return provider == null ? ((Provider<?>)instantiator.getParameterizedInstance(classImplementingProvider, namedParameters, getQualifiers().toArray())).get() : provider.get();
    }
    catch(Exception e) {
      throw new BeanResolutionException(getType(), e);
    }
  }

  /**
   * Returns the class which implements the Provider interface.
   *
   * @return the class which implements the Provider interface, never null
   */
  public Class<?> getClassImplementingProvider() {
    return classImplementingProvider;
  }

  @Override
  public int hashCode() {
    return Objects.hash(provider, getQualifiers(), classImplementingProvider);
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
        && Objects.equals(classImplementingProvider, other.classImplementingProvider);
  }

  @Override
  public String toString() {
    return "Injectable-Provider(" + getType() + " + " + getQualifiers() + ")";
  }
}
