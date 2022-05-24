package org.int4.dirk.library;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.spi.discovery.TypeRegistrationExtension;
import org.int4.dirk.util.Fields;
import org.int4.dirk.util.Methods;
import org.int4.dirk.util.Types;

/**
 * Extension which looks for members annotated with a produces annotation, and if found registers
 * these with the injector.
 */
public class ProducesTypeRegistrationExtension implements TypeRegistrationExtension {
  private final Class<? extends Annotation> produces;

  /**
   * Constructs a new instance.
   *
   * @param produces an annotation {@link Class} marking producer fields and methods, cannot be {@code null}
   */
  public ProducesTypeRegistrationExtension(Class<? extends Annotation> produces) {
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
