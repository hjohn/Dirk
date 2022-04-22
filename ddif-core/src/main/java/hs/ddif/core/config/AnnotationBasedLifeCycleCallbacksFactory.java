package hs.ddif.core.config;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.core.util.Methods;
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.config.LifeCycleCallbacks;
import hs.ddif.spi.config.LifeCycleCallbacksFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  private final Class<? extends Annotation> postConstruct;
  private final Class<? extends Annotation> preDestroy;
  private final AnnotationStrategy annotationStrategy;

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   * @param postConstruct a marker annotation {@link Class} for post construct methods, cannot be {@code null}
   * @param preDestroy a marker annotation {@link Class} for pre-destroy methods, cannot be {@code null}
   */
  public AnnotationBasedLifeCycleCallbacksFactory(AnnotationStrategy annotationStrategy, Class<? extends Annotation> postConstruct, Class<? extends Annotation> preDestroy) {
    this.annotationStrategy = Objects.requireNonNull(annotationStrategy, "annotationStrategy cannot be null");
    this.postConstruct = Objects.requireNonNull(postConstruct, "postConstruct cannot be null");
    this.preDestroy = Objects.requireNonNull(preDestroy, "preDestroy cannot be null");
  }

  @Override
  public LifeCycleCallbacks create(Class<?> cls) throws DefinitionException {
    List<Method> postConstructMethods = Methods.findAnnotated(cls, postConstruct);
    List<Method> preDestroyMethods = Methods.findAnnotated(cls, preDestroy);

    Collections.sort(postConstructMethods, CLASS_HIERARCHY_COMPARATOR);
    Collections.sort(preDestroyMethods, CLASS_HIERARCHY_COMPARATOR.reversed());

    for(Method method : postConstructMethods) {
      checkMethod(method);
    }

    for(Method method : preDestroyMethods) {
      checkMethod(method);
    }

    return new DefaultLifeCycleCallbacks(postConstructMethods, preDestroyMethods);
  }

  private void checkMethod(Method method) throws DefinitionException {
    if(method.getParameterCount() > 0) {
      throw new DefinitionException(method, "cannot have parameters when annotated as a lifecycle method (post construct or pre destroy)");
    }
    if(!annotationStrategy.getInjectAnnotations(method).isEmpty()) {
      throw new DefinitionException(method, "cannot be inject annotated when annotated as a lifecycle method (post construct or pre destroy): " + annotationStrategy.getInjectAnnotations(method));
    }
  }

  private class DefaultLifeCycleCallbacks implements LifeCycleCallbacks {
    private final List<Method> postConstructMethods;
    private final List<Method> preDestroyMethods;

    DefaultLifeCycleCallbacks(List<Method> postConstructMethods, List<Method> preDestroyMethods) {
      this.postConstructMethods = postConstructMethods;
      this.preDestroyMethods = preDestroyMethods;
    }

    @Override
    public void postConstruct(Object instance) throws Exception {
      for(Method method : postConstructMethods) {
        method.setAccessible(true);
        method.invoke(instance);
      }
    }

    @Override
    public void preDestroy(Object instance) {
      for(Method method : preDestroyMethods) {
        try {
          method.setAccessible(true);
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
  }
}
