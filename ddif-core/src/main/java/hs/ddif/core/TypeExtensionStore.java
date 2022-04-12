package hs.ddif.core;

import hs.ddif.api.instantiation.TypeExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A store for {@link TypeExtension}s.
 */
class TypeExtensionStore {

  /**
   * Map containing {@link TypeExtension} by their supported type. Key {@code null}
   * is used as the default extension.
   */
  private final Map<Class<?>, TypeExtension<?>> typeExtensions;

  private final TypeExtension<?> defaultExtension;

  /**
   * Constructs a new instance.
   *
   * @param defaultExtension a default {@link TypeExtension} used when there is no type match, cannot be {@code null}
   * @param typeExtensions a map of {@link TypeExtension}s, cannot be {@code null} but can be empty
   */
  public TypeExtensionStore(TypeExtension<?> defaultExtension, Map<Class<?>, TypeExtension<?>> typeExtensions) {
    Map<Class<?>, TypeExtension<?>> map = new HashMap<>();

    map.putAll(typeExtensions);

    this.typeExtensions = Collections.unmodifiableMap(map);
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