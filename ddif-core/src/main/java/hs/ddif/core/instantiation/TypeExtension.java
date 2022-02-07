package hs.ddif.core.instantiation;

import hs.ddif.annotations.Opt;
import hs.ddif.core.store.Key;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Interface for customizing how a specific type can be instantiated.
 *
 * @param <T> the type customized
 */
public interface TypeExtension<T> {

  /**
   * Creates a new {@link Instantiator} which will produce a type matching
   * the given {@link Key}. Depending on the {@link Instantiator} produced the
   * instantiation process may undergo further customization based on annotations
   * found at the injection site, represented by the given {@link AnnotatedElement}.
   *
   * @param instantiatorFactory an {@link InstantiatorFactory} to get delegate {@link Instantiator}s, cannot be {@code null}
   * @param key a {@link Key}, cannot be {@code null}
   * @param element an {@link AnnotatedElement}, can be {@code null}
   * @return an {@link Instantiator}, never {@code null}
   */
  Instantiator<T> create(InstantiatorFactory instantiatorFactory, Key key, AnnotatedElement element);

  /**
   * Helper method to detect if an {@link AnnotatedElement} should be treated
   * as optional.
   *
   * @param element an {@link AnnotatedElement}, cannot be {@code null}
   * @return {@code true} if the element was optional, otherwise {@code false}
   */
  public static boolean isOptional(AnnotatedElement element) {
    if(element != null) {
      for(Annotation annotation : element.getAnnotations()) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        String simpleName = annotationType.getName();

        if(simpleName.endsWith(".Nullable") || annotationType.equals(Opt.class)) {
          return true;
        }
      }
    }

    return false;
  }
}