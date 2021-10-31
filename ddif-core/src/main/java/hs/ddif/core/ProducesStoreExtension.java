package hs.ddif.core;

import hs.ddif.annotations.Produces;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.FieldInjectable;
import hs.ddif.core.inject.store.MethodInjectable;
import hs.ddif.core.inject.store.ResolvableInjectableStore;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Extension which looks for members annotated with {@link Produces}, and if found creates
 * {@link ResolvableInjectable}s for them.
 */
public class ProducesStoreExtension implements ResolvableInjectableStore.Extension {

  @Override
  public List<Supplier<ResolvableInjectable>> getDerived(Resolver<ResolvableInjectable> resolver, ResolvableInjectable injectable) {
    List<Supplier<ResolvableInjectable>> suppliers = new ArrayList<>();
    Class<?> injectableClass = TypeUtils.getRawType(injectable.getType(), null);

    for(Method method : MethodUtils.getMethodsListWithAnnotation(injectableClass, Produces.class, true, true)) {
      suppliers.add(() -> new MethodInjectable(method, injectable.getType()));
    }

    for(Field field : FieldUtils.getFieldsListWithAnnotation(injectableClass, Produces.class)) {
      suppliers.add(() -> new FieldInjectable(field, injectable.getType()));
    }

    return suppliers;
  }
}
