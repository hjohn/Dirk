package org.int4.dirk.library;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.spi.config.LifeCycleCallbacks;
import org.int4.dirk.spi.config.LifeCycleCallbacksFactory;
import org.int4.dirk.util.Methods;

/**
 * Implementation of a {@link LifeCycleCallbacksFactory} which determines which life cycle
 * methods to call based on configurable annotations.
 */
public class AnnotationBasedLifeCycleCallbacksFactory implements LifeCycleCallbacksFactory {
  private static final Logger LOGGER = Logger.getLogger(AnnotationBasedLifeCycleCallbacksFactory.class.getName());
  private static final Comparator<Method> CLASS_HIERARCHY_COMPARATOR = (a, b) -> {
    if(a.getDeclaringClass().isAssignableFrom(b.getDeclaringClass())) {
      return -1;
    }
    else if(b.getDeclaringClass().isAssignableFrom(a.getDeclaringClass())) {
      return 1;
    }

    return 0;
  };

  private static final LifeCycleCallbacks EMPTY = new LifeCycleCallbacks() {
    @Override
    public void postConstruct(Object instance) {
    }

    @Override
    public void preDestroy(Object instance) {
    }

    @Override
    public boolean needsDestroy() {
      return false;
    }
  };

  private final Class<? extends Annotation> postConstruct;
  private final Class<? extends Annotation> preDestroy;

  /**
   * Constructs a new instance.
   *
   * @param postConstruct a marker annotation {@link Class} for post construct methods, cannot be {@code null}
   * @param preDestroy a marker annotation {@link Class} for pre-destroy methods, cannot be {@code null}
   */
  public AnnotationBasedLifeCycleCallbacksFactory(Class<? extends Annotation> postConstruct, Class<? extends Annotation> preDestroy) {
    this.postConstruct = Objects.requireNonNull(postConstruct, "postConstruct cannot be null");
    this.preDestroy = Objects.requireNonNull(preDestroy, "preDestroy cannot be null");
  }

  @Override
  public LifeCycleCallbacks create(Class<?> cls) throws DefinitionException {
    List<Method> postConstructMethods = Methods.findAnnotated(cls, postConstruct);
    List<Method> preDestroyMethods = Methods.findAnnotated(cls, preDestroy);

    if(postConstructMethods.isEmpty() && preDestroyMethods.isEmpty()) {
      return EMPTY;
    }

    Collections.sort(postConstructMethods, CLASS_HIERARCHY_COMPARATOR);
    Collections.sort(preDestroyMethods, CLASS_HIERARCHY_COMPARATOR.reversed());

    for(Method method : postConstructMethods) {
      checkMethod(method);
      method.setAccessible(true);
    }

    for(Method method : preDestroyMethods) {
      checkMethod(method);
      method.setAccessible(true);
    }

    return new DefaultLifeCycleCallbacks(postConstructMethods, preDestroyMethods);
  }

  private static void checkMethod(Method method) throws DefinitionException {
    if(method.getParameterCount() > 0) {
      throw new DefinitionException(method, "cannot have parameters when annotated as a lifecycle method (post construct or pre destroy)");
    }
  }

  private static class DefaultLifeCycleCallbacks implements LifeCycleCallbacks {
    private final List<Method> postConstructMethods;
    private final List<Method> preDestroyMethods;

    DefaultLifeCycleCallbacks(List<Method> postConstructMethods, List<Method> preDestroyMethods) {
      this.postConstructMethods = postConstructMethods;
      this.preDestroyMethods = preDestroyMethods;
    }

    @Override
    public void postConstruct(Object instance) throws InvocationTargetException {
      for(Method method : postConstructMethods) {
        try {
          method.invoke(instance);
        }
        catch(IllegalAccessException | IllegalArgumentException e) {
          throw new IllegalStateException(method + " call failed", e);
        }
      }
    }

    @Override
    public void preDestroy(Object instance) {
      for(Method method : preDestroyMethods) {
        try {
          method.invoke(instance);
        }
        catch(InvocationTargetException e) {
          LOGGER.log(Level.WARNING, "Exception thrown by pre-destroy method: " + method, e.getCause());
        }
        catch(Exception e) {
          LOGGER.log(Level.WARNING, "Exception while calling pre-destroy method: " + method, e);
        }
      }
    }

    @Override
    public boolean needsDestroy() {
      return !preDestroyMethods.isEmpty();
    }
  }
}
