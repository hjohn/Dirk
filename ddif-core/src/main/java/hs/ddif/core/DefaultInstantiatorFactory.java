package hs.ddif.core;

import hs.ddif.api.instantiation.Instantiator;
import hs.ddif.api.instantiation.InstantiatorFactory;
import hs.ddif.api.instantiation.TypeExtension;
import hs.ddif.api.instantiation.domain.Key;
import hs.ddif.api.util.Types;

import java.lang.reflect.AnnotatedElement;

/**
 * Produces type specific {@link Instantiator}s.
 */
class DefaultInstantiatorFactory implements InstantiatorFactory {
  private final TypeExtensionStore typeExtensionStore;

  /**
   * Constructs a new instance.
   *
   * @param typeExtensionStore a {@link TypeExtensionStore}, cannot be {@code null}
   */
  DefaultInstantiatorFactory(TypeExtensionStore typeExtensionStore) {
    this.typeExtensionStore = typeExtensionStore;
  }

  @Override
  public <T> Instantiator<T> getInstantiator(Key key, AnnotatedElement element) {
    TypeExtension<T> extension = typeExtensionStore.getExtension(Types.raw(key.getType()));

    return extension.create(this, key, element);
  }
}
