package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.instantiation.InstanceCreationFailure;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.reflect.MethodUtils;

class PostConstructor {
  private static final Comparator<Method> CLASS_HIERARCHY_COMPARATOR = (a, b) -> {
    if(a.getDeclaringClass().isAssignableFrom(b.getDeclaringClass())) {
      return -1;
    }
    else if(b.getDeclaringClass().isAssignableFrom(a.getDeclaringClass())) {
      return 1;
    }

    return 0;
  };

  private final List<Method> postConstructMethods;

  PostConstructor(Class<?> cls) {
    List<Method> methods = MethodUtils.getMethodsListWithAnnotation(cls, PostConstruct.class, true, true);

    Collections.sort(methods, CLASS_HIERARCHY_COMPARATOR);

    this.postConstructMethods = methods;
  }

  void call(Object instance) throws InstanceCreationFailure {
    for(Method method : postConstructMethods) {
      try {
        method.setAccessible(true);
        method.invoke(instance);
      }
      catch(Exception e) {
        throw new InstanceCreationFailure(method, "Exception in PostConstruct call", e);
      }
    }
  }
}
