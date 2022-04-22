package hs.ddif.core.config;

import hs.ddif.api.instantiation.InstanceCreationException;
import hs.ddif.api.instantiation.Key;
import hs.ddif.api.util.Types;
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.Instantiator;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.TypeExtension;
import hs.ddif.spi.instantiation.TypeTrait;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.TypeVariable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Type extension for {@link Instantiator}s that gather all matching injectables
 * in a {@link List}.
 *
 * @param <T> the type of element in the collection
 */
public class ListTypeExtension<T> implements TypeExtension<List<T>> {
  private static final TypeVariable<?> TYPE_VARIABLE = List.class.getTypeParameters()[0];
  private static final Set<TypeTrait> NONE = EnumSet.noneOf(TypeTrait.class);

  private final AnnotationStrategy annotationStrategy;

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   */
  public ListTypeExtension(AnnotationStrategy annotationStrategy) {
    this.annotationStrategy = annotationStrategy;
  }

  @Override
  public Instantiator<List<T>> create(InstantiatorFactory factory, Key key, AnnotatedElement element) {
    Key elementKey = new Key(Types.getTypeParameter(key.getType(), List.class, TYPE_VARIABLE), key.getQualifiers());
    boolean optional = annotationStrategy.isOptional(element);

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return elementKey;
      }

      @Override
      public List<T> getInstance(InstantiationContext context) throws InstanceCreationException {
        List<T> instances = context.createAll(elementKey);

        return instances.isEmpty() && optional ? null : instances;
      }

      @Override
      public Set<TypeTrait> getTypeTraits() {
        return NONE;
      }
    };
  }
}
