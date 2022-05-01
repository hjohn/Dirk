package hs.ddif.core.definition.factory;

import hs.ddif.api.instantiation.CreationException;
import hs.ddif.core.definition.injection.Constructable;
import hs.ddif.core.definition.injection.Injection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * A {@link Constructable} using a method call to obtain an instance.
 *
 * @param <T> the type of the instances produced
 */
public class MethodObjectFactory<T> implements Constructable<T> {
  private final Method method;
  private final boolean isStatic;

  /**
   * Constructs a new instance.
   *
   * @param method a {@link Method}, cannot be {@code null}
   */
  public MethodObjectFactory(Method method) {
    this.method = method;
    this.isStatic = Modifier.isStatic(method.getModifiers());

    method.setAccessible(true);
  }

  @Override
  public T create(List<Injection> injections) throws CreationException {
    try {
      Object[] values = new Object[injections.size() - (isStatic ? 0 : 1)];  // Parameters for method
      Object instance = null;
      int parameterIndex = 0;

      for(Injection injection : injections) {
        if(injection.getTarget() instanceof Method) {
          values[parameterIndex++] = injection.getValue();
        }
        else {
          instance = injection.getValue();
        }
      }

      @SuppressWarnings("unchecked")
      T value = (T)method.invoke(instance, values);

      return value;
    }
    catch(InvocationTargetException e) {
      throw new CreationException(method, "call failed", e.getCause());
    }
    catch(Exception e) {
      throw new IllegalStateException(method + " call failed", e);
    }
  }

  @Override
  public void destroy(T instance) {
    // TODO Call a corresponding Disposer method belonging to this Producer
  }
}