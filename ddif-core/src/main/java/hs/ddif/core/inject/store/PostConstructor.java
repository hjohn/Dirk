package hs.ddif.core.inject.store;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.reflect.MethodUtils;

public class PostConstructor {
  private final List<Method> postConstructMethods;
  private final Class<?> cls;

  public PostConstructor(Class<?> cls) {
    this.cls = cls;
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

  public void call(Object bean) {
    try {
      for(Method method : postConstructMethods) {
        method.setAccessible(true);
        method.invoke(bean);
      }
    }
    catch(Exception e) {
      throw new ConstructionException("PostConstruct call failed: " + cls, e);
    }
  }
}
