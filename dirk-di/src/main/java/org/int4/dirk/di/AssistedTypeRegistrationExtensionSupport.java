package org.int4.dirk.di;

import java.lang.reflect.AnnotatedElement;

import org.int4.dirk.annotations.Argument;
import org.int4.dirk.annotations.Assisted;
import org.int4.dirk.extensions.assisted.AssistedAnnotationStrategy;
import org.int4.dirk.extensions.assisted.AssistedTypeRegistrationExtension;
import org.int4.dirk.extensions.assisted.ConfigurableAssistedAnnotationStrategy;
import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.spi.config.LifeCycleCallbacksFactory;
import org.int4.dirk.spi.definition.TypeRegistrationExtension;
import org.int4.dirk.util.Annotations;

import jakarta.inject.Inject;

class AssistedTypeRegistrationExtensionSupport {
  private static final Inject INJECT = Annotations.of(Inject.class);

  static TypeRegistrationExtension create(AnnotationStrategy annotationStrategy, LifeCycleCallbacksFactory lifeCycleCallbacksFactory) {
    AssistedAnnotationStrategy ASSISTED_ANNOTATION_STRATEGY = new ConfigurableAssistedAnnotationStrategy<>(Assisted.class, Argument.class, AssistedTypeRegistrationExtensionSupport::extractArgumentName, INJECT);

    return new AssistedTypeRegistrationExtension(annotationStrategy, lifeCycleCallbacksFactory, ASSISTED_ANNOTATION_STRATEGY);
  }

  private static String extractArgumentName(AnnotatedElement element) {
    Argument annotation = element.getAnnotation(Argument.class);

    return annotation == null ? null : annotation.value();
  }
}
