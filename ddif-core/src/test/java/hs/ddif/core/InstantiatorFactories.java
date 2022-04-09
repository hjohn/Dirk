package hs.ddif.core;

import hs.ddif.api.annotation.AnnotationStrategy;
import hs.ddif.api.instantiation.InstantiatorFactory;
import hs.ddif.api.instantiation.TypeExtension;

import java.util.Map;

public class InstantiatorFactories {

  public static InstantiatorFactory create() {
    return create(InjectableFactories.ANNOTATION_STRATEGY, Map.of());
  }

  public static InstantiatorFactory create(AnnotationStrategy annotationStrategy, Map<Class<?>, TypeExtension<?>> typeExtensions) {
    return new DefaultInstantiatorFactory(TypeExtensionStores.create(annotationStrategy, typeExtensions));
  }
}
