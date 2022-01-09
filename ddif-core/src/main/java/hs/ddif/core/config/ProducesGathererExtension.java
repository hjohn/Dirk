package hs.ddif.core.config;

import hs.ddif.annotations.Produces;
import hs.ddif.core.config.standard.AutoDiscoveringGatherer;
import hs.ddif.core.inject.bind.BindingException;
import hs.ddif.core.inject.injectable.FieldInjectableFactory;
import hs.ddif.core.inject.injectable.MethodInjectableFactory;
import hs.ddif.core.inject.injectable.Injectable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Extension which looks for members annotated with {@link Produces}, and if found creates
 * {@link Injectable}s for them.
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
  public List<Injectable> getDerived(Injectable injectable) {
    List<Injectable> injectables = new ArrayList<>();
    Class<?> injectableClass = TypeUtils.getRawType(injectable.getType(), null);

    for(Method method : MethodUtils.getMethodsListWithAnnotation(injectableClass, Produces.class, true, true)) {
      Type providedType = method.getGenericReturnType();

      if(TypeUtils.getRawType(providedType, null) == Provider.class) {
        throw new BindingException("Nested Provider not allowed in: " + method);
      }

      injectables.add(methodInjectableFactory.create(method, injectable.getType()));
    }

    for(Field field : FieldUtils.getFieldsListWithAnnotation(injectableClass, Produces.class)) {
      Type providedType = field.getGenericType();

      if(TypeUtils.getRawType(providedType, null) == Provider.class) {
        throw new BindingException("Nested Provider not allowed in: " + field);
      }

      injectables.add(fieldInjectableFactory.create(field, injectable.getType()));
    }

    return injectables;
  }
}
