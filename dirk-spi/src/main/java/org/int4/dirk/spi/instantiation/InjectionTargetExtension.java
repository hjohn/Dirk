package org.int4.dirk.spi.instantiation;

import java.lang.reflect.TypeVariable;
import java.util.Objects;

/**
 * An interface to allow for custom handling of injection targets of type
 * {@code T}. The target class must be an interface with at least one type variable.
 *
 * <p>Whenever an injection target of type {@code T} needs injection this
 * extension will be called to provide the actual value.
 *
 * @param <T> the type handled
 * @param <E> the element type required
 */
public class InjectionTargetExtension<T, E> {
  private final Resolution resolution;
  private final InstanceProvider<T, E> instanceProvider;
  private final TypeVariable<Class<T>> elementTypeVariable;

  /**
   * Constructs a new instance.
   *
   * @param elementTypeVariable a {@link TypeVariable} which represents the element type {@code E}, cannot be {@code null}
   * @param resolution a {@link Resolution}, cannot be {@code null}
   * @param instanceProvider an {@link InstanceProvider}, cannot be {@code null}
   */
  public InjectionTargetExtension(TypeVariable<Class<T>> elementTypeVariable, Resolution resolution, InstanceProvider<T, E> instanceProvider) {
    this.elementTypeVariable = Objects.requireNonNull(elementTypeVariable, "elementTypeVariable");
    this.resolution = Objects.requireNonNull(resolution, "resolution");
    this.instanceProvider = Objects.requireNonNull(instanceProvider, "instanceProvider");
  }

  /**
   * Returns the target class that is handled by this extension.
   *
   * @return a {@link Class}, never {@code null}
   */
  public final Class<T> getTargetClass() {
    return elementTypeVariable.getGenericDeclaration();
  }

  /**
   * Returns the {@link InstanceProvider} that can create the type handled by this extension.
   *
   * @return an {@link InstanceProvider}, never {@code null}
   */
  public final InstanceProvider<T, E> getInstanceProvider() {
    return instanceProvider;
  }

  /**
   * Returns a {@link TypeVariable} which represents the element type {@code E}.
   *
   * @return a {@link TypeVariable}, never {@code null}
   */
  public final TypeVariable<Class<T>> getElementTypeVariable() {
    return elementTypeVariable;
  }

  /**
   * Returns how the injection target should be resolved. This determines what kind of
   * restrictions are applied when an injection target of this type is added to an injector.
   * When eager, restrictions are enforced before the injector is modified. When lazy,
   * use of the {@link Instance} may result in errors if the required types are
   * not available.
   *
   * @return a {@link Resolution}, never {@code null}
   */
  public final Resolution getResolution() {
    return resolution;
  }
}
