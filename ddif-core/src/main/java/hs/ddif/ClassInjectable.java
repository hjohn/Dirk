package hs.ddif;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.inject.Provider;

public class ClassInjectable implements Injectable {
  private final Class<?> injectableClass;

  public ClassInjectable(Class<?> injectableClass) {
    this.injectableClass = injectableClass;
  }

  @Override
  public Class<?> getInjectableClass() {
    return injectableClass;
  }

  @Override
  public Object getInstance(Injector injector, Map<AccessibleObject, Binding> injections) {
    try {
      /*
       * Look for a constructor injection, and if found use that.  If not, use the default
       * constructor.
       */

      Object bean = null;

      for(AccessibleObject accessibleObject : injections.keySet()) {
        if(accessibleObject instanceof Constructor) {
          Constructor<?> constructor = (Constructor<?>)accessibleObject;

          bean = constructor.newInstance((Object[])injections.get(accessibleObject).getValue(injector));
        }
      }

      if(bean == null) {
        bean = getInjectableClass().newInstance();
      }

      /*
       * Do field/method injections.
       */

      for(Map.Entry<AccessibleObject, Binding> entry : injections.entrySet()) {
        try {
          AccessibleObject accessibleObject = entry.getKey();

          if(accessibleObject instanceof Field) {
            Field field = (Field)accessibleObject;

            field.setAccessible(true);
            field.set(bean, entry.getValue().getValue(injector));
          }
        }
        catch(IllegalArgumentException e) {
          throw new InjectionException("Incompatible types", e);
        }
        catch(IllegalAccessException e) {
          throw new InjectionException("Illegal access", e);
        }
      }

      return bean;
    }
    catch(IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Provider<Object> getProvider() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    return injectableClass.toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    return injectableClass.equals(((ClassInjectable)obj).injectableClass);
  }
}
