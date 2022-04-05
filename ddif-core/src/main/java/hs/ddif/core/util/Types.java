package hs.ddif.core.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Support functions for types, classes and interfaces.
 */
public class Types {

  /**
   * Given a {@link Class}, returns a set of all classes and interfaces extended
   * or implemented.
   *
   * @param cls a {@link Class}, cannot be {@code null}
   * @return a set of all classes and interfaces extended or implemented, never {@code null}, never contains {@code null}s and never empty
   */
  public static Set<Class<?>> getSuperTypes(Class<?> cls) {
    Deque<Class<?>> toScan = new ArrayDeque<>();
    Set<Class<?>> superTypes = new HashSet<>();

    toScan.add(cls);

    while(!toScan.isEmpty()) {
      Class<?> scanClass = toScan.remove();

      superTypes.add(scanClass);

      for(Class<?> iface : scanClass.getInterfaces()) {
        toScan.add(iface);
      }

      if(scanClass.getSuperclass() != null) {
        toScan.add(scanClass.getSuperclass());
      }
    }

    return superTypes;
  }

  /**
   * Given a {@link Type}, returns a set of all types extended or implemented.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @return a set of all types extended or implemented, never {@code null}, never contains {@code null}s and never empty
   */
  public static Set<Type> getGenericSuperTypes(Type type) {
    Deque<Type> toScan = new ArrayDeque<>();
    Set<Type> superTypes = new HashSet<>();

    toScan.add(type);

    Map<TypeVariable<?>, Type> typeArguments = type instanceof ParameterizedType ? TypeUtils.getTypeArguments((ParameterizedType)type) : new HashMap<>();

    while(!toScan.isEmpty()) {
      Type scanType = toScan.remove();
      Class<?> scanClass = raw(scanType);

      Type resolvedType = TypeUtils.unrollVariables(typeArguments, scanType);

      superTypes.add(resolvedType);

      for(Type iface : scanClass.getGenericInterfaces()) {
        if(iface instanceof ParameterizedType) {
          typeArguments.putAll(TypeUtils.getTypeArguments((ParameterizedType)iface));
        }

        toScan.add(iface);
      }

      Type superType = scanClass.getGenericSuperclass();

      if(superType != null) {
        if(superType instanceof ParameterizedType) {
          typeArguments.putAll(TypeUtils.getTypeArguments((ParameterizedType)superType));
        }

        toScan.add(superType);
      }
    }

    return superTypes;
  }

  /**
   * Create a new {@link ParameterizedType} with the given type arguments.
   *
   * @param raw a {@link Class} to parameterize, cannot be {@code null}
   * @param typeArguments an array of type arguments
   * @return a {@link ParameterizedType}, never {@code null}
   */
  public static ParameterizedType parameterize(Class<?> raw, Type... typeArguments) {
    return TypeUtils.parameterize(raw, typeArguments);
  }

  /**
   * Creates a new {@link WildcardType} with the given upper bounds. Note that
   * the Java specification does not allow wildcard types with multiple upper
   * bounds to be created in source code, however you can create them
   * programmatically in order to for example filter by types that must implement
   * two interfaces.
   *
   * @param upperBounds an array of {@link Type}s, cannot be {@code null} or contain {@code null}s
   * @return a {@link WildcardType}, never {@code null}
   */
  public static WildcardType wildcardExtends(Type... upperBounds) {
    return TypeUtils.wildcardType().withUpperBounds(upperBounds).build();
  }

  /**
   * Gets the raw type of a Java {@link Type}. This works for classes and parameterized
   * types. Returns {@code null} if no raw type can be derived.
   *
   * @param <T> the expected type
   * @param type a {@link Type}, cannot be {@code null}
   * @return a {@link Class} representing the raw type of the given type, can be {@code null}
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> raw(Type type) {
    return (Class<T>)TypeUtils.getRawType(type, null);
  }

  /**
   * Checks if any type parameters for the given {@link Type} are bound to variables.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @return {@code true} if any parameters are bound to variables, otherwise {@code false}
   */
  public static boolean containsTypeVariables(Type type) {
    return TypeUtils.containsTypeVariables(type);
  }

  /**
   * Gets the {@link Type} of a generic parameter, identified by given by the {@link TypeVariable},
   * of the given {@link Class} when resolved against the given {@link Type}.
   *
   * @param type a {@link Type} from which to determine the parameters of the given {@link Class}, cannot be {@code null}
   * @param cls a {@link Class} to determine a type parameter for, cannot be {@code null}
   * @param typeVariable a {@link TypeVariable} of the given {@link Class} to extract, cannot be {@code null}
   * @return a {@link Type}, can be {@code null} if the {@link TypeVariable} was not associated with the given {@link Class}
   */
  public static Type getTypeParameter(Type type, Class<?> cls, TypeVariable<?> typeVariable) {
    return TypeUtils.getTypeArguments(type, cls).get(typeVariable);
  }

  /**
   * Gets the type arguments of a class/interface based on a subtype.
   *
   * <p>This method returns {@code null} if {@code type} is not assignable to
   * {@code toClass}. It returns an empty map if none of the classes or
   * interfaces in its inheritance hierarchy specify any type arguments.
   *
   * @param type a type from which to determine the type parameters of {@code toClass}, cannot be {@code null}
   * @param toClass a class whose type parameters are to be determined based on the subtype {@code type}, cannot be {@code null}
   * @return a map of the type assignments for the type variables in each type in the inheritance hierarchy from {@code type} to {@code toClass} inclusive or {@code null}
   */
  public static Map<TypeVariable<?>, Type> getTypeArguments(Type type, Class<?> toClass) {
    return TypeUtils.getTypeArguments(type, toClass);
  }

  /**
   * Get a type representing {@code type} with variable assignments resolved.
   *
   * @param typeArguments as from {@link Types#getTypeArguments(Type, Class)}, cannot be {@code null}
   * @param type a type to unroll variable assignments for, cannot be {@code null}
   * @return a {@link Type} or {@code null} when not all variables can be resolved
   */
  public static Type resolveVariables(Map<TypeVariable<?>, Type> typeArguments, Type type) {
    return TypeUtils.unrollVariables(typeArguments, type);
  }

  /**
   * Checks if the subject type may be implicitly cast to the target type
   * following Java generics rules. If both types are {@link Class} objects, the method
   * returns the result of {@link ClassUtils#isAssignable(Class, Class)}.
   *
   * @param type the subject type to be assigned to the target type, cannot be {@code null}
   * @param toType the target type, cannot be {@code null}
   * @return {@code true} if {@code type} is assignable to {@code toType}
   */
  public static boolean isAssignable(Type type, Type toType) {
    return TypeUtils.isAssignable(type, toType);
  }

  /**
   * Returns the upper bounds of the given {@link WildcardType}.
   *
   * @param type a {@link WildcardType}, cannot be {@code null}
   * @return a non-empty array containing the upper bounds of the wildcard type, never {@code null}
   */
  public static Type[] getUpperBounds(WildcardType type) {
    return TypeUtils.getImplicitUpperBounds(type);
  }
}
