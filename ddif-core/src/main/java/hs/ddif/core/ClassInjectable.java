package hs.ddif.core;

import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link ScopedInjectable} for creating instances based on a {@link Class}.
 */
public class ClassInjectable implements ScopedInjectable {
  private final Class<?> injectableClass;
  private final Annotation scopeAnnotation;
  private final Map<AccessibleObject, Binding[]> bindings;

  /**
   * Constructs a new instance.
   *
   * @param injectableClass the {@link Class}, cannot be null, cannot be an interface or be abstract
   * @throws BindingException if the given class is not annotated and has no public empty constructor or is incorrectly annotated
   */
  public ClassInjectable(Class<?> injectableClass) {
    if(injectableClass == null) {
      throw new IllegalArgumentException("injectableClass cannot be null");
    }
    if(injectableClass.isInterface() || Modifier.isAbstract(injectableClass.getModifiers())) {
      throw new IllegalArgumentException("injectableClass must be a concrete class: " + injectableClass);
    }

    this.injectableClass = injectableClass;
    this.scopeAnnotation = findScopeAnnotation(injectableClass);

    this.bindings = Binder.resolve(injectableClass);

    /*
     * Check bindings to see if this injectable can be instantiated and injected.
     */

    int constructorCount = 0;

    for(Map.Entry<AccessibleObject, Binding[]> entry : bindings.entrySet()) {
      if(entry.getKey() instanceof Constructor) {
        constructorCount++;
      }
      else if(entry.getKey() instanceof Field) {
        Field field = (Field)entry.getKey();

        if(Modifier.isFinal(field.getModifiers())) {
          throw new BindingException("Cannot inject final field: " + field + " in: " + injectableClass);
        }
      }
    }

    if(constructorCount < 1) {
      throw new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: " + injectableClass);
    }
    if(constructorCount > 1) {
      throw new BindingException("Multiple @Inject annotated constructors found, but only one allowed: " + injectableClass);
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

    throw new IllegalStateException("Bindings must always contain a constructor entry");
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
        values[i] = constructorEntry.getValue()[i].getValue(injector);
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
        catch(IllegalArgumentException | IllegalAccessException e) {
          throw new IllegalStateException("Unable to set field " + entry.getKey() + " of: " + injectableClass, e);
        }
      }

      return bean;
    }
    catch(IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new IllegalStateException("Unable to construct: " + injectableClass, e);
    }
  }

  @Override
  public Set<AnnotationDescriptor> getQualifiers() {
    return AnnotationDescriptor.extractQualifiers(injectableClass);
  }

  @Override
  public Annotation getScope() {
    return scopeAnnotation;
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

  private static Annotation findScopeAnnotation(Class<?> cls) {
    List<Annotation> matchingAnnotations = AnnotationUtils.findAnnotations(cls, javax.inject.Scope.class);

    if(matchingAnnotations.size() > 1) {
      throw new BindingException("Multiple scope annotations found, but only one allowed: " + cls + ", found: " + matchingAnnotations);
    }

    return matchingAnnotations.isEmpty() ? null : matchingAnnotations.get(0);
  }
}
