package hs.ddif.core.instantiation.factory;

import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.instantiation.injection.InjectionContext;
import hs.ddif.core.instantiation.injection.ObjectFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * An {@link ObjectFactory} using a method call to obtain an instance.
 *
 * @param <T> the type of the instances produced
 */
public class MethodObjectFactory<T> implements ObjectFactory<T> {
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
  public T createInstance(InjectionContext injectionContext) throws InstanceCreationFailure {
    try {
      List<Injection> injections = injectionContext.getInjections();
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
    catch(Exception e) {
      throw new InstanceCreationFailure(method, "call failed", e);
    }
  }

  @Override
  public void destroyInstance(T instance, InjectionContext injectionContext) {
    // TODO Call a corresponding Disposer method belonging to this Producer
  }
}