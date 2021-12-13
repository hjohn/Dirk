package hs.ddif.core;

import hs.ddif.annotations.Produces;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.AutoDiscoveringGatherer;
import hs.ddif.core.inject.store.FieldInjectableFactory;
import hs.ddif.core.inject.store.MethodInjectableFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Extension which looks for members annotated with {@link Produces}, and if found creates
 * {@link ResolvableInjectable}s for them.
 */
public class ProducesGathererExtension implements AutoDiscoveringGatherer.Extension {
  private final MethodInjectableFactory methodInjectableFactory;
  private final FieldInjectableFactory fieldInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param methodInjectableFactory a {@link MethodInjectableFactory}, cannot be null
   * @param fieldInjectableFactory a {@link FieldInjectableFactory}, cannot be null
   */
  public ProducesGathererExtension(MethodInjectableFactory methodInjectableFactory, FieldInjectableFactory fieldInjectableFactory) {
    this.methodInjectableFactory = methodInjectableFactory;
    this.fieldInjectableFactory = fieldInjectableFactory;
  }

  @Override
  public List<ResolvableInjectable> getDerived(ResolvableInjectable injectable) {
    List<ResolvableInjectable> injectables = new ArrayList<>();
    Class<?> injectableClass = TypeUtils.getRawType(injectable.getType(), null);

    for(Method method : MethodUtils.getMethodsListWithAnnotation(injectableClass, Produces.class, true, true)) {
      injectables.add(methodInjectableFactory.create(method, injectable.getType()));
    }

    for(Field field : FieldUtils.getFieldsListWithAnnotation(injectableClass, Produces.class)) {
      injectables.add(fieldInjectableFactory.create(field, injectable.getType()));
    }

    return injectables;
  }
}
