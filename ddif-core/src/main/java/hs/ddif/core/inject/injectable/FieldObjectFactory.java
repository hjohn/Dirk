package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.injection.Injection;
import hs.ddif.core.inject.injection.ObjectFactory;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;

import java.lang.reflect.Field;
import java.util.List;

/**
 * An {@link ObjectFactory} which reads a field to obtain an instance.
 */
public class FieldObjectFactory implements ObjectFactory {
  private final Field field;

  FieldObjectFactory(Field field) {
    this.field = field;

    field.setAccessible(true);
  }

  @Override
  public Object createInstance(List<Injection> injections) throws InstanceCreationFailure {
    try {
      return field.get(injections.isEmpty() ? null : injections.get(0).getValue());
    }
    catch(Exception e) {
      throw new InstanceCreationFailure(field, "read failed", e);
    }
  }
}