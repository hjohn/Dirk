package hs.ddif.core;

import hs.ddif.spi.instantiation.InjectionTargetExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A store for {@link InjectionTargetExtension}s.
 */
class InjectionTargetExtensionStore {

  /**
   * Map containing {@link InjectionTargetExtension} by their supported type. Key {@code null}
   * is used as the default extension.
   */
  private final Map<Class<?>, InjectionTargetExtension<?>> extensions = new HashMap<>();

  private final InjectionTargetExtension<?> defaultExtension;

  /**
   * Constructs a new instance.
   *
   * @param defaultExtension a default {@link InjectionTargetExtension} used when there is no type match, cannot be {@code null}
   * @param extensions a collection of {@link InjectionTargetExtension}s, cannot be {@code null} but can be empty
   */
  public InjectionTargetExtensionStore(InjectionTargetExtension<?> defaultExtension, Collection<InjectionTargetExtension<?>> extensions) {
    for(InjectionTargetExtension<?> extension : extensions) {
      this.extensions.put(Objects.requireNonNull(extension.getInstantiatorType(), "instantiatorType of " + extension), extension);
    }

    this.defaultExtension = defaultExtension;
  }

  /**
   * Gets the {@link InjectionTargetExtension} which handled the given {@link Class}. A
   * default extension is returned for any targets that were not specifically configured.
   *
   * @param <T> the type handled by the extension
   * @param cls a {@link Class}, cannot be {@code null}
   * @return an {@link InjectionTargetExtension}, never {@code null}
   */
  public <T> InjectionTargetExtension<T> getExtension(Class<?> cls) {
    InjectionTargetExtension<?> extension = extensions.get(cls);

    if(extension == null) {
      extension = defaultExtension;
    }

    @SuppressWarnings("unchecked")
    InjectionTargetExtension<T> cast = (InjectionTargetExtension<T>)extension;

    return cast;
  }
}
