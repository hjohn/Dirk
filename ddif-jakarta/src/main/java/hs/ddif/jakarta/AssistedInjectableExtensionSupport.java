package hs.ddif.jakarta;

import hs.ddif.annotations.Argument;
import hs.ddif.annotations.Assisted;
import hs.ddif.api.definition.DiscoveryExtension;
import hs.ddif.api.definition.LifeCycleCallbacksFactory;
import hs.ddif.api.util.Annotations;
import hs.ddif.core.definition.BindingProvider;
import hs.ddif.extensions.assisted.AssistedAnnotationStrategy;
import hs.ddif.extensions.assisted.AssistedDiscoveryExtension;
import hs.ddif.extensions.assisted.ConfigurableAssistedAnnotationStrategy;

import java.lang.reflect.AnnotatedElement;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

class AssistedInjectableExtensionSupport {
  private static final Inject INJECT = Annotations.of(Inject.class);

  static DiscoveryExtension create(BindingProvider bindingProvider, LifeCycleCallbacksFactory lifeCycleCallbacksFactory) {
    AssistedAnnotationStrategy<?> ASSISTED_ANNOTATION_STRATEGY = new ConfigurableAssistedAnnotationStrategy<>(Assisted.class, Argument.class, AssistedInjectableExtensionSupport::extractArgumentName, INJECT, Provider.class, Provider::get);

    return new AssistedDiscoveryExtension(bindingProvider, lifeCycleCallbacksFactory, ASSISTED_ANNOTATION_STRATEGY);
  }

  private static String extractArgumentName(AnnotatedElement element) {
    Argument annotation = element.getAnnotation(Argument.class);

    return annotation == null ? null : annotation.value();
  }
}
