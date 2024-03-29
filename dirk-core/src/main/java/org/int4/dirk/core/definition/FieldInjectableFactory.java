package org.int4.dirk.core.definition;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.definition.factory.FieldObjectFactory;

/**
 * Constructs {@link Injectable}s for {@link Field} values of a specific
 * owner {@link Type}.
 */
public class FieldInjectableFactory {
  private final BindingProvider bindingProvider;
  private final InjectableFactory injectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param bindingProvider a {@link BindingProvider}, cannot be {@code null}
   * @param injectableFactory a {@link InjectableFactory}, cannot be {@code null}
   */
  public FieldInjectableFactory(BindingProvider bindingProvider, InjectableFactory injectableFactory) {
    this.bindingProvider = bindingProvider;
    this.injectableFactory = injectableFactory;
  }

  /**
   * Creates a new {@link Injectable}.
   *
   * @param <T> the type of the given field
   * @param field a {@link Field}, cannot be {@code null}
   * @param ownerType the type of the owner of the field, cannot be {@code null} and must match with {@link Field#getDeclaringClass()}
   * @return a new {@link Injectable}, never {@code null}
   * @throws DefinitionException when a definition problem was encountered
   */
  public <T> Injectable<T> create(Field field, Type ownerType) throws DefinitionException {
    if(field == null) {
      throw new IllegalArgumentException("field cannot be null");
    }
    if(ownerType == null) {
      throw new IllegalArgumentException("ownerType cannot be null");
    }

    return injectableFactory.create(ownerType, field, field, bindingProvider.ofField(field, ownerType), new FieldObjectFactory<>(field));
  }
}
