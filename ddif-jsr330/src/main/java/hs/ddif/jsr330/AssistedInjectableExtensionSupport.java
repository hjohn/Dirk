package hs.ddif.jsr330;

import hs.ddif.annotations.Argument;
import hs.ddif.annotations.Assisted;
import hs.ddif.core.config.standard.InjectableExtension;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.util.Annotations;
import hs.ddif.extensions.assisted.AssistedAnnotationStrategy;
import hs.ddif.extensions.assisted.AssistedInjectableExtension;
import hs.ddif.extensions.assisted.ConfigurableAssistedAnnotationStrategy;

import java.lang.reflect.AnnotatedElement;

import javax.inject.Inject;
import javax.inject.Provider;

class AssistedInjectableExtensionSupport {
  private static final Inject INJECT = Annotations.of(Inject.class);

  static InjectableExtension create(ClassInjectableFactory classInjectableFactory) {
    AssistedAnnotationStrategy<?> ASSISTED_ANNOTATION_STRATEGY = new ConfigurableAssistedAnnotationStrategy<>(Assisted.class, Argument.class, AssistedInjectableExtensionSupport::extractArgumentName, INJECT, Provider.class, Provider::get);

    return new AssistedInjectableExtension(classInjectableFactory, ASSISTED_ANNOTATION_STRATEGY);
  }

  private static String extractArgumentName(AnnotatedElement element) {
    Argument annotation = element.getAnnotation(Argument.class);

    return annotation == null ? null : annotation.value();
  }
}
