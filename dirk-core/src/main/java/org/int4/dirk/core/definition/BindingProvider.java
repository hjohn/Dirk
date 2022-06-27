package org.int4.dirk.core.definition;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.util.Key;
import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.TypeTrait;
import org.int4.dirk.util.Primitives;
import org.int4.dirk.util.Types;

/**
 * Provides {@link Binding}s for constructors, methods and fields.
 */
public class BindingProvider {
  private static final EnumSet<TypeTrait> REQUIRES_EXACTLY_ONE = EnumSet.of(TypeTrait.REQUIRES_AT_LEAST_ONE, TypeTrait.REQUIRES_AT_MOST_ONE);

  private final InjectionTargetExtensionStore injectionTargetExtensionStore;
  private final GenericBindingProvider<Binding> delegate;

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   * @param injectionTargetExtensionStore an {@link InjectionTargetExtensionStore}, cannot be {@code null}
   */
  public BindingProvider(AnnotationStrategy annotationStrategy, InjectionTargetExtensionStore injectionTargetExtensionStore) {
    this.injectionTargetExtensionStore = injectionTargetExtensionStore;

    this.delegate = new GenericBindingProvider<>(annotationStrategy, (type, annotatedElement) -> {
      Parameter parameter = annotatedElement instanceof Parameter ? (Parameter)annotatedElement : null;

      return new DefaultBinding(
        type,
        annotatedElement == null ? Set.of() : annotationStrategy.getQualifiers(annotatedElement),
        annotatedElement == null ? false : annotationStrategy.isOptional(annotatedElement),
        parameter == null ? (AccessibleObject)annotatedElement : parameter.getDeclaringExecutable(),
        parameter
      );
    });
  }

  /**
   * Returns all bindings for the given {@link Constructor} and all member bindings
   * for the given class.
   *
   * @param constructor a {@link Constructor} to examine for bindings, cannot be {@code null}
   * @param cls a {@link Class} to examine for bindings, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   * @throws DefinitionException when a definition problem is encountered
   */
  public List<Binding> ofConstructorAndMembers(Constructor<?> constructor, Class<?> cls) throws DefinitionException {
    return delegate.ofConstructorAndMembers(constructor, cls);
  }

  /**
   * Returns all bindings for the given {@link Constructor}.
   *
   * @param constructor a {@link Constructor} to examine for bindings, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   * @throws DefinitionException when a definition problem is encountered
   */
  public List<Binding> ofConstructor(Constructor<?> constructor) throws DefinitionException {
    return delegate.ofConstructor(constructor);
  }

  /**
   * Returns all member bindings for the given class. These are inject annotated
   * methods and fields, but not constructors.
   *
   * @param cls a {@link Class} to examine for bindings, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}, but can be empty
   * @throws DefinitionException when a definition problem is encountered
   */
  public List<Binding> ofMembers(Class<?> cls) throws DefinitionException {
    return delegate.ofMembers(cls);
  }

  /**
   * Returns all bindings for the given {@link Method}.
   *
   * @param method a {@link Method} to examine for bindings, cannot be {@code null}
   * @param ownerType a {@link Type} in which this method is declared, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   * @throws DefinitionException when a definition problem is encountered
   */
  public List<Binding> ofMethod(Method method, Type ownerType) throws DefinitionException {
    return delegate.ofMethod(method, ownerType);
  }

  /**
   * Returns all bindings for the given {@link Field}.
   *
   * @param field a {@link Field} to examine for bindings, cannot be {@code null}
   * @param ownerType a {@link Type} in which this executable is declared, cannot be {@code null}
   * @return an immutable list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   * @throws DefinitionException when a definition problem is encountered
   */
  public List<Binding> ofField(Field field, Type ownerType) throws DefinitionException {
    return delegate.ofField(field, ownerType);
  }

  /**
   * Returns an annotated {@link Constructor} suitable for injection.
   *
   * @param cls a {@link Class}, cannot be {@code null}
   * @return a {@link Constructor} suitable for injection, never {@code null}
   * @throws DefinitionException when a definition problem is encountered
   */
  public Constructor<?> getAnnotatedConstructor(Class<?> cls) throws DefinitionException {
    return delegate.getAnnotatedConstructor(cls);
  }

  /**
   * Returns a {@link Constructor} suitable for injection. A public empty
   * constructor is considered suitable if no other constructors are annotated.
   *
   * @param <T> the class type
   * @param cls a {@link Class}, cannot be {@code null}
   * @return a {@link Constructor} suitable for injection, never {@code null}
   * @throws DefinitionException when a definition problem is encountered
   */
  public <T> Constructor<T> getConstructor(Class<T> cls) throws DefinitionException {
    return delegate.getConstructor(cls);
  }

  private class DefaultBinding implements Binding {
    private final Type type;
    private final Set<Annotation> qualifiers;
    private final Key elementKey;
    private final boolean optional;
    private final Set<TypeTrait> typeTraits;
    private final AccessibleObject accessibleObject;
    private final Parameter parameter;
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    /**
     * Constructs a new instance.
     *
     * @param type a {@link Type}, cannot be {@code null}
     * @param qualifiers a set of qualifier annotations, cannot be {@code null}
     * @param optional {@code true} when the binding is optional, otherwise {@code false}
     * @param accessibleObject an {@link AccessibleObject}, can be {@code null}
     * @param parameter a {@link Parameter}, cannot be {@code null} for {@link java.lang.reflect.Executable}s and must be {@code null} otherwise
     */
    public DefaultBinding(Type type, Set<Annotation> qualifiers, boolean optional, AccessibleObject accessibleObject, Parameter parameter) {
      if(type == null) {
        throw new IllegalArgumentException("type cannot be null");
      }
      if(qualifiers == null) {
        throw new IllegalArgumentException("qualifiers cannot be null");
      }
      if(accessibleObject instanceof Executable && parameter == null) {
        throw new IllegalArgumentException("parameter cannot be null when accessibleObject is an instance of Executable");
      }
      if(!(accessibleObject instanceof Executable) && parameter != null) {
        throw new IllegalArgumentException("parameter must be null when accessibleObject is not an instance of Executable");
      }

      InjectionType injectionType = constructData(type);

      this.type = Primitives.toBoxed(type);
      this.qualifiers = Collections.unmodifiableSet(qualifiers);
      this.elementKey = new Key(injectionType == null ? type : injectionType.getElementType(), qualifiers);
      this.optional = optional;
      this.typeTraits = injectionType == null ? REQUIRES_EXACTLY_ONE : Collections.unmodifiableSet(injectionType.getTypeTraits());
      this.accessibleObject = accessibleObject;
      this.parameter = parameter;

      if(accessibleObject != null) {
        accessibleObject.setAccessible(true);
      }
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return qualifiers;
    }

    @Override
    public Key getElementKey() {
      return elementKey;
    }

    @Override
    public boolean isOptional() {
      return optional;
    }

    @Override
    public Set<TypeTrait> getTypeTraits() {
      return typeTraits;
    }

    @Override
    public AccessibleObject getAccessibleObject() {
      return accessibleObject;
    }

    @Override
    public Parameter getParameter() {
      return parameter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T associateIfAbsent(String key, Supplier<T> valueSupplier) {
      return (T)data.computeIfAbsent(key, k -> valueSupplier.get());
    }

    @Override
    public String toString() {
      if(accessibleObject instanceof Executable) {
        int index = Arrays.asList(((Executable)accessibleObject).getParameters()).indexOf(parameter);

        return "Parameter " + index + " [" + type + "] of [" + accessibleObject + "]";
      }
      else if(accessibleObject != null) {
        return "Field [" + (getQualifiers().isEmpty() ? "" : getQualifiers().stream().map(Object::toString).collect(Collectors.joining(" ")) + " ") + ((Field)accessibleObject).toGenericString() + "]";
      }

      return "Owner Type [" + type + "]";
    }
  }

  private InjectionType constructData(Type inputType) {
    Type type = inputType;
    Set<TypeTrait> typeTraits = null;
    boolean mergeTraits = true;

    for(;;) {
      InjectionTargetExtension<?, ?> extension = injectionTargetExtensionStore.getExtension(Types.raw(type));

      if(typeTraits == null) {
        if(extension == null) {
          return null;
        }

        typeTraits = new HashSet<>();
      }

      if(mergeTraits) {
        Set<TypeTrait> traits = extension == null ? REQUIRES_EXACTLY_ONE : extension.getTypeTraits();

        typeTraits.addAll(traits);

        if(!traits.contains(TypeTrait.LAZY)) {
          mergeTraits = false;
        }
      }

      if(extension == null) {
        return new InjectionType(type, typeTraits);
      }

      type = extension.getElementType(type);  // returning the same type is disallowed (makes no sense either)
    }
  }

  private static class InjectionType {
    final Type elementType;
    final Set<TypeTrait> typeTraits;

    InjectionType(Type elementType, Set<TypeTrait> typeTraits) {
      this.elementType = elementType;
      this.typeTraits = typeTraits;
    }

    Type getElementType() {
      return elementType;
    }

    Set<TypeTrait> getTypeTraits() {
      return typeTraits;
    }
  }
}
