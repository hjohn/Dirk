package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.ObjectFactory;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * Creates {@link ResolvableInjectable}s.
 */
public interface ResolvableInjectableFactory {

  /**
   * Creates a new {@link ResolvableInjectable}.
   *
   * @param type a {@link Type}, cannot be null
   * @param qualifiers a set of {@link AnnotationDescriptor}s, cannot be null or contain nulls, but can be empty
   * @param bindings a list of {@link Binding}s, cannot be null or contain nulls, but can be empty
   * @param scope a scope {@link Annotation}, can be null
   * @param discriminator an object to serve as a discriminator for similar injectables, can be null
   * @param objectFactory an {@link ObjectFactory}, cannot be null
   * @return a {@link ResolvableInjectable}, never null
   */
  ResolvableInjectable create(Type type, Set<AnnotationDescriptor> qualifiers, List<Binding> bindings, Annotation scope, Object discriminator, ObjectFactory objectFactory);
}
