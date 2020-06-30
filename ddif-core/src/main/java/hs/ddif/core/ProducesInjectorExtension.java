package hs.ddif.core;

import hs.ddif.annotations.Produces;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.core.inject.store.FieldInjectable;
import hs.ddif.core.inject.store.MethodInjectable;

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
public class ProducesInjectorExtension implements BeanDefinitionStore.Extension {

  @Override
  public List<ResolvableInjectable> getDerived(ResolvableInjectable injectable) {
    return createValidatedInjectables(TypeUtils.getRawType(injectable.getType(), null), injectable);
  }

  private static List<ResolvableInjectable> createValidatedInjectables(Class<?> injectableClass, ResolvableInjectable injectable) {
    List<ResolvableInjectable> injectables = new ArrayList<>();

    for(Method method : MethodUtils.getMethodsListWithAnnotation(injectableClass, Produces.class, true, true)) {
      injectables.add(new MethodInjectable(method, injectable.getType()));
    }

    for(Field field : FieldUtils.getFieldsListWithAnnotation(injectableClass, Produces.class)) {
      injectables.add(new FieldInjectable(field, injectable.getType()));
    }

    return injectables;
  }
}
