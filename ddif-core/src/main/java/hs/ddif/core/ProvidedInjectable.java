package hs.ddif.core;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

public class ProvidedInjectable implements Injectable {
  private final Provider<?> provider;
  private final Class<?> injectableClass;
  private final List<AnnotationDescriptor> descriptors;

  public ProvidedInjectable(Provider<?> provider, AnnotationDescriptor... descriptors) {
    if(provider == null) {
      throw new IllegalArgumentException("provider cannot be null");
    }

    Type type = provider.getClass().getGenericInterfaces()[0];

    this.provider = provider;
    this.injectableClass = Binder.determineClassFromType(Binder.getGenericType(type));
    this.descriptors = new ArrayList<>(Arrays.asList(descriptors));
  }

  @Override
  public boolean needsInjection() {
    return false;
  }

  @Override
  public Class<?> getInjectableClass() {
    return injectableClass;
  }

  @Override
  public Object getInstance(Injector injector, Map<AccessibleObject, Binding> bindings) {
    return provider.get();
  }

  @Override
  public Set<AnnotationDescriptor> getQualifiers() {
    return AnnotationDescriptor.extractQualifiers(injectableClass);
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
