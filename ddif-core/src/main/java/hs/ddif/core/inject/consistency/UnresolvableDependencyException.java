package hs.ddif.core.inject.consistency;

import hs.ddif.core.bind.Key;
import hs.ddif.core.store.Injectable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Thrown when not all dependencies of a class can be resolved.  This occurs when
 * a class requires a specific dependency but no such dependency is available or
 * more than one matching dependency is available.
 */
public class UnresolvableDependencyException extends InjectorStoreConsistencyException {

  public UnresolvableDependencyException(Injectable injectable, AccessibleObject accessibleObject, Key key, Set<? extends Injectable> candidates) {
    super(
      (candidates.isEmpty() ? "Missing" : "Multiple candidates for") + " dependency of type: " + key
        + " required for: " + formatInjectionPoint(injectable.getInjectableClass(), accessibleObject)
        + (candidates.isEmpty() ? "" : ": " + candidates)
    );
  }

  private static String formatInjectionPoint(Class<?> concreteClass, AccessibleObject accessibleObject) {
    if(accessibleObject instanceof Constructor) {
      Constructor<?> constructor = (Constructor<?>)accessibleObject;

      return concreteClass.getName() + "#<init>(" + formatInjectionParameterTypes(constructor.getGenericParameterTypes()) + ")";
    }
    else if(accessibleObject instanceof Field) {
      Field field = (Field)accessibleObject;

      return "field \"" + field.getName() + "\" in [" + concreteClass.getName() + "]";
    }

    return concreteClass.getName() + "->" + accessibleObject;
  }

  private static String formatInjectionParameterTypes(Type[] types) {
    StringBuilder builder = new StringBuilder();

    for(Type type : types) {
      if(builder.length() > 0) {
        builder.append(", ");
      }
      builder.append(type.toString());
    }

    return builder.toString();
  }
}
