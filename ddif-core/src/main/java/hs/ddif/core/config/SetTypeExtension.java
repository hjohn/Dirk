package hs.ddif.core.config;

import hs.ddif.core.definition.bind.AnnotationStrategy;
import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.Instantiator;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.TypeExtension;
import hs.ddif.core.instantiation.TypeTrait;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.store.Key;
import hs.ddif.core.util.Types;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.TypeVariable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Type extension for {@link Instantiator}s that gather all matching injectables
 * in a {@link Set}.
 *
 * @param <T> the type of element in the collection
 */
public class SetTypeExtension<T> implements TypeExtension<Set<T>> {
  private static final TypeVariable<?> TYPE_VARIABLE = Set.class.getTypeParameters()[0];
  private static final Set<TypeTrait> NONE = EnumSet.noneOf(TypeTrait.class);

  private final AnnotationStrategy annotationStrategy;

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   */
  public SetTypeExtension(AnnotationStrategy annotationStrategy) {
    this.annotationStrategy = annotationStrategy;
  }

  @Override
  public Instantiator<Set<T>> create(InstantiatorFactory factory, Key key, AnnotatedElement element) {
    Key elementKey = new Key(Types.getTypeParameter(key.getType(), Set.class, TYPE_VARIABLE), key.getQualifiers());
    boolean optional = annotationStrategy.isOptional(element);

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return elementKey;
      }

      @Override
      public Set<T> getInstance(InstantiationContext context) throws InstanceCreationFailure {
        List<T> instances = context.createAll(elementKey);

        return instances.isEmpty() && optional ? null : new HashSet<>(instances);
      }

      @Override
      public Set<TypeTrait> getTypeTraits() {
        return NONE;
      }
    };
  }
}
