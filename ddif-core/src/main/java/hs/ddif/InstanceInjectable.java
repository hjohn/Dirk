package hs.ddif;

import java.lang.reflect.AccessibleObject;
import java.util.Map;

public class InstanceInjectable implements Injectable {
  private final Object instance;

  public InstanceInjectable(Object instance) {
    this.instance = instance;
  }

  @Override
  public boolean needsInjection() {
    return false;
  }

  @Override
  public Class<?> getInjectableClass() {
    return instance.getClass();
  }

  @Override
  public Object getInstance(Injector injector, Map<AccessibleObject, Binding> bindings) {
    return instance;
  }

  @Override
  public int hashCode() {
    return instance.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    return instance.equals(((InstanceInjectable)obj).instance);
  }
}
