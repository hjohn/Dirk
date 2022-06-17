package org.int4.dirk.core.definition.factory;

import java.lang.reflect.Field;
import java.util.List;

import org.int4.dirk.core.definition.injection.Constructable;
import org.int4.dirk.core.definition.injection.Injection;

/**
 * A {@link Constructable} which reads a field to obtain an instance.
 *
 * @param <T> the type of the instances produced
 */
public class FieldObjectFactory<T> implements Constructable<T> {
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
  public T create(List<Injection> injections) {
    try {
      @SuppressWarnings("unchecked")
      T instance = (T)field.get(injections.isEmpty() ? null : injections.get(0).getValue());

      return instance;
    }
    catch(Exception e) {
      throw new IllegalStateException(field + " read failed", e);
    }
  }

  @Override
  public void destroy(T instance) {
    // TODO Call a corresponding Disposer method belonging to this Producer
  }

  @Override
  public boolean needsDestroy() {
    return false;
  }
}