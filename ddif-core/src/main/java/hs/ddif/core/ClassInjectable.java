package hs.ddif.core;

import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.reflect.MethodUtils;

/**
 * A {@link ScopedInjectable} for creating instances based on a {@link Class}.
 */
public class ClassInjectable implements ScopedInjectable {
  private final Class<?> injectableClass;
  private final List<Method> postConstructMethods;
  private final Annotation scopeAnnotation;
  private final Map<AccessibleObject, Binding[]> bindings;

  /**
   * When true, the object is currently being constructed.  This is used to
   * detect loops in {@link PostConstruct} annotated methods, where another
   * depedency is created that depends on this object, which isn't fully
   * constructed yet (and thus not yet available as dependency, triggering
   * another creation).
   */
  private boolean underConstruction;

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

    List<Method> methods = MethodUtils.getMethodsListWithAnnotation(injectableClass, PostConstruct.class, true, true);

    Collections.sort(methods, new Comparator<Method>() {
      @Override
      public int compare(Method a, Method b) {
        if(a.getDeclaringClass().isAssignableFrom(b.getDeclaringClass())) {
          return -1;
        }
        else if(b.getDeclaringClass().isAssignableFrom(a.getDeclaringClass())) {
          return 1;
        }

        return 0;
      }
    });

    this.postConstructMethods = methods;
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
    if(underConstruction) {
      throw new ConstructionException("Object already under construction (dependency creation loop in @PostConstruct method!): " + injectableClass);
    }

    try {
      underConstruction = true;

      Object bean = constructInstance(injector);

      injectInstance(injector, bean);

      try {
        for(Method method : postConstructMethods) {
          method.setAccessible(true);
          method.invoke(bean);
        }
      }
      catch(Exception e) {
        throw new ConstructionException("PostConstruct call failed: " + injectableClass, e);
      }

      return bean;
    }
    finally {
      underConstruction = false;
    }
  }

  private void injectInstance(Injector injector, Object bean) {
    for(Map.Entry<AccessibleObject, Binding[]> entry : bindings.entrySet()) {
      try {
        AccessibleObject accessibleObject = entry.getKey();

        if(accessibleObject instanceof Field) {
          Field field = (Field)accessibleObject;
          Binding binding = entry.getValue()[0];
          Object valueToSet = binding.getValue(injector);

          if(valueToSet != null) {  // Donot set fields to null, leave default value instead
            field.setAccessible(true);
            field.set(bean, valueToSet);
          }
        }
      }
      catch(Exception e) {
        throw new ConstructionException("Unable to set field [" + entry.getKey() + "] of: " + injectableClass, e);
      }
    }
  }

  private Object constructInstance(Injector injector) {
    try {
      Map.Entry<AccessibleObject, Binding[]> constructorEntry = findConstructorEntry(bindings);
      Constructor<?> constructor = (Constructor<?>)constructorEntry.getKey();
      Object[] values = new Object[constructorEntry.getValue().length];  // Parameters for constructor

      for(int i = 0; i < values.length; i++) {
        values[i] = constructorEntry.getValue()[i].getValue(injector);
      }

      return constructor.newInstance(values);
    }
    catch(Exception e) {
      throw new ConstructionException("Unable to construct: " + injectableClass, e);
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
