package hs.ddif.core;

import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;

public class ProvidedInjectable implements ScopedInjectable {
  private final Provider<?> provider;
  private final Class<?> classImplementingProvider;
  private final Class<?> providedClass;
  private final List<AnnotationDescriptor> descriptors;
  private final Type type;

  public ProvidedInjectable(Provider<?> provider, AnnotationDescriptor... descriptors) {
    this(provider.getClass(), provider, descriptors);
  }

  public ProvidedInjectable(Class<?> classImplementingProvider, AnnotationDescriptor... descriptors) {
    this(classImplementingProvider, null, descriptors);
  }

  private ProvidedInjectable(Class<?> classImplementingProvider, Provider<?> provider, AnnotationDescriptor... descriptors) {
    if(classImplementingProvider == null) {
      throw new IllegalArgumentException("classImplementingProvider cannot be null");
    }

    this.classImplementingProvider = classImplementingProvider;
    this.provider = provider;
    this.type = TypeUtils.getTypeArguments(classImplementingProvider, Provider.class).get(Provider.class.getTypeParameters()[0]);
    this.providedClass = TypeUtils.getRawType(type, null);
    this.descriptors = new ArrayList<>(Arrays.asList(descriptors));
  }

  @Override
  public Map<AccessibleObject, Binding[]> getBindings() {
    return Collections.emptyMap();
  }

  @Override
  public Class<?> getInjectableClass() {
    return providedClass;
  }

  @Override
  public Object getInstance(Injector injector) {
    try {
      return provider == null ? ((Provider<?>)injector.getInstance(classImplementingProvider)).get() : provider.get();
    }
    catch(Exception e) {
      throw new NoSuchBeanException(type, e);
    }
  }

  @Override
  public Set<AnnotationDescriptor> getQualifiers() {
    Set<AnnotationDescriptor> qualifiers = AnnotationDescriptor.extractQualifiers(providedClass);

    qualifiers.addAll(descriptors);

    return qualifiers;
  }

  @Override
  public Annotation getScope() {
    return null;
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
    return Objects.hash(providedClass, provider, descriptors, classImplementingProvider);
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

    return providedClass.equals(other.providedClass)
        && descriptors.equals(other.descriptors)
        && Objects.equals(provider, other.provider)
        && Objects.equals(classImplementingProvider, other.classImplementingProvider);
  }

  @Override
  public String toString() {
    return "Injectable-Provider(" + getInjectableClass() + " + " + descriptors + ")";
  }
}
