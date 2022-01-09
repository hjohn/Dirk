package hs.ddif.core.config.standard;

import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.instantiation.InstanceCreationFailure;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.inject.instantiation.MultipleInstances;
import hs.ddif.core.inject.instantiation.NoSuchInstance;
import hs.ddif.core.inject.instantiation.ValueFactory;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.store.Key;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Default implementation {@link Binding}.
 */
public class DefaultBinding implements Binding {
  private final Key key;
  private final AccessibleObject accessibleObject;
  private final Parameter parameter;
  private final boolean isCollection;
  private final boolean isDirect;
  private final boolean isOptional;
  private final ValueFactory valueFactory;

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   * @param accessibleObject an {@link AccessibleObject}, can be {@code null}
   * @param parameter a {@link Parameter}, cannot be {@code null} for {@link java.lang.reflect.Executable}s and must be {@code null} otherwise
   * @param isCollection {@code true} if this binding represents a collection, otherwise {@code false}
   * @param isDirect {@code true} if this binding represents a dependency without indirection (not wrapped in a provider), otherwise {@code false}
   * @param isOptional {@code true} if this binding is optional, otherwise {@code false}
   * @param valueFactory a {@link ValueFactory}, cannot be null
   */
  public DefaultBinding(Key key, AccessibleObject accessibleObject, Parameter parameter, boolean isCollection, boolean isDirect, boolean isOptional, ValueFactory valueFactory) {
    if(key == null) {
      throw new IllegalArgumentException("key cannot be null");
    }
    if(accessibleObject instanceof Executable && parameter == null) {
      throw new IllegalArgumentException("parameter cannot be null when accessibleObject is an instance of Executable");
    }
    if(!(accessibleObject instanceof Executable) && parameter != null) {
      throw new IllegalArgumentException("parameter must be null when accessibleObject is not an instance of Executable");
    }
    if(valueFactory == null) {
      throw new IllegalArgumentException("valueFactory cannot be null");
    }

    this.key = key;
    this.accessibleObject = accessibleObject;
    this.parameter = parameter;
    this.isCollection = isCollection;
    this.isDirect = isDirect;
    this.isOptional = isOptional;
    this.valueFactory = valueFactory;
  }

  @Override
  public Key getKey() {
    return key;
  }

  @Override
  public AccessibleObject getAccessibleObject() {
    return accessibleObject;
  }

  @Override
  public Parameter getParameter() {
    return parameter;
  }

  @Override
  public boolean isCollection() {
    return isCollection;
  }

  @Override
  public boolean isDirect() {
    return isDirect;
  }

  @Override
  public boolean isOptional() {
    return isOptional;
  }

  @Override
  public Object getValue(Instantiator instantiator) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance, OutOfScopeException {
    return valueFactory.getValue(instantiator);
  }

  @Override
  public String toString() {
    if(accessibleObject instanceof Executable) {
      return "Parameter " + Arrays.asList(((Executable)accessibleObject).getParameters()).indexOf(parameter) + " of [" + accessibleObject + "]";
    }
    else if(accessibleObject != null) {
      return "Field [" + accessibleObject + "]";
    }

    return "Owner Type [" + key.getType() + "]";
  }
}
