package hs.ddif.core;

import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstanceInjectable implements Injectable {
  private final Object instance;
  private final List<AnnotationDescriptor> descriptors;

  public InstanceInjectable(Object instance, AnnotationDescriptor... descriptors) {
    if(instance == null) {
      throw new IllegalArgumentException("instance cannot be null");
    }

    this.instance = instance;
    this.descriptors = new ArrayList<>(Arrays.asList(descriptors));
  }

  @Override
  public Map<AccessibleObject, Binding> getBindings() {
    return Collections.emptyMap();
  }

  @Override
  public Class<?> getInjectableClass() {
    return instance.getClass();
  }

  @Override
  public Object getInstance(Injector injector) {
    return instance;
  }

  @Override
  public Set<AnnotationDescriptor> getQualifiers() {
    Set<AnnotationDescriptor> qualifiers = AnnotationDescriptor.extractQualifiers(instance.getClass());

    qualifiers.addAll(descriptors);

    return qualifiers;
  }

  @Override
  public int hashCode() {
    return instance.hashCode() ^ descriptors.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    return instance.equals(((InstanceInjectable)obj).instance) && descriptors.equals(((InstanceInjectable)obj).descriptors);
  }

  @Override
  public String toString() {
    return "Injectable-Instance(" + instance.getClass() + " + " + descriptors + ")";
  }
}
