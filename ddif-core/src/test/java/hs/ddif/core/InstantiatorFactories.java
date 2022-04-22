package hs.ddif.core;

import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.TypeExtension;

import java.util.Map;

public class InstantiatorFactories {

  public static InstantiatorFactory create() {
    return create(InjectableFactories.ANNOTATION_STRATEGY, Map.of());
  }

  public static InstantiatorFactory create(AnnotationStrategy annotationStrategy, Map<Class<?>, TypeExtension<?>> typeExtensions) {
    return new DefaultInstantiatorFactory(TypeExtensionStores.create(annotationStrategy, typeExtensions));
  }
}
