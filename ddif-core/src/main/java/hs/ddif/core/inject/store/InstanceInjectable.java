package hs.ddif.core.inject.store;

import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.util.AnnotationDescriptor;

import java.util.List;
import java.util.Objects;

/**
 * An injectable for a predefined instance.
 */
public class InstanceInjectable extends AbstractResolvableInjectable {
  private final Object instance;

  /**
   * Constructs a new instance.
   *
   * @param instance an instance, cannot be null
   * @param descriptors an array of descriptors
   */
  public InstanceInjectable(Object instance, AnnotationDescriptor... descriptors) {
    super(List.of(), null, instance.getClass(), false, descriptors);

    this.instance = instance;
  }

  @Override
  public Object getInstance(Instantiator instantiator, NamedParameter... namedParameters) {
    return instance;
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
