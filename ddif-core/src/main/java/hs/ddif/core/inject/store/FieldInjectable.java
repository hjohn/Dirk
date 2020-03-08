package hs.ddif.core.inject.store;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * A {@link ResolvableInjectable} for creating instances based on a {@link Field}.
 */
public class FieldInjectable implements ResolvableInjectable {
  private final Field field;
  private final Type ownerType;
  private final Type injectableType;
  private final Set<AnnotationDescriptor> qualifiers;
  private final Annotation scopeAnnotation;

  /**
   * Creates a new {@link FieldInjectable} from the given {@link Field}.
   *
   * @param field a {@link Field}, cannot be null
   * @param ownerType the type of the owner of the field, cannot be null and must match with {@link Field#getDeclaringClass()}
   */
  public FieldInjectable(Field field, Type ownerType) {
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

    this.field = field;
    this.ownerType = ownerType;
    this.injectableType = type;
    this.qualifiers = AnnotationExtractor.extractQualifiers(field);
    this.scopeAnnotation = AnnotationExtractor.findScopeAnnotation(field);
  }

  @Override
  public Object getInstance(Instantiator instantiator, NamedParameter... parameters) {
    if(parameters.length > 0) {
      throw new ConstructionException("Superflous parameters supplied, none expected for producer field but got: " + Arrays.toString(parameters));
    }

    return constructInstance(instantiator);
  }

  private Object constructInstance(Instantiator instantiator) {
    try {
      Object obj = instantiator.getInstance(ownerType);

      field.setAccessible(true);

      return field.get(obj);
    }
    catch(Exception e) {
      throw new ConstructionException("Unable to construct: " + injectableType, e);
    }
  }

  @Override
  public Map<AccessibleObject, List<Binding>> getBindings() {
    return Collections.emptyMap();
  }

  @Override
  public Annotation getScope() {
    return scopeAnnotation;
  }

  @Override
  public Type getType() {
    return injectableType;
  }

  @Override
  public Set<AnnotationDescriptor> getQualifiers() {
    return qualifiers;
  }

  @Override
  public boolean isTemplate() {
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(injectableType, qualifiers, ownerType);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    FieldInjectable other = (FieldInjectable)obj;

    return injectableType.equals(other.injectableType)
        && qualifiers.equals(other.qualifiers)
        && ownerType.equals(other.ownerType);
  }

  @Override
  public String toString() {
    return "Injectable-Field(" + injectableType + ")";
  }
}
