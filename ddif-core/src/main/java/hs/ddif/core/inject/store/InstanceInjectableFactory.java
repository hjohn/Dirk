package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

/**
 * Constructs {@link ResolvableInjectable}s for a given object instance.
 */
public class InstanceInjectableFactory {
  private static final Annotation SINGLETON = AnnotationUtils.of(Singleton.class);

  /**
   * Creates a new {@link ResolvableInjectable}.
   *
   * @param instance an instance, cannot be null
   * @param descriptors an array of descriptors
   * @return a new {@link ResolvableInjectable}, never null
   */
  public ResolvableInjectable create(Object instance, AnnotationDescriptor... descriptors) {
    if(instance == null) {
      throw new IllegalArgumentException("instance cannot be null");
    }

    return new ResolvableInjectable(
      instance.getClass(),
      Set.of(descriptors),
      List.of(),
      SINGLETON,
      instance,
      (instantiator, parameters) -> instance
    );
  }
}
