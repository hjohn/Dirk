package hs.ddif.core.config;

import hs.ddif.annotations.Produces;
import hs.ddif.core.config.standard.InjectableExtension;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.util.Fields;
import hs.ddif.core.util.Methods;
import hs.ddif.core.util.Types;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension which looks for members annotated with {@link Produces}, and if found creates
 * {@link Injectable}s for them.
 */
public class ProducesInjectableExtension implements InjectableExtension {
  private final MethodInjectableFactory methodInjectableFactory;
  private final FieldInjectableFactory fieldInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param methodInjectableFactory a {@link MethodInjectableFactory}, cannot be {@code null}
   * @param fieldInjectableFactory a {@link FieldInjectableFactory}, cannot be {@code null}
   */
  public ProducesInjectableExtension(MethodInjectableFactory methodInjectableFactory, FieldInjectableFactory fieldInjectableFactory) {
    this.methodInjectableFactory = methodInjectableFactory;
    this.fieldInjectableFactory = fieldInjectableFactory;
  }

  @Override
  public List<Injectable<?>> getDerived(Type type) {
    List<Injectable<?>> injectables = new ArrayList<>();
    Class<?> injectableClass = Types.raw(type);

    if(injectableClass != null) {
      for(Method method : Methods.findAnnotated(injectableClass, Produces.class)) {
        injectables.add(methodInjectableFactory.create(method, type));
      }

      for(Field field : Fields.findAnnotated(injectableClass, Produces.class)) {
        injectables.add(fieldInjectableFactory.create(field, type));
      }
    }

    return injectables;
  }
}
