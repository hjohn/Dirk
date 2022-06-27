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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.util.Primitives;

/**
 * Provides {@link Binding}s for constructors, methods and fields.
 */
public class BindingProvider {
  private final GenericBindingProvider<Binding> delegate;

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   */
  public BindingProvider(AnnotationStrategy annotationStrategy) {
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
    private final boolean optional;
    private final AccessibleObject accessibleObject;
    private final Parameter parameter;

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

      this.type = Primitives.toBoxed(type);
      this.qualifiers = Collections.unmodifiableSet(qualifiers);
      this.optional = optional;
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
    public boolean isOptional() {
      return optional;
    }

    @Override
    public AccessibleObject getAccessibleObject() {
      return accessibleObject;
    }

    @Override
    public Parameter getParameter() {
      return parameter;
    }

    @Override
    public String toString() {
      if(accessibleObject instanceof Executable) {
        int index = Arrays.asList(((Executable)accessibleObject).getParameters()).indexOf(parameter);

        return "Parameter " + index + " [" + type + "] of [" + accessibleObject + "]";
      }

      if(accessibleObject != null) {
        return "Field [" + (getQualifiers().isEmpty() ? "" : getQualifiers().stream().map(Object::toString).collect(Collectors.joining(" ")) + " ") + ((Field)accessibleObject).toGenericString() + "]";
      }

      return "Owner Type [" + type + "]";
    }
  }
}
