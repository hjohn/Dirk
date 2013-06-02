package hs.ddif;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class ClassInjectable implements Injectable {
  private final Class<?> injectableClass;

  public ClassInjectable(Class<?> injectableClass) {
    if(injectableClass.isInterface()) {
      throw new IllegalArgumentException("parameter 'injectableClass' must be a concrete class: " + injectableClass);
    }

    this.injectableClass = injectableClass;
  }

  @Override
  public Class<?> getInjectableClass() {
    return injectableClass;
  }

  @Override
  public boolean canBeInstantiated(Map<AccessibleObject, Binding> bindings) {
    return findConstructorEntry(bindings) != null;
  }

  private static Map.Entry<AccessibleObject, Binding> findConstructorEntry(Map<AccessibleObject, Binding> bindings) {
    for(Map.Entry<AccessibleObject, Binding> entry : bindings.entrySet()) {
      if(entry.getKey() instanceof Constructor) {
        return entry;
      }
    }

    return null;
  }

  @Override
  public Object getInstance(Injector injector, Map<AccessibleObject, Binding> bindings) {
    try {

      /*
       * Look for a constructor injection to create the object, and instantiate it.
       */

      Map.Entry<AccessibleObject, Binding> constructorEntry = findConstructorEntry(bindings);
      Constructor<?> constructor = (Constructor<?>)constructorEntry.getKey();
      Object bean = constructor.newInstance((Object[])constructorEntry.getValue().getValue(injector));

      /*
       * Do field/method injections.
       */

      for(Map.Entry<AccessibleObject, Binding> entry : bindings.entrySet()) {
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

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + injectableClass + "]";
  }
}
