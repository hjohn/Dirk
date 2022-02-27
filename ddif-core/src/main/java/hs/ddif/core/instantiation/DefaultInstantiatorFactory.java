package hs.ddif.core.instantiation;

import hs.ddif.core.store.Key;
import hs.ddif.core.util.Types;

import java.lang.reflect.AnnotatedElement;

/**
 * Produces type specific {@link Instantiator}s.
 */
public class DefaultInstantiatorFactory implements InstantiatorFactory {
  private final TypeExtensionStore typeExtensionStore;

  /**
   * Constructs a new instance.
   *
   * @param typeExtensionStore a {@link TypeExtensionStore}, cannot be {@code null}
   */
  public DefaultInstantiatorFactory(TypeExtensionStore typeExtensionStore) {
    this.typeExtensionStore = typeExtensionStore;
  }

  @Override
  public <T> Instantiator<T> getInstantiator(Key key, AnnotatedElement element) {
    TypeExtension<T> extension = typeExtensionStore.getExtension(Types.raw(key.getType()));

    return extension.create(this, key, element);
  }
}
