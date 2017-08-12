package hs.ddif.core;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

public class ClassInjectable implements Injectable {
  private final Class<?> injectableClass;
  private final Map<AccessibleObject, Binding[]> bindings;

  public ClassInjectable(Class<?> injectableClass) {
    if(injectableClass.isInterface() || Modifier.isAbstract(injectableClass.getModifiers())) {
      throw new IllegalArgumentException("parameter 'injectableClass' must be a concrete class: " + injectableClass);
    }

    this.injectableClass = injectableClass;
    this.bindings = Binder.resolve(injectableClass);

    /*
     * Check bindings to see if this injectable can be instantiated and injected.
     */

    int constructorCount = 0;

    for(Map.Entry<AccessibleObject, Binding[]> entry : bindings.entrySet()) {
      if(entry.getKey() instanceof Constructor) {
        constructorCount++;
      }
      if(entry.getKey() instanceof Field) {
        Field field = (Field)entry.getKey();

        if(Modifier.isFinal(field.getModifiers())) {
          throw new BindingException("Cannot inject final field: " + field + " in: " + injectableClass);
        }
      }
    }

    if(constructorCount < 1) {
      throw new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: " + injectableClass);
    }
    else if(constructorCount > 1) {
      throw new BindingException("Multiple constructors found to be annotated with @Inject, but only one allowed: " + injectableClass);
    }
  }

  @Override
  public Class<?> getInjectableClass() {
    return injectableClass;
  }

  @Override
  public Map<AccessibleObject, Binding[]> getBindings() {
    return bindings;
  }

  private static Map.Entry<AccessibleObject, Binding[]> findConstructorEntry(Map<AccessibleObject, Binding[]> bindings) {
    for(Map.Entry<AccessibleObject, Binding[]> entry : bindings.entrySet()) {
      if(entry.getKey() instanceof Constructor) {
        return entry;
      }
    }

    return null;
  }

  @Override
  public Object getInstance(Injector injector) {
    try {

      /*
       * Look for a constructor injection to create the object, and instantiate it.
       */

      Map.Entry<AccessibleObject, Binding[]> constructorEntry = findConstructorEntry(bindings);
      Constructor<?> constructor = (Constructor<?>)constructorEntry.getKey();
      Object[] values = new Object[constructorEntry.getValue().length];  // Parameters for constructor

      for(int i = 0; i < values.length; i++) {
        Binding binding = constructorEntry.getValue()[i];

        try {
          values[i] = binding.getValue(injector);
        }
        catch(NoSuchBeanException e) {
          if(!binding.isOptional()) {
            throw e;
          }

          values[i] = null;
        }
      }

      Object bean = constructor.newInstance(values);

      /*
       * Do field/method injections.
       */

      for(Map.Entry<AccessibleObject, Binding[]> entry : bindings.entrySet()) {
        try {
          AccessibleObject accessibleObject = entry.getKey();

          if(accessibleObject instanceof Field) {
            Field field = (Field)accessibleObject;

            field.setAccessible(true);
            field.set(bean, entry.getValue()[0].getValue(injector));
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
  public Set<AnnotationDescriptor> getQualifiers() {
    return AnnotationDescriptor.extractQualifiers(injectableClass);
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
    return "Injectable-Class(" + getInjectableClass() + ")";
  }
}
