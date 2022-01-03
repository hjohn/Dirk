package hs.ddif.core.inject.store;

import hs.ddif.annotations.Opt;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.InstanceResolutionFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.MultipleInstances;
import hs.ddif.core.inject.instantiator.NoSuchInstance;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.store.Key;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;

import org.apache.commons.lang3.reflect.TypeUtils;

import io.leangen.geantyref.GenericTypeReflector;

/**
 * Provides {@link Binding}s for constructors, methods and fields.
 */
public class BindingProvider {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);

  /**
   * Returns all bindings for the given {@link Constructor} and all member bindings
   * for the given class.
   *
   * @param constructor a {@link Constructor} to examine for bindings, cannot be null
   * @param cls a {@link Class} to examine for bindings, cannot be null
   * @return a list of bindings, never null and never contains nulls, but can be empty
   */
  public static List<Binding> ofConstructorAndMembers(Constructor<?> constructor, Class<?> cls) {
    List<Binding> bindings = ofConstructor(constructor);

    bindings.addAll(ofMembers(cls));

    return bindings;
  }

  /**
   * Returns all bindings for the given {@link Constructor}.
   *
   * @param constructor a {@link Constructor} to examine for bindings, cannot be null
   * @return a list of bindings, never null and never contains nulls, but can be empty
   */
  public static List<Binding> ofConstructor(Constructor<?> constructor) {
    return ofExecutable(constructor, constructor.getDeclaringClass());
  }

  /**
   * Returns all member bindings for the given class. These are inject annotated
   * methods and fields, but not constructors.
   *
   * @param cls a {@link Class} to examine for bindings, cannot be null
   * @return a list of bindings, never null and never contains nulls, but can be empty
   */
  public static List<Binding> ofMembers(Class<?> cls) {
    List<Binding> bindings = new ArrayList<>();
    Class<?> currentInjectableClass = cls;

    while(currentInjectableClass != null) {
      for(final Field field : currentInjectableClass.getDeclaredFields()) {
        if(field.isAnnotationPresent(Inject.class)) {
          if(Modifier.isFinal(field.getModifiers())) {
            throw new BindingException("Cannot inject final field: " + field + " in: " + cls);
          }

          Type type = GenericTypeReflector.getExactFieldType(field, cls);

          try {
            bindings.add(createBinding(
              field,
              null,
              type,
              isOptional(field),
              Annotations.findDirectlyMetaAnnotatedAnnotations(field, QUALIFIER)
            ));
          }
          catch(BindingException e) {
            throw new BindingException("Unable to create binding for: " + field + " in: " + cls, e);
          }
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
   * @param method a {@link Method} to examine for bindings, cannot be null
   * @param ownerType a {@link Type} in which this method is declared, cannot be null
   * @return a list of bindings, never null and never contains nulls, but can be empty
   */
  public static List<Binding> ofMethod(Method method, Type ownerType) {
    return ofExecutable(method, ownerType);
  }

  /**
   * Returns all bindings for the given {@link Field}.
   *
   * @param field a {@link Field} to examine for bindings, cannot be null
   * @param ownerType a {@link Type} in which this executable is declared, cannot be null
   * @return an immutable list of bindings, never null and never contains nulls, but can be empty
   */
  public static List<Binding> ofField(Field field, Type ownerType) {

    /*
     * Fields don't have any bindings, unless it is a non-static field in which case the
     * declaring class is required before the field can be accessed.
     */

    return Modifier.isStatic(field.getModifiers()) ? List.of() : List.of(new OwnerBinding(ownerType));
  }

  /**
   * Returns an annotated {@link Constructor} suitable for injection.
   *
   * @param cls a {@link Class}, cannot be null
   * @return a {@link Constructor} suitable for injection, never null
   * @throws BindingException when no suitable constructor is found
   */
  public static Constructor<?> getAnnotatedConstructor(Class<?> cls) {
    return getConstructor(cls, true);
  }

  /**
   * Returns a {@link Constructor} suitable for injection. A public empty
   * constructor is considered suitable if no other constructors are annotated.
   *
   * @param cls a {@link Class}, cannot be null
   * @return a {@link Constructor} suitable for injection, never null
   * @throws BindingException when no suitable constructor is found
   */
  public static Constructor<?> getConstructor(Class<?> cls) {
    return getConstructor(cls, false);
  }

  private static Constructor<?> getConstructor(Class<?> cls, boolean annotatedOnly) {
    Constructor<?> suitableConstructor = null;
    Constructor<?> defaultConstructor = null;

    for(Constructor<?> constructor : cls.getDeclaredConstructors()) {
      if(constructor.isAnnotationPresent(Inject.class)) {
        if(suitableConstructor != null) {
          throw new BindingException("Multiple @Inject annotated constructors found, but only one allowed: " + cls);
        }

        suitableConstructor = constructor;
      }
      else if(!annotatedOnly && constructor.getParameterCount() == 0 && Modifier.isPublic(constructor.getModifiers())) {
        defaultConstructor = constructor;
      }
    }

    if(suitableConstructor == null && defaultConstructor == null) {
      throw new BindingException("No suitable constructor found; annotate a constructor" + (annotatedOnly ? "" : " or provide an empty public constructor") + ": " + cls);
    }

    return suitableConstructor == null ? defaultConstructor : suitableConstructor;
  }

  private static List<Binding> ofExecutable(Executable executable, Type ownerType) {
    Parameter[] parameters = executable.getParameters();
    Type[] genericParameterTypes = executable.getGenericParameterTypes();
    List<Binding> bindings = new ArrayList<>();

    for(int i = 0; i < genericParameterTypes.length; i++) {
      Type type = genericParameterTypes[i];

      try {
        Binding binding = createBinding(
          executable,
          parameters[i],
          type,
          isOptional(parameters[i]),
          Annotations.findDirectlyMetaAnnotatedAnnotations(parameters[i], QUALIFIER)
        );

        bindings.add(binding);
      }
      catch(BindingException e) {
        throw new BindingException("Unable to create binding for Parameter " + i + " [" + type + " " + parameters[i].getName() + "] of: " + executable + " in: " + ownerType, e);
      }
    }

    if(!Modifier.isStatic(executable.getModifiers()) && executable instanceof Method) {
      // For a non-static method, the class itself is also a required binding:
      bindings.add(new OwnerBinding(ownerType));
    }

    return bindings;
  }

  private static Binding createBinding(AccessibleObject accessibleObject, Parameter parameter, Type type, boolean optional, Set<Annotation> qualifiers) {
    return createBinding(accessibleObject, parameter, false, type, optional, qualifiers);
  }

  private static Binding createBinding(AccessibleObject accessibleObject, Parameter parameter, boolean isProviderAlready, Type type, boolean optional, Set<Annotation> qualifiers) {
    final Class<?> cls = TypeUtils.getRawType(type, null);

    if(Set.class.isAssignableFrom(cls)) {
      return new HashSetBinding(accessibleObject, parameter, requireNotProvider(getFirstTypeParameter(type, Set.class)), qualifiers, optional);
    }
    if(List.class.isAssignableFrom(cls)) {
      return new ArrayListBinding(accessibleObject, parameter, requireNotProvider(getFirstTypeParameter(type, List.class)), qualifiers, optional);
    }
    if(Provider.class.isAssignableFrom(cls) && !isProviderAlready) {
      return new ProviderBinding(accessibleObject, parameter, createBinding(accessibleObject, parameter, true, requireNotProvider(getFirstTypeParameter(type, Provider.class)), optional, qualifiers));
    }

    return new DirectBinding(accessibleObject, parameter, new Key(type, qualifiers), optional);
  }

  private static Type requireNotProvider(Type type) {
    if(TypeUtils.getRawType(type, null) == Provider.class) {
      throw new BindingException("Nested Provider not allowed: " + type);
    }

    return type;
  }

  private static Type getFirstTypeParameter(Type type, Class<?> cls) {
    return TypeUtils.getTypeArguments(type, cls).get(cls.getTypeParameters()[0]);
  }

  private static boolean isOptional(AnnotatedElement element) {
    for(Annotation annotation : element.getAnnotations()) {
      String simpleName = annotation.annotationType().getSimpleName();

      if(simpleName.equals("Nullable") || annotation.annotationType().equals(Opt.class)) {
        return true;
      }
    }

    return false;
  }

  private static abstract class AbstractBinding implements Binding {
    private final AccessibleObject accessibleObject;
    private final Parameter parameter;
    private final Key key;

    AbstractBinding(AccessibleObject accessibleObject, Parameter parameter, Key key) {
      this.accessibleObject = accessibleObject;
      this.parameter = parameter;
      this.key = key;
    }

    @Override
    public AccessibleObject getAccessibleObject() {
      return accessibleObject;
    }

    @Override
    public Key getKey() {
      return key;
    }

    @Override
    public Parameter getParameter() {
      return parameter;
    }

    @Override
    public String toString() {
      if(accessibleObject instanceof Executable) {
        return "Parameter " + Arrays.asList(((Executable)accessibleObject).getParameters()).indexOf(parameter) + " of [" + accessibleObject + "]";
      }

      return "Field [" + accessibleObject + "]";
    }
  }

  private static final class HashSetBinding extends AbstractBinding {
    private final Key elementKey;
    private final boolean optional;

    private HashSetBinding(AccessibleObject accessibleObject, Parameter parameter, Type elementType, Set<Annotation> qualifiers, boolean optional) {
      super(accessibleObject, parameter, new Key(TypeUtils.parameterize(Set.class, elementType), qualifiers));

      this.elementKey = new Key(elementType, qualifiers);
      this.optional = optional;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws InstanceCreationFailure {
      List<Object> instances = instantiator.getInstances(elementKey);

      return instances.isEmpty() && optional ? null : new HashSet<>(instances);
    }

    @Override
    public boolean isOptional() {
      return true;
    }

    @Override
    public boolean isCollection() {
      return true;
    }

    @Override
    public boolean isDirect() {
      return true;
    }
  }

  private static final class ArrayListBinding extends AbstractBinding {
    private final Key elementKey;
    private final boolean optional;

    private ArrayListBinding(AccessibleObject accessibleObject, Parameter parameter, Type elementType, Set<Annotation> qualifiers, boolean optional) {
      super(accessibleObject, parameter, new Key(TypeUtils.parameterize(List.class, elementType), qualifiers));

      this.elementKey = new Key(elementType, qualifiers);
      this.optional = optional;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws InstanceCreationFailure {
      List<Object> instances = instantiator.getInstances(elementKey);

      return instances.isEmpty() && optional ? null : instances;
    }

    @Override
    public boolean isOptional() {
      return true;
    }

    @Override
    public boolean isCollection() {
      return true;
    }

    @Override
    public boolean isDirect() {
      return true;
    }
  }

  private static final class ProviderBinding extends AbstractBinding {
    private final Binding binding;

    private ProviderBinding(AccessibleObject accessibleObject, Parameter parameter, Binding binding) {
      super(accessibleObject, parameter, binding.getKey());

      this.binding = binding;
    }

    @Override
    public Object getValue(final Instantiator instantiator) throws MultipleInstances, InstanceCreationFailure, OutOfScopeException {

      /*
       * Although it is possible to attempt to inject an external Provider that is known, it is currently hard
       * to find the correct instance when qualifiers on the provided type are in play. The store does not
       * allow searching for a Provider<@Qualifier X>, and searching for @Qualifier Provider<X> (which previous
       * implementations did) is not at all the same thing.
       *
       * To allow for this kind of matching one could instead look for the provided type directly, then use a
       * custom Matcher. The Matcher interface would need to expose more information (Injectable instead of just
       * the Class), and Injectable would have to expose its owner type. In that case a Matcher could check the
       * method (see if it is called "get") and the owner type (see if it is a Provider).
       *
       * However, injecting an externally supplied Provider has one disadvantage: a badly behaving provider (which
       * returns null) can't be prevented. The wrapper approach (which is at the moment always used) can however
       * check for nulls and throw an exception.
       */

      return new Provider<>() {
        @Override
        public Object get() {
          try {
            return binding.getValue(instantiator);
          }
          catch(InstanceResolutionFailure f) {
            throw f.toRuntimeException();
          }
          catch(OutOfScopeException e) {
            throw new NoSuchInstanceException(e.getMessage(), e);
          }
        }
      };
    }

    @Override
    public boolean isOptional() {
      return binding.isOptional();
    }

    @Override
    public boolean isCollection() {
      return false;
    }

    @Override
    public boolean isDirect() {
      return false;
    }
  }

  private static final class DirectBinding extends AbstractBinding {
    private final Key key;
    private final boolean optional;

    private DirectBinding(AccessibleObject accessibleObject, Parameter parameter, Key requiredKey, boolean optional) {
      super(accessibleObject, parameter, requiredKey);

      this.key = requiredKey;
      this.optional = optional;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws InstanceCreationFailure, NoSuchInstance, MultipleInstances, OutOfScopeException {
      if(optional) {
        return instantiator.findInstance(key);
      }

      return instantiator.getInstance(key);
    }

    @Override
    public boolean isOptional() {
      return optional;
    }

    @Override
    public boolean isCollection() {
      return false;
    }

    @Override
    public boolean isDirect() {
      return true;
    }
  }

  private static final class OwnerBinding implements Binding {
    private final Key key;
    private final Type ownerType;

    private OwnerBinding(Type ownerType) {
      this.key = new Key(ownerType);
      this.ownerType = ownerType;
    }

    @Override
    public Key getKey() {
      return key;
    }

    @Override
    public AccessibleObject getAccessibleObject() {
      return null;
    }

    @Override
    public Parameter getParameter() {
      return null;
    }

    @Override
    public boolean isCollection() {
      return false;
    }

    @Override
    public boolean isDirect() {
      return true;
    }

    @Override
    public boolean isOptional() {
      return false;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance, OutOfScopeException {
      return instantiator.getInstance(key);
    }

    @Override
    public String toString() {
      return "Owner Type [" + ownerType + "]";
    }
  }
}
