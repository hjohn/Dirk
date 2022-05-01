package hs.ddif.core;

import hs.ddif.spi.instantiation.TypeExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A store for {@link TypeExtension}s.
 */
class TypeExtensionStore {

  /**
   * Map containing {@link TypeExtension} by their supported type. Key {@code null}
   * is used as the default extension.
   */
  private final Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

  private final TypeExtension<?> defaultExtension;

  /**
   * Constructs a new instance.
   *
   * @param defaultExtension a default {@link TypeExtension} used when there is no type match, cannot be {@code null}
   * @param typeExtensions a collection of {@link TypeExtension}s, cannot be {@code null} but can be empty
   */
  public TypeExtensionStore(TypeExtension<?> defaultExtension, Collection<TypeExtension<?>> typeExtensions) {
    for(TypeExtension<?> extension : typeExtensions) {
      this.typeExtensions.put(Objects.requireNonNull(extension.getInstantiatorType(), "instantiatorType of " + extension), extension);
    }

    this.defaultExtension = defaultExtension;
  }

  /**
   * Gets the {@link TypeExtension} which handled the given {@link Class}. A
   * default extension is returned for any types that were not specifically configured.
   *
   * @param <T> the type handled by the extension
   * @param cls a {@link Class}, cannot be {@code null}
   * @return a {@link TypeExtension}, never {@code null}
   */
  public <T> TypeExtension<T> getExtension(Class<?> cls) {
    TypeExtension<?> extension = typeExtensions.get(cls);

    if(extension == null) {
      extension = defaultExtension;
    }

    @SuppressWarnings("unchecked")
    TypeExtension<T> cast = (TypeExtension<T>)extension;

    return cast;
  }
}
