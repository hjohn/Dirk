package hs.ddif.core.instantiation;

import hs.ddif.core.config.DirectTypeExtension;
import hs.ddif.core.definition.bind.AnnotationStrategy;

import java.util.Map;

public class TypeExtensionStores {

  public static TypeExtensionStore create(AnnotationStrategy annotationStrategy, Map<Class<?>, TypeExtension<?>> typeExtensions) {
    return new TypeExtensionStore(new DirectTypeExtension<>(annotationStrategy), typeExtensions);
  }

  public static TypeExtensionStore create(AnnotationStrategy annotationStrategy) {
    return create(annotationStrategy, TypeExtensions.create(annotationStrategy));
  }
}

