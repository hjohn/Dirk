package hs.ddif.core.definition.bind;

import hs.ddif.core.store.Key;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Provides {@link Binding}s for constructors, methods and fields.
 */
public class BindingProvider {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);

  /**
   * Returns all bindings for the given {@link Constructor} and all member bindings
   * for the given class.
   *
   * @param constructor a {@link Constructor} to examine for bindings, cannot be {@code null}
   * @param cls a {@link Class} to examine for bindings, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   * @throws BindingException when an exception occurred while creating a binding
   */
  public List<Binding> ofConstructorAndMembers(Constructor<?> constructor, Class<?> cls) throws BindingException {
    List<Binding> bindings = ofConstructor(constructor);

    bindings.addAll(ofMembers(cls));

    return bindings;
  }

  /**
   * Returns all bindings for the given {@link Constructor}.
   *
   * @param constructor a {@link Constructor} to examine for bindings, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   */
  public List<Binding> ofConstructor(Constructor<?> constructor) {
    return ofExecutable(constructor, constructor.getDeclaringClass());
  }

  /**
   * Returns all member bindings for the given class. These are inject annotated
   * methods and fields, but not constructors.
   *
   * @param cls a {@link Class} to examine for bindings, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}, but can be empty
   * @throws BindingException when an exception occurred while creating a binding
   */
  public List<Binding> ofMembers(Class<?> cls) throws BindingException {
    List<Binding> bindings = new ArrayList<>();
    Class<?> currentInjectableClass = cls;
    Map<TypeVariable<?>, Type> typeArguments = null;

    while(currentInjectableClass != null) {
      for(final Field field : currentInjectableClass.getDeclaredFields()) {
        if(field.isAnnotationPresent(Inject.class)) {
          if(Modifier.isFinal(field.getModifiers())) {
            throw new BindingException(cls, field, "cannot be final");
          }

          if(typeArguments == null) {
            typeArguments = TypeUtils.getTypeArguments(cls, currentInjectableClass);  // pretty sure that you can re-use these even for when are examining fields of a super class later

            if(typeArguments == null) {
              throw new IllegalArgumentException("ownerType must be assignable to field's declaring class: " + cls + "; declaring class: " + currentInjectableClass);
            }
          }

          Type type = TypeUtils.unrollVariables(typeArguments, field.getGenericType());

          bindings.add(new DefaultBinding(new Key(type, Annotations.findDirectlyMetaAnnotatedAnnotations(field, QUALIFIER)), field, null));
        }
      }

      currentInjectableClass = currentInjectableClass.getSuperclass();
    }

    for(Binding binding : bindings) {
      binding.getAccessibleObject().setAccessible(true);
    }

    return bindings;
  }

  /**
   * Returns all bindings for the given {@link Method}.
   *
   * @param method a {@link Method} to examine for bindings, cannot be {@code null}
   * @param ownerType a {@link Type} in which this method is declared, cannot be {@code null}
   * @return a list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   */
  public List<Binding> ofMethod(Method method, Type ownerType) {
    return ofExecutable(method, ownerType);
  }

  /**
   * Returns all bindings for the given {@link Field}.
   *
   * @param field a {@link Field} to examine for bindings, cannot be {@code null}
   * @param ownerType a {@link Type} in which this executable is declared, cannot be {@code null}
   * @return an immutable list of bindings, never {@code null} and never contains {@code null}s, but can be empty
   */
  public List<Binding> ofField(Field field, Type ownerType) {

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
   * @throws BindingException when an exception occurred while creating a binding
   */
  public Constructor<?> getAnnotatedConstructor(Class<?> cls) throws BindingException {
    return getConstructor(cls, true);
  }

  /**
   * Returns a {@link Constructor} suitable for injection. A public empty
   * constructor is considered suitable if no other constructors are annotated.
   *
   * @param cls a {@link Class}, cannot be {@code null}
   * @return a {@link Constructor} suitable for injection, never {@code null}
   * @throws BindingException when an exception occurred while creating a binding
   */
  public static Constructor<?> getConstructor(Class<?> cls) throws BindingException {
    return getConstructor(cls, false);
  }

  private static Constructor<?> getConstructor(Class<?> cls, boolean annotatedOnly) throws BindingException {
    Constructor<?> suitableConstructor = null;
    Constructor<?> defaultConstructor = null;

    for(Constructor<?> constructor : cls.getDeclaredConstructors()) {
      if(constructor.isAnnotationPresent(Inject.class)) {
        if(suitableConstructor != null) {
          throw new BindingException(cls, "cannot have multiple Inject annotated constructors");
        }

        suitableConstructor = constructor;
      }
      else if(!annotatedOnly && constructor.getParameterCount() == 0 && Modifier.isPublic(constructor.getModifiers())) {
        defaultConstructor = constructor;
      }
    }

    if(suitableConstructor == null && defaultConstructor == null) {
      throw new BindingException(cls, "should have at least one suitable constructor; annotate a constructor" + (annotatedOnly ? "" : " or provide an empty public constructor"));
    }

    return suitableConstructor == null ? defaultConstructor : suitableConstructor;
  }

  private static List<Binding> ofExecutable(Executable executable, Type ownerType) {
    Parameter[] parameters = executable.getParameters();
    List<Binding> bindings = new ArrayList<>();

    for(int i = 0; i < parameters.length; i++) {
      Type type = parameters[i].getParameterizedType();

      bindings.add(new DefaultBinding(new Key(type, Annotations.findDirectlyMetaAnnotatedAnnotations(parameters[i], QUALIFIER)), executable, parameters[i]));
    }

    if(!Modifier.isStatic(executable.getModifiers()) && executable instanceof Method) {
      // For a non-static method, the class itself is also a required binding:
      bindings.add(ownerBinding(ownerType));
    }

    return bindings;
  }

  private static Binding ownerBinding(Type ownerType) {
    return new DefaultBinding(new Key(ownerType), null, null);
  }

  private static class DefaultBinding implements Binding {
    private final Key key;
    private final AccessibleObject accessibleObject;
    private final Parameter parameter;

    /**
     * Constructs a new instance.
     *
     * @param key a {@link Key}, cannot be {@code null}
     * @param accessibleObject an {@link AccessibleObject}, can be {@code null}
     * @param parameter a {@link Parameter}, cannot be {@code null} for {@link java.lang.reflect.Executable}s and must be {@code null} otherwise
     */
    public DefaultBinding(Key key, AccessibleObject accessibleObject, Parameter parameter) {
      if(key == null) {
        throw new IllegalArgumentException("key cannot be null");
      }
      if(accessibleObject instanceof Executable && parameter == null) {
        throw new IllegalArgumentException("parameter cannot be null when accessibleObject is an instance of Executable");
      }
      if(!(accessibleObject instanceof Executable) && parameter != null) {
        throw new IllegalArgumentException("parameter must be null when accessibleObject is not an instance of Executable");
      }

      this.key = key;
      this.accessibleObject = accessibleObject;
      this.parameter = parameter;
    }

    @Override
    public Key getKey() {
      return key;
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
    public int hashCode() {
      return Objects.hash(accessibleObject, key, parameter);
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }
      if(obj == null || getClass() != obj.getClass()) {
        return false;
      }

      DefaultBinding other = (DefaultBinding)obj;

      return Objects.equals(key, other.key)
        && Objects.equals(accessibleObject, other.accessibleObject)
        && Objects.equals(parameter, other.parameter);
    }

    @Override
    public String toString() {
      if(accessibleObject instanceof Executable) {
        return "Parameter " + Arrays.asList(((Executable)accessibleObject).getParameters()).indexOf(parameter) + " of [" + accessibleObject + "]";
      }
      else if(accessibleObject != null) {
        return "Field [" + (getKey().getQualifiers().isEmpty() ? "" : getKey().getQualifiers().stream().map(Object::toString).collect(Collectors.joining(" ")) + " ") + ((Field)accessibleObject).toGenericString() + "]";
      }

      return "Owner Type [" + key.getType() + "]";
    }
  }
}