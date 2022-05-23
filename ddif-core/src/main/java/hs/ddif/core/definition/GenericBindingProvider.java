package hs.ddif.core.definition;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.util.Types;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides bindings for constructors, methods and fields.
 *
 * @param <B> the type of the resulting binding
 */
public class GenericBindingProvider<B> {
  private final AnnotationStrategy annotationStrategy;
  private final BindingFactory<B> factory;

  /**
   * Factory for creating bindings that have been found.
   *
   * @param <B> the type of the binding class
   */
  public interface BindingFactory<B> {

    /**
     * Creates a binding of type {@code B}.
     *
     * @param type a {@link Type}, never {@code null}
     * @param annotatedElement an {@link AnnotatedElement}, can be {@code null}
     * @return a binding of type {@code B}, never {@code null}
     * @throws DefinitionException when a definition problem is encountered
     */
    B create(Type type, AnnotatedElement annotatedElement) throws DefinitionException;
  }

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   * @param factory a factory for bindings of type {@code B}, cannot be {@code null}
   */
  public GenericBindingProvider(AnnotationStrategy annotationStrategy, BindingFactory<B> factory) {
    this.annotationStrategy = Objects.requireNonNull(annotationStrategy, "annotationStrategy");
    this.factory = Objects.requireNonNull(factory, "factory");
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
  public List<B> ofConstructorAndMembers(Constructor<?> constructor, Class<?> cls) throws DefinitionException {
    List<B> bindings = ofConstructor(constructor);

    bindings.addAll(ofMembers(cls));

    return bindings;
  }

  /**
   * Returns all bindings for the given {@link Constructor}.
   *
   * @param constructor a {@link Constructor} to examine for bindings, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   * @throws DefinitionException when a definition problem is encountered
   */
  public List<B> ofConstructor(Constructor<?> constructor) throws DefinitionException {
    return ofExecutable(constructor, constructor.getDeclaringClass());
  }

  /**
   * Returns all member bindings for the given class. These are inject annotated
   * methods and fields, but not constructors.
   *
   * @param cls a {@link Class} to examine for bindings, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}, but can be empty
   * @throws DefinitionException when a definition problem is encountered
   */
  public List<B> ofMembers(Class<?> cls) throws DefinitionException {
    List<B> bindings = new ArrayList<>();
    Class<?> currentInjectableClass = cls;
    Map<TypeVariable<?>, Type> typeArguments = null;

    while(currentInjectableClass != null) {
      for(Field field : currentInjectableClass.getDeclaredFields()) {
        if(!annotationStrategy.getInjectAnnotations(field).isEmpty()) {
          if(Modifier.isFinal(field.getModifiers())) {
            throw new DefinitionException(field, "of [" + cls + "] cannot be final");
          }

          if(typeArguments == null) {
            typeArguments = Types.getTypeArguments(cls, Object.class);  // pretty sure that you can re-use these even for when are examining fields of a super class later

            if(typeArguments == null) {
              throw new IllegalArgumentException("ownerType must be assignable to field's declaring class: " + cls + "; declaring class: " + currentInjectableClass);
            }
          }

          Type type = Types.resolveVariables(typeArguments, field.getGenericType());

          bindings.add(factory.create(type, field));
        }
      }

      for(Method method : currentInjectableClass.getDeclaredMethods()) {
        if(!annotationStrategy.getInjectAnnotations(method).isEmpty()) {
          if(method.getParameterCount() == 0) {
            throw new DefinitionException(method, "of [" + cls + "] must have parameters");
          }

          bindings.addAll(ofExecutable(method, cls));
        }
      }

      currentInjectableClass = currentInjectableClass.getSuperclass();
    }

    return bindings;
  }

  /**
   * Returns all bindings for the given {@link Method}.
   *
   * @param method a {@link Method} to examine for bindings, cannot be {@code null}
   * @param ownerType a {@link Type} in which this method is declared, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   * @throws DefinitionException when a definition problem is encountered
   */
  public List<B> ofMethod(Method method, Type ownerType) throws DefinitionException {
    List<B> bindings = ofExecutable(method, ownerType);

    if(!Modifier.isStatic(method.getModifiers())) {
      // For a non-static method, the class itself is also a required binding:
      bindings.add(ownerBinding(ownerType));
    }

    return bindings;
  }

  /**
   * Returns all bindings for the given {@link Field}.
   *
   * @param field a {@link Field} to examine for bindings, cannot be {@code null}
   * @param ownerType a {@link Type} in which this executable is declared, cannot be {@code null}
   * @return an immutable list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   * @throws DefinitionException when a definition problem is encountered
   */
  public List<B> ofField(Field field, Type ownerType) throws DefinitionException {

    /*
     * Fields don't have any bindings, unless it is a non-static field in which case the
     * declaring class is required before the field can be accessed.
     */

    return Modifier.isStatic(field.getModifiers()) ? List.of() : List.of(ownerBinding(ownerType));
  }

  /**
   * Returns an annotated {@link Constructor} suitable for injection.
   *
   * @param cls a {@link Class}, cannot be {@code null}
   * @return a {@link Constructor} suitable for injection, never {@code null}
   * @throws DefinitionException when a definition problem is encountered
   */
  public Constructor<?> getAnnotatedConstructor(Class<?> cls) throws DefinitionException {
    return getConstructor(cls, true);
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
    return getConstructor(cls, false);
  }

  private <T> Constructor<T> getConstructor(Class<T> cls, boolean annotatedOnly) throws DefinitionException {
    Constructor<T> suitableConstructor = null;
    Constructor<T> defaultConstructor = null;

    @SuppressWarnings("unchecked")
    Constructor<T>[] declaredConstructors = (Constructor<T>[])cls.getDeclaredConstructors();

    for(Constructor<T> constructor : declaredConstructors) {
      if(!annotationStrategy.getInjectAnnotations(constructor).isEmpty()) {
        if(suitableConstructor != null) {
          throw new DefinitionException(cls, "cannot have multiple Inject annotated constructors");
        }

        suitableConstructor = constructor;
      }
      else if(!annotatedOnly && constructor.getParameterCount() == 0 && Modifier.isPublic(constructor.getModifiers())) {
        defaultConstructor = constructor;
      }
    }

    if(suitableConstructor == null && defaultConstructor == null) {
      throw new DefinitionException(cls, "should have at least one suitable constructor; annotate a constructor" + (annotatedOnly ? "" : " or provide an empty public constructor"));
    }

    return suitableConstructor == null ? defaultConstructor : suitableConstructor;
  }

  private List<B> ofExecutable(Executable executable, Type ownerType) throws DefinitionException {
    Type[] params = executable.getGenericParameterTypes();
    Parameter[] parameters = executable.getParameters();
    List<B> bindings = new ArrayList<>();
    Map<TypeVariable<?>, Type> typeArguments = Types.getTypeArguments(ownerType, executable.getDeclaringClass());

    if(typeArguments == null) {
      throw new IllegalArgumentException("ownerType must be assignable to declaring class: " + ownerType + "; declaring class: " + executable.getDeclaringClass());
    }

    for(int i = 0; i < parameters.length; i++) {
      Type type = Types.resolveVariables(typeArguments, params[i]);

      bindings.add(factory.create(type, parameters[i]));
    }

    return bindings;
  }

  private B ownerBinding(Type ownerType) throws DefinitionException {
    return factory.create(ownerType, null);
  }
}
