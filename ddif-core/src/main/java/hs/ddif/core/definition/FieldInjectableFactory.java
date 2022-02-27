package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.instantiation.factory.FieldObjectFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeUtils;

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
   * @param field a {@link Field}, cannot be {@code null}
   * @param ownerType the type of the owner of the field, cannot be {@code null} and must match with {@link Field#getDeclaringClass()}
   * @return a new {@link Injectable}, never {@code null}
   */
  public Injectable create(Field field, Type ownerType) {
    if(field == null) {
      throw new IllegalArgumentException("field cannot be null");
    }
    if(ownerType == null) {
      throw new IllegalArgumentException("ownerType cannot be null");
    }

    Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(ownerType, field.getDeclaringClass());

    if(typeArguments == null) {
      throw new IllegalArgumentException("ownerType must be assignable to field's declaring class: " + ownerType + "; declaring class: " + field.getDeclaringClass());
    }

    Type type = TypeUtils.unrollVariables(typeArguments, field.getGenericType());

    if(type == null) {
      throw new DefinitionException(field, "is of unresolvable type");
    }

    return injectableFactory.create(type, field, bindingProvider.ofField(field, ownerType), new FieldObjectFactory(field));
  }
}
