package hs.ddif.core.instantiation;

import hs.ddif.core.store.Key;
import hs.ddif.core.util.Types;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Produces type specific {@link Instantiator}s.
 */
public class InstantiatorFactory {

  /**
   * Map containing {@link TypeExtension} by their supported type. Key {@code null}
   * is used as the default extension.
   */
  private final Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

  /**
   * Constructs a new instance.
   *
   * @param typeExtensions a map of {@link TypeExtension}, cannot be {@code null} but can be empty
   */
  public InstantiatorFactory(Map<Class<?>, TypeExtension<?>> typeExtensions) {
    this.typeExtensions.putAll(typeExtensions);
    this.typeExtensions.put(null, new DirectTypeExtension<>());
  }

  /**
   * Gets an {@link Instantiator} for the given {@link Key} and using annotations
   * found on the optional given {@link AnnotatedElement}.
   *
   * @param <T> the type the {@link Instantiator} produces
   * @param key a {@link Key}, cannot be {@code null}
   * @param element an {@link AnnotatedElement}, can be {@code null}
   * @return an {@link Instantiator}, never {@code null}
   */
  public <T> Instantiator<T> getInstantiator(Key key, AnnotatedElement element) {
    TypeExtension<T> extension = getTypeExtension(key.getType());

    return extension.create(this, key, element);
  }

  private <T> TypeExtension<T> getTypeExtension(Type type) {
    TypeExtension<?> extension = typeExtensions.get(Types.raw(type));

    if(extension == null) {
      extension = typeExtensions.get(null);
    }

    @SuppressWarnings("unchecked")
    TypeExtension<T> cast = (TypeExtension<T>)extension;

    return cast;
  }
}
