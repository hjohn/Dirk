package hs.ddif.library;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.spi.discovery.DiscoveryExtension;
import hs.ddif.util.Fields;
import hs.ddif.util.Methods;
import hs.ddif.util.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Extension which looks for members annotated with a produces annotation, and if found registers
 * these with the injector.
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
  public void deriveTypes(Registry registry, Type type) throws DefinitionException {
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
