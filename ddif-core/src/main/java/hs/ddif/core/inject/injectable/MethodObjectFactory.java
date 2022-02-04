package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.injection.Injection;
import hs.ddif.core.inject.injection.ObjectFactory;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * An {@link ObjectFactory} using a method call to obtain an instance.
 */
public class MethodObjectFactory implements ObjectFactory {
  private final Method method;
  private final boolean isStatic;

  MethodObjectFactory(Method method) {
    this.method = method;
    this.isStatic = Modifier.isStatic(method.getModifiers());

    method.setAccessible(true);
  }

  @Override
  public Object createInstance(List<Injection> injections) throws InstanceCreationFailure {
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

      return method.invoke(instance, values);
    }
    catch(Exception e) {
      throw new InstanceCreationFailure(method, "call failed", e);
    }
  }
}