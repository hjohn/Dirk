package hs.ddif.core.instantiation.factory;

import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.instantiation.injection.ObjectFactory;

import java.lang.reflect.Field;
import java.util.List;

/**
 * An {@link ObjectFactory} which reads a field to obtain an instance.
 */
public class FieldObjectFactory implements ObjectFactory {
  private final Field field;

  /**
   * Constructs a new instance.
   *
   * @param field a {@link Field}, cannot be {@code null}
   */
  public FieldObjectFactory(Field field) {
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