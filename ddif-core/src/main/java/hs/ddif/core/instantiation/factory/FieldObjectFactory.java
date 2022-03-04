package hs.ddif.core.instantiation.factory;

import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.instantiation.injection.InjectionContext;
import hs.ddif.core.instantiation.injection.ObjectFactory;

import java.lang.reflect.Field;
import java.util.List;

/**
 * An {@link ObjectFactory} which reads a field to obtain an instance.
 *
 * @param <T> the type of the instances produced
 */
public class FieldObjectFactory<T> implements ObjectFactory<T> {
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
  public T createInstance(InjectionContext injectionContext) throws InstanceCreationFailure {
    try {
      List<Injection> injections = injectionContext.getInjections();

      @SuppressWarnings("unchecked")
      T instance = (T)field.get(injections.isEmpty() ? null : injections.get(0).getValue());

      return instance;
    }
    catch(Exception e) {
      throw new InstanceCreationFailure(field, "read failed", e);
    }
  }

  @Override
  public void destroyInstance(T instance, InjectionContext injectionContext) {
    // TODO Call a corresponding Disposer method belonging to this Producer
  }
}