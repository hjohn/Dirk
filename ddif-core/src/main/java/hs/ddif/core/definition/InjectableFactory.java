package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.injection.ObjectFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Creates {@link Injectable}s given a {@link Type}, an {@link AnnotatedElement},
 * a list of {@link Binding}s and an {@link ObjectFactory}.
 */
public interface InjectableFactory {

  /**
   * Creates a new {@link Injectable}.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @param element an {@link AnnotatedElement} from which to get scope and qualifier annotations, cannot be {@code null}
   * @param bindings a list of {@link Binding}s, cannot be {@code null} or contain {@code null}s, but can be empty
   * @param objectFactory an {@link ObjectFactory}, cannot be {@code null}
   * @return a {@link Injectable}, never {@code null}
   */
  Injectable create(Type type, AnnotatedElement element, List<Binding> bindings, ObjectFactory objectFactory);

}
