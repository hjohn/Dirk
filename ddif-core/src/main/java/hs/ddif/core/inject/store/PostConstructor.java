package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.InstantiationException;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.reflect.MethodUtils;

class PostConstructor {
  private final List<Method> postConstructMethods;

  PostConstructor(Class<?> cls) {
    List<Method> methods = MethodUtils.getMethodsListWithAnnotation(cls, PostConstruct.class, true, true);

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

  void call(Object instance) throws InstantiationException {
    for(Method method : postConstructMethods) {
      try {
        method.setAccessible(true);
        method.invoke(instance);
      }
      catch(Exception e) {
        throw new InstantiationException(method, "Exception in PostConstruct call", e);
      }
    }
  }
}
