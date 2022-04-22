package hs.ddif.core;

import hs.ddif.core.config.DirectTypeExtension;
import hs.ddif.core.instantiation.TypeExtensions;
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.instantiation.TypeExtension;

import java.util.Map;

public class TypeExtensionStores {

  public static TypeExtensionStore create(AnnotationStrategy annotationStrategy, Map<Class<?>, TypeExtension<?>> typeExtensions) {
    return new TypeExtensionStore(new DirectTypeExtension<>(annotationStrategy), typeExtensions);
  }

  public static TypeExtensionStore create(AnnotationStrategy annotationStrategy) {
    return create(annotationStrategy, TypeExtensions.create(annotationStrategy));
  }
}

