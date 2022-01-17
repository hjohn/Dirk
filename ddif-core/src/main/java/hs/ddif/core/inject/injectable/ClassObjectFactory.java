package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.injection.Injection;
import hs.ddif.core.inject.injection.ObjectFactory;
import hs.ddif.core.inject.instantiation.InstanceCreationFailure;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object factory for concrete classes. This factory use the given {@link Constructor}
 * to construct the associated class, inject it using the given {@link Injection}s and
 * do post construct calls.
 */
public class ClassObjectFactory implements ObjectFactory {
  private static final Set<Object> UNDER_CONSTRUCTION = ConcurrentHashMap.newKeySet();

  private final Constructor<?> constructor;
  private final PostConstructor postConstructor;

  /**
   * Constructs a new instance.
   *
   * @param constructor a {@link Constructor} which produces the required class, cannot be {@code null}
   */
  public ClassObjectFactory(Constructor<?> constructor) {
    this.constructor = constructor;
    this.postConstructor = new PostConstructor(constructor.getDeclaringClass());

    constructor.setAccessible(true);
  }

  @Override
  public Object createInstance(List<Injection> injections) throws InstanceCreationFailure {
    if(UNDER_CONSTRUCTION.contains(this)) {
      throw new InstanceCreationFailure(constructor.getDeclaringClass(), "already under construction (dependency creation loop in @PostConstruct method!)");
    }

    try {
      UNDER_CONSTRUCTION.add(this);

      Object instance = constructInstance(injections);

      injectInstance(instance, injections);

      postConstructor.call(instance);

      return instance;
    }
    finally {
      UNDER_CONSTRUCTION.remove(this);
    }
  }

  private Object constructInstance(List<Injection> injections) throws InstanceCreationFailure {
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
    catch(Exception e) {
      throw new InstanceCreationFailure(constructor, "call failed", e);
    }
  }

  private static void injectInstance(Object instance, List<Injection> injections) throws InstanceCreationFailure {
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
          throw new InstanceCreationFailure(field, "inject failed", e);
        }
      }
    }
  }
}
