package hs.ddif.core.inject.store;

import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ObjectFactory;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Constructs {@link ResolvableInjectable}s for {@link Field} values of a specific
 * owner {@link Type}.
 */
public class FieldInjectableFactory {

  /**
   * Creates a new {@link ResolvableInjectable}.
   *
   * @param field a {@link Field}, cannot be null
   * @param ownerType the type of the owner of the field, cannot be null and must match with {@link Field#getDeclaringClass()}
   * @return a new {@link ResolvableInjectable}, never null
   */
  public ResolvableInjectable create(Field field, Type ownerType) {
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
      throw new BindingException("Field has unresolved type: " + field);
    }
    if(TypeUtils.containsTypeVariables(type)) {
      throw new BindingException("Field has unresolved type variables: " + field);
    }
    if(field.isAnnotationPresent(Inject.class)) {
      throw new BindingException("Field cannot be annotated with Inject: " + field);
    }

    return new ResolvableInjectable(
      type,
      AnnotationExtractor.extractQualifiers(field),
      BindingProvider.ofField(field, ownerType),
      AnnotationExtractor.findScopeAnnotation(field),
      field,  // for proper discrimination, the exact field should also be taken into account, next to its generic type
      new FieldObjectFactory(field, ownerType)
    );
  }

  static class FieldObjectFactory implements ObjectFactory {
    private final Field field;
    private final Type ownerType;

    FieldObjectFactory(Field field, Type ownerType) {
      this.field = field;
      this.ownerType = ownerType;
    }

    @Override
    public Object createInstance(Instantiator instantiator, NamedParameter... parameters) throws InstanceCreationFailure {
      if(parameters.length > 0) {
        throw new InstanceCreationFailure(field, "Superflous parameters supplied, none expected for producer field but got: " + Arrays.toString(parameters));
      }

      return constructInstance(instantiator);
    }

    private Object constructInstance(Instantiator instantiator) throws InstanceCreationFailure {
      try {
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        Object obj = isStatic ? null : instantiator.getInstance(ownerType);

        field.setAccessible(true);

        return field.get(obj);
      }
      catch(Exception e) {
        throw new InstanceCreationFailure(field, "Exception while constructing instance via Producer", e);
      }
    }
  }
}
