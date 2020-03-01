package hs.ddif.core.inject.store;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractResolvableInjectable implements ResolvableInjectable {
  private final Map<AccessibleObject, Binding[]> bindings;
  private final Annotation scope;
  private final Type injectableType;
  private final Set<AnnotationDescriptor> descriptors;
  private final boolean isTemplate;

  protected AbstractResolvableInjectable(Map<AccessibleObject, Binding[]> bindings, Annotation scope, Type injectableType, boolean isTemplate, Set<AnnotationDescriptor> descriptors) {
    if(bindings == null) {
      throw new IllegalArgumentException("bindings cannot be null");
    }
    if(injectableType == null) {
      throw new IllegalArgumentException("injectableType cannot be null");
    }

    this.bindings = bindings;
    this.scope = scope;
    this.injectableType = injectableType;
    this.isTemplate = isTemplate;
    this.descriptors = descriptors;
  }

  protected AbstractResolvableInjectable(Map<AccessibleObject, Binding[]> bindings, Annotation scope, Class<?> injectableClass, boolean isTemplate, AnnotationDescriptor... descriptors) {
    this(bindings, scope, injectableClass, isTemplate, new HashSet<>(Arrays.asList(descriptors)));
  }

  @Override
  public final Map<AccessibleObject, Binding[]> getBindings() {
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

  @Override
  public boolean isTemplate() {
    return isTemplate;
  }
}
