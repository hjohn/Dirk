package org.int4.dirk.core.definition.factory;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.core.definition.injection.Constructable;
import org.int4.dirk.core.definition.injection.Injection;
import org.int4.dirk.core.util.Description;
import org.int4.dirk.spi.config.LifeCycleCallbacks;

/**
 * Object factory for concrete classes. This factory use the given {@link Constructor}
 * to construct the associated class, inject it using the given {@link Injection}s and
 * do life cycle callbacks using the provided {@link LifeCycleCallbacks} instance.
 *
 * @param <T> the type of the instances produced
 */
public class ClassObjectFactory<T> implements Constructable<T> {
  private static final ThreadLocal<Set<Object>> UNDER_CONSTRUCTION_THREAD_LOCAL = ThreadLocal.withInitial(() -> new HashSet<>());

  private final Constructor<T> constructor;
  private final LifeCycleCallbacks lifeCycleCallbacks;

  /**
   * Constructs a new instance.
   *
   * @param constructor a {@link Constructor} which produces the required class, cannot be {@code null}
   * @param lifeCycleCallbacks a {@link LifeCycleCallbacks} instance, cannot be {@code null}
   */
  public ClassObjectFactory(Constructor<T> constructor, LifeCycleCallbacks lifeCycleCallbacks) {
    this.constructor = Objects.requireNonNull(constructor, "constructor cannot be null");
    this.lifeCycleCallbacks = Objects.requireNonNull(lifeCycleCallbacks, "lifeCycleCallbacks cannot be null");

    constructor.setAccessible(true);
  }

  @Override
  public T create(List<Injection> injections) throws CreationException {
    if(UNDER_CONSTRUCTION_THREAD_LOCAL.get().contains(this)) {
      throw new CreationException("[" + constructor.getDeclaringClass() + "] already under construction (dependency creation loop in setter, initializer or post-construct method?)");
    }

    try {
      UNDER_CONSTRUCTION_THREAD_LOCAL.get().add(this);

      T instance = constructInstance(injections);

      injectInstance(instance, injections);

      try {
        lifeCycleCallbacks.postConstruct(instance);
      }
      catch(InvocationTargetException e) {
        throw new CreationException("[" + constructor.getDeclaringClass() + "] threw exception during post construction", e.getCause());
      }

      return instance;
    }
    finally {
      UNDER_CONSTRUCTION_THREAD_LOCAL.get().remove(this);
    }
  }

  @Override
  public void destroy(T instance) {
    lifeCycleCallbacks.preDestroy(instance);
  }

  @Override
  public boolean needsDestroy() {
    return lifeCycleCallbacks.needsDestroy();
  }

  private T constructInstance(List<Injection> injections) throws CreationException {
    try {
      Object[] values = new Object[constructor.getParameterCount()];  // Parameters for constructor
      int parameterIndex = 0;

      for(Injection injection : injections) {
        if(injection.getTarget() == constructor) {
          values[parameterIndex++] = injection.getValue();
        }
      }

      return constructor.newInstance(values);
    }
    catch(InvocationTargetException e) {
      throw new CreationException("[" + constructor + "] call failed", e.getCause());
    }
    catch(IllegalAccessException | InstantiationException e) {
      throw new IllegalStateException(constructor + " call failed", e);
    }
  }

  private static void injectInstance(Object instance, List<Injection> injections) throws CreationException {
    Object[] values = null;
    int parameterIndex = 0;

    for(Injection injection : injections) {
      AccessibleObject accessibleObject = injection.getTarget();

      if(accessibleObject instanceof Field) {
        Field field = (Field)accessibleObject;

        try {
          Object valueToSet = injection.getValue();

          if(valueToSet != null) {  // Do not set fields to null, leave default value instead
            field.set(instance, valueToSet);
          }
        }
        catch(Exception e) {
          throw new IllegalStateException(field + " inject failed", e);
        }
      }
      else if(accessibleObject instanceof Method) {
        Method method = (Method)accessibleObject;

        if(values == null) {
          values = new Object[method.getParameterCount()];
        }

        values[parameterIndex++] = injection.getValue();

        if(parameterIndex == method.getParameterCount()) {
          try {
            method.invoke(instance, values);
          }
          catch(InvocationTargetException e) {
            throw new CreationException(Description.of(method) + " inject failed", e.getCause());
          }
          catch(Exception e) {
            throw new IllegalStateException(method + " inject failed", e);
          }

          values = null;
          parameterIndex = 0;
        }
      }
    }
  }
}
