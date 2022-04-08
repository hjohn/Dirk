package hs.ddif.core.config;

import hs.ddif.core.config.standard.DiscoveryExtension;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.util.Fields;
import hs.ddif.core.util.Methods;
import hs.ddif.core.util.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Extension which looks for members annotated with a produces annotation, and if found creates
 * {@link Injectable}s for them.
 */
public class ProducesDiscoveryExtension implements DiscoveryExtension {
  private final Class<? extends Annotation> produces;

  /**
   * Constructs a new instance.
   *
   * @param produces an annotation {@link Class} marking producer fields and methods, cannot be {@code null}
   */
  public ProducesDiscoveryExtension(Class<? extends Annotation> produces) {
    this.produces = produces;
  }

  @Override
  public void deriveTypes(Registry registry, Type type) {
    Class<?> injectableClass = Types.raw(type);

    if(injectableClass != null) {
      for(Method method : Methods.findAnnotated(injectableClass, produces)) {
        registry.add(method, type);
      }

      for(Field field : Fields.findAnnotated(injectableClass, produces)) {
        registry.add(field, type);
      }
    }
  }
}
