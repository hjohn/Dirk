package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.injection.ObjectFactory;

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
   * @param type a {@link Type}, cannot be {@code null}
   * @param qualifiers a set of qualifier {@link Annotation}s, cannot be {@code null} or contain {@code null}s, but can be empty
   * @param bindings a list of {@link Binding}s, cannot be {@code null} or contain {@code null}s, but can be empty
   * @param scope a scope {@link Annotation}, can be {@code null}
   * @param discriminator an object to serve as a discriminator for similar injectables, can be {@code null}
   * @param objectFactory an {@link ObjectFactory}, cannot be {@code null}
   * @return a {@link Injectable}, never {@code null}
   * @throws UninjectableTypeException when the given {@link Type} is not suitable for injection
   */
  Injectable create(Type type, Set<Annotation> qualifiers, List<Binding> bindings, Annotation scope, Object discriminator, ObjectFactory objectFactory) throws UninjectableTypeException;
}
