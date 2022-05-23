package hs.ddif.jakarta;

import hs.ddif.annotations.Argument;
import hs.ddif.annotations.Assisted;
import hs.ddif.extensions.assisted.AssistedAnnotationStrategy;
import hs.ddif.extensions.assisted.AssistedTypeRegistrationExtension;
import hs.ddif.extensions.assisted.ConfigurableAssistedAnnotationStrategy;
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.config.LifeCycleCallbacksFactory;
import hs.ddif.spi.discovery.TypeRegistrationExtension;
import hs.ddif.util.Annotations;

import java.lang.reflect.AnnotatedElement;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

class AssistedTypeRegistrationExtensionSupport {
  private static final Inject INJECT = Annotations.of(Inject.class);

  static TypeRegistrationExtension create(AnnotationStrategy annotationStrategy, LifeCycleCallbacksFactory lifeCycleCallbacksFactory) {
    AssistedAnnotationStrategy<?> ASSISTED_ANNOTATION_STRATEGY = new ConfigurableAssistedAnnotationStrategy<>(Assisted.class, Argument.class, AssistedTypeRegistrationExtensionSupport::extractArgumentName, INJECT, Provider.class, Provider::get);

    return new AssistedTypeRegistrationExtension(annotationStrategy, lifeCycleCallbacksFactory, ASSISTED_ANNOTATION_STRATEGY);
  }

  private static String extractArgumentName(AnnotatedElement element) {
    Argument annotation = element.getAnnotation(Argument.class);

    return annotation == null ? null : annotation.value();
  }
}
