package hs.ddif.core;

import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

public class ProvidedInjectable implements ScopedInjectable {
  private final Provider<?> provider;
  private final Class<?> injectableClass;
  private final List<AnnotationDescriptor> descriptors;
  private final Type type;

  public ProvidedInjectable(Provider<?> provider, AnnotationDescriptor... descriptors) {
    if(provider == null) {
      throw new IllegalArgumentException("provider cannot be null");
    }

    this.type = provider.getClass().getGenericInterfaces()[0];
    this.provider = provider;
    this.injectableClass = TypeUtils.determineClassFromType(TypeUtils.getGenericType(type));
    this.descriptors = new ArrayList<>(Arrays.asList(descriptors));
  }

  @Override
  public Map<AccessibleObject, Binding[]> getBindings() {
    return Collections.emptyMap();
  }

  @Override
  public Class<?> getInjectableClass() {
    return injectableClass;
  }

  @Override
  public Object getInstance(Injector injector) {
    try {
      return provider.get();
    }
    catch(Exception e) {
      throw new NoSuchBeanException(type, e);
    }
  }

  @Override
  public Set<AnnotationDescriptor> getQualifiers() {
    return AnnotationDescriptor.extractQualifiers(injectableClass);
  }

  @Override
  public Annotation getScope() {
    return null;
  }

  @Override
  public int hashCode() {
    return injectableClass.toString().hashCode() ^ provider.hashCode() ^ descriptors.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    return injectableClass.equals(((ProvidedInjectable)obj).injectableClass)
        && provider.equals(((ProvidedInjectable)obj).provider)
        && descriptors.equals(((ProvidedInjectable)obj).descriptors);
  }

  @Override
  public String toString() {
    return "Injectable-Provider(" + getInjectableClass() + " + " + descriptors + ")";
  }
}
