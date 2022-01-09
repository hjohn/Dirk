package hs.ddif.core.inject.injectable;

import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.injection.ObjectFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * Creates {@link Injectable}s.
 */
public interface InjectableFactory {

  /**
   * Creates a new {@link Injectable}.
   *
   * @param type a {@link Type}, cannot be null
   * @param qualifiers a set of qualifier {@link Annotation}s, cannot be null or contain nulls, but can be empty
   * @param bindings a list of {@link Binding}s, cannot be null or contain nulls, but can be empty
   * @param scope a scope {@link Annotation}, can be null
   * @param discriminator an object to serve as a discriminator for similar injectables, can be null
   * @param objectFactory an {@link ObjectFactory}, cannot be null
   * @return a {@link Injectable}, never null
   */
  Injectable create(Type type, Set<Annotation> qualifiers, List<Binding> bindings, Annotation scope, Object discriminator, ObjectFactory objectFactory);
}
