package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.injection.Constructable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Factory for {@link Injectable}s.
 */
public interface InjectableFactory {

  /**
   * Creates {@link Injectable}s given an owner {@link Type}, an optional {@link Member}
   * an {@link AnnotatedElement}, a list of {@link Binding}s and an {@link Constructable}.
   *
   * <p>The type of the {@link Injectable} is determined from the given member if
   * not {@code null} and otherwise is the same as the given owner type.
   *
   * <p>The scope and qualifiers of the {@link Injectable} are determined from the given
   * annotated element.
   *
   * @param <T> the type of the instances produced
   * @param ownerType a {@link Type}, cannot be {@code null}
   * @param member a {@link Member} of the ownerType, can be {@code null}
   * @param element an {@link AnnotatedElement} from which to get scope and qualifier annotations, cannot be {@code null}
   * @param bindings a list of {@link Binding}s, cannot be {@code null} or contain {@code null}s, but can be empty
   * @param constructable a {@link Constructable}, cannot be {@code null}
   * @return a {@link Injectable}, never {@code null}
   * @throws DefinitionException when the owner type does not own the member; when the injectable's type cannot be determined or is void;
   *   when the annotated element has multiple scope annotations or is inject annotated
   */
  <T> Injectable<T> create(Type ownerType, Member member, AnnotatedElement element, List<Binding> bindings, Constructable<T> constructable);

}
