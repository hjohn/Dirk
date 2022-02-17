package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.instantiation.factory.FieldObjectFactory;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Constructs {@link Injectable}s for {@link Field} values of a specific
 * owner {@link Type}.
 */
public class FieldInjectableFactory {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);

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
    if(field.isAnnotationPresent(Inject.class)) {
      throw new DefinitionException(field, "cannot be annotated with Inject");
    }

    try {
      return injectableFactory.create(
        type,
        Annotations.findDirectlyMetaAnnotatedAnnotations(field, QUALIFIER),
        bindingProvider.ofField(field, ownerType),
        ScopeAnnotations.find(field),
        field,  // for proper discrimination, the exact field should also be taken into account, next to its generic type
        new FieldObjectFactory(field)
      );
    }
    catch(UninjectableTypeException e) {
      throw new DefinitionException(field, "has unsuitable type", e);
    }
  }
}
