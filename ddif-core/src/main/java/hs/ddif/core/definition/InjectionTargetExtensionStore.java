package hs.ddif.core.definition;

import hs.ddif.spi.instantiation.InjectionTargetExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A store for {@link InjectionTargetExtension}s.
 */
public class InjectionTargetExtensionStore {

  /**
   * Map containing {@link InjectionTargetExtension} by their supported type. Key {@code null}
   * is used as the default extension.
   */
  private final Map<Class<?>, InjectionTargetExtension<?, ?>> extensions = new HashMap<>();

  /**
   * Constructs a new instance.
   *
   * @param extensions a collection of {@link InjectionTargetExtension}s, cannot be {@code null} but can be empty
   */
  public InjectionTargetExtensionStore(Collection<InjectionTargetExtension<?, ?>> extensions) {
    for(InjectionTargetExtension<?, ?> extension : extensions) {
      Class<?> targetClass = extension.getTargetClass();

      if(targetClass == null) {
        throw new IllegalArgumentException("extension " + extension + " target class cannot be null");
      }

      /*
       * Only interfaces with type parameters are allowed for now as there are no
       * known use cases to allow anything else:
       */

      if(!targetClass.isInterface()) {
        throw new IllegalArgumentException("extension " + extension + " target class must be an interface: " + targetClass);
      }
      if(targetClass.getTypeParameters().length == 0) {
        throw new IllegalArgumentException("extension " + extension + " target class must declare at least one type parameter: " + targetClass);
      }

      this.extensions.put(targetClass, extension);
    }
  }

  /**
   * Gets the {@link InjectionTargetExtension} which handles the given {@link Class}. A
   * default extension is returned for any targets that were not specifically configured.
   *
   * @param <T> the type handled by the extension
   * @param <E> the element type handled by the extension
   * @param cls a {@link Class}, cannot be {@code null}
   * @return an {@link InjectionTargetExtension}, never {@code null}
   */
  public <T, E> InjectionTargetExtension<T, E> getExtension(Class<T> cls) {
    InjectionTargetExtension<?, ?> extension = extensions.get(cls);

    if(extension == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    InjectionTargetExtension<T, E> cast = (InjectionTargetExtension<T, E>)extension;

    return cast;
  }
}
