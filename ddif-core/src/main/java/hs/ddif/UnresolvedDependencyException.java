package hs.ddif;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class UnresolvedDependencyException extends DependencyException {

  public UnresolvedDependencyException(Injectable injectable, AccessibleObject accessibleObject, Key key) {
    super(key + " required for: " + formatInjectionPoint(injectable.getInjectableClass(), accessibleObject));
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
