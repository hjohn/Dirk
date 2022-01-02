package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Constructs {@link ResolvableInjectable}s for {@link Type}s which resolve to
 * concrete classes.
 */
public class ClassInjectableFactory {
  private final List<Extension> extensions;

  interface Extension {

    /**
     * Returns the precondition text. This should give a clear description of what
     * is required for this extension to create injectables.
     *
     * @return a precondition text, never null
     */
    String getPreconditionText();

    /**
     * Returns a {@link ResolvableInjectable} or {@code null} if the precondition
     * failed. If the precondition is met but the injectable could not be created
     * this will throw an exception.
     *
     * @param type a {@link Type}, never null
     * @return a {@link ResolvableInjectable} or {@code null} if the precondition failed
     */
    ResolvableInjectable create(Type type);
  }

  /**
   * Constructs a new instance.
   *
   * @param factory a {@link ResolvableInjectableFactory}, cannot be null
   */
  public ClassInjectableFactory(ResolvableInjectableFactory factory) {
    this.extensions = List.of(
      new AssistedInjectionExtension(factory),
      new ClassExtension(factory)
    );
  }

  /**
   * Attempts to create a new {@link ResolvableInjectable} from the given {@link Type}.
   *
   * @param type a {@link Type}, cannot be null
   * @return a {@link ResolvableInjectable}, never null
   * @throws BindingException when the given type does not meet all requirements
   */
  public ResolvableInjectable create(Type type) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }

    if(TypeUtils.containsTypeVariables(type)) {
      throw new BindingException("Unresolved type variables in " + type + " are not allowed: " + Arrays.toString(TypeUtils.getRawType(type, null).getTypeParameters()));
    }

    for(Extension extension : extensions) {
      ResolvableInjectable injectable = extension.create(type);

      if(injectable != null) {
        return injectable;
      }
    }

    throw new BindingException("Type cannot be injected: " + type + "; try supplying:" + extensions.stream().map(Extension::getPreconditionText).collect(Collectors.joining("\n - ", "\n - ", "")));
  }
}
