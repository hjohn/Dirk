package hs.ddif.core.inject.store;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.Collections;
import java.util.Objects;

import javax.inject.Singleton;

public class InstanceInjectable extends AbstractResolvableInjectable {
  private static final Annotation SINGLETON_ANNOTATION = new Annotation() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Singleton.class;
    }
  };

  private final Object instance;

  public InstanceInjectable(Object instance, AnnotationDescriptor... descriptors) {
    super(Collections.<AccessibleObject, Binding[]>emptyMap(), SINGLETON_ANNOTATION, instance.getClass(), descriptors);

    this.instance = instance;
  }

  @Override
  public Object getInstance(Instantiator instantiator, NamedParameter... namedParameters) {
    return instance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(instance, getDescriptors());
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    InstanceInjectable other = (InstanceInjectable)obj;

    return instance.equals(other.instance)
        && getDescriptors().equals(other.getDescriptors());
  }

  @Override
  public String toString() {
    return "Injectable-Instance(" + instance.getClass() + " + " + getDescriptors() + ")";
  }
}
