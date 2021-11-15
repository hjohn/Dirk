package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.ResolvableBinding;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractResolvableInjectable implements ResolvableInjectable {
  private final List<ResolvableBinding> bindings;
  private final Annotation scope;
  private final Type injectableType;
  private final Set<AnnotationDescriptor> descriptors;

  protected AbstractResolvableInjectable(List<ResolvableBinding> bindings, Annotation scope, Type injectableType, Set<AnnotationDescriptor> descriptors) {
    if(bindings == null) {
      throw new IllegalArgumentException("bindings cannot be null");
    }
    if(injectableType == null) {
      throw new IllegalArgumentException("injectableType cannot be null");
    }

    this.bindings = bindings;
    this.scope = scope;
    this.injectableType = injectableType;
    this.descriptors = descriptors;
  }

  protected AbstractResolvableInjectable(List<ResolvableBinding> bindings, Annotation scope, Type injectableType, AnnotationDescriptor... descriptors) {
    this(bindings, scope, injectableType, new HashSet<>(Arrays.asList(descriptors)));
  }

  @Override
  public final List<ResolvableBinding> getBindings() {
    return bindings;
  }

  @Override
  public final Annotation getScope() {
    return scope;
  }

  @Override
  public final Type getType() {
    return injectableType;
  }

  @Override
  public final Set<AnnotationDescriptor> getQualifiers() {
    return descriptors;
  }
}
