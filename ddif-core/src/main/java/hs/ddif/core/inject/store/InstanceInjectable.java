package hs.ddif.core.inject.store;

import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ResolvableBinding;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Singleton;

/**
 * An injectable for a predefined instance.
 */
public class InstanceInjectable implements ResolvableInjectable {
  private static final Annotation SINGLETON = AnnotationUtils.of(Singleton.class);

  private final Object instance;
  private final Set<AnnotationDescriptor> descriptors;

  /**
   * Constructs a new instance.
   *
   * @param instance an instance, cannot be null
   * @param descriptors an array of descriptors
   */
  public InstanceInjectable(Object instance, AnnotationDescriptor... descriptors) {
    if(instance == null) {
      throw new IllegalArgumentException("instance cannot be null");
    }

    this.instance = instance;
    this.descriptors = Set.of(descriptors);
  }

  @Override
  public Object getInstance(Instantiator instantiator, NamedParameter... namedParameters) {
    return instance;
  }

  @Override
  public List<ResolvableBinding> getBindings() {
    return List.of();
  }

  @Override
  public Annotation getScope() {
    return SINGLETON;
  }

  @Override
  public Type getType() {
    return instance.getClass();
  }

  @Override
  public Set<AnnotationDescriptor> getQualifiers() {
    return descriptors;
  }

  @Override
  public int hashCode() {
    return Objects.hash(instance, getQualifiers());
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
        && getQualifiers().equals(other.getQualifiers());
  }

  @Override
  public String toString() {
    return "Injectable-Instance(" + instance.getClass() + " + " + getQualifiers() + ")";
  }
}
