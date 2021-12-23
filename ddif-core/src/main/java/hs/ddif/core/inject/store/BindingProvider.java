package hs.ddif.core.inject.store;

import hs.ddif.annotations.Opt;
import hs.ddif.annotations.Parameter;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.InstanceResolutionFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.Key;
import hs.ddif.core.inject.instantiator.MultipleInstances;
import hs.ddif.core.inject.instantiator.NoSuchInstance;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;

import org.apache.commons.lang3.reflect.TypeUtils;

import io.leangen.geantyref.GenericTypeReflector;

public class BindingProvider {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);

  /**
   * Returns all bindings for the given class.  Bindings are grouped by {@link AccessibleObject}
   * (fields, methods, constructors).  Fields only ever have a single binding.
   *
   * @param injectableClass a {@link Class} to examine for bindings, cannot be null
   * @return an immutable map of bindings, never null and never contains nulls, but can be empty
   */
  public static Map<AccessibleObject, List<Binding>> ofClass(Class<?> injectableClass) {
    Map<AccessibleObject, List<Binding>> bindings = new HashMap<>();
    Class<?> currentInjectableClass = injectableClass;

    while(currentInjectableClass != null) {
      for(final Field field : currentInjectableClass.getDeclaredFields()) {
        Inject inject = field.getAnnotation(Inject.class);

        if(inject != null) {
          Type type = GenericTypeReflector.getExactFieldType(field, injectableClass);

          bindings.put(field, List.of(createBinding(
            field,
            AbstractBinding.FIELD,
            type,
            isOptional(field),
            field.getAnnotation(Parameter.class) != null,
            Annotations.findDirectlyMetaAnnotatedAnnotations(field, QUALIFIER)
          )));
        }
      }

      currentInjectableClass = currentInjectableClass.getSuperclass();
    }

    Constructor<?> emptyConstructor = null;
    Constructor<?>[] constructors = injectableClass.getDeclaredConstructors();
    boolean foundInjectableConstructor = false;

    // Finds empty public constructor or any annotated ones regardless of visibility
    for(Constructor<?> constructor : constructors) {
      Inject inject = constructor.getAnnotation(Inject.class);

      if(constructor.getParameterTypes().length == 0 && Modifier.isPublic(constructor.getModifiers())) {
        emptyConstructor = constructor;
      }

      if(inject != null) {
        foundInjectableConstructor = true;
        bindings.put(constructor, ofExecutable(constructor, injectableClass));
      }
    }

    if(!foundInjectableConstructor && emptyConstructor != null) {
      bindings.put(emptyConstructor, ofExecutable(emptyConstructor, injectableClass));
    }

    for(AccessibleObject accessibleObject : bindings.keySet()) {
      accessibleObject.setAccessible(true);
    }

    return Collections.unmodifiableMap(bindings);
  }

  /**
   * Returns all bindings for the given {@link Executable} (method or constructor).
   *
   * @param executable a {@link Executable} to examine for bindings, cannot be null
   * @param ownerType a {@link Type} in which this executable is declared, cannot be null
   * @return an immutable list of bindings, never null and never contains nulls, but can be empty
   */
  public static List<Binding> ofExecutable(Executable executable, Type ownerType) {
    java.lang.reflect.Parameter[] parameters = executable.getParameters();
    Type[] genericParameterTypes = executable.getGenericParameterTypes();
    List<Binding> bindings = new ArrayList<>();

    for(int i = 0; i < genericParameterTypes.length; i++) {
      Type type = genericParameterTypes[i];
      Binding binding = createBinding(
        executable,
        i,
        type,
        isOptional(parameters[i]),
        parameters[i].getAnnotation(Parameter.class) != null,
        Annotations.findDirectlyMetaAnnotatedAnnotations(parameters[i], QUALIFIER)
      );

      bindings.add(binding);
    }

    if(!Modifier.isStatic(executable.getModifiers()) && executable instanceof Method) {
      // For a non-static method, the class itself is also a required binding:
      bindings.add(new DirectBinding(executable, AbstractBinding.DECLARING_CLASS, new Key(ownerType, Set.of()), false, false));
    }

    return Collections.unmodifiableList(bindings);
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

    return Modifier.isStatic(field.getModifiers()) ? List.of() : List.of(new DirectBinding(field, AbstractBinding.DECLARING_CLASS, new Key(ownerType, Set.of()), false, false));
  }

  private static Binding createBinding(AccessibleObject accessibleObject, int argNo, Type type, boolean optional, boolean isParameter, Set<Annotation> qualifiers) {
    return createBinding(accessibleObject, argNo, false, type, optional, isParameter, qualifiers);
  }

  private static Binding createBinding(AccessibleObject accessibleObject, int argNo, boolean isProviderAlready, Type type, boolean optional, boolean isParameter, Set<Annotation> qualifiers) {
    final Class<?> cls = TypeUtils.getRawType(type, null);

    if(!isParameter) {
      if(Set.class.isAssignableFrom(cls)) {
        return new HashSetBinding(accessibleObject, argNo, getFirstTypeParameter(type, Set.class), qualifiers, optional);
      }
      if(List.class.isAssignableFrom(cls)) {
        return new ArrayListBinding(accessibleObject, argNo, getFirstTypeParameter(type, List.class), qualifiers, optional);
      }
      if(Provider.class.isAssignableFrom(cls) && !isProviderAlready) {
        return new ProviderBinding(accessibleObject, argNo, createBinding(accessibleObject, argNo, true, getFirstTypeParameter(type, Provider.class), false, false, qualifiers));
      }
    }

    Type finalType = type instanceof Class && ((Class<?>)type).isPrimitive() ? WRAPPER_CLASS_BY_PRIMITIVE_CLASS.get(type) : type;

    return new DirectBinding(accessibleObject, argNo, new Key(finalType, qualifiers), optional, isParameter);
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

  private static final Map<Class<?>, Class<?>> WRAPPER_CLASS_BY_PRIMITIVE_CLASS = new HashMap<>();

  static {
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(boolean.class, Boolean.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(byte.class, Byte.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(short.class, Short.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(char.class, Character.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(int.class, Integer.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(long.class, Long.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(float.class, Float.class);
    WRAPPER_CLASS_BY_PRIMITIVE_CLASS.put(double.class, Double.class);
  }

  private static abstract class AbstractBinding implements Binding {
    public static final int DECLARING_CLASS = -1;
    public static final int FIELD = -2;

    private final AccessibleObject accessibleObject;
    private final int argNo;
    private final Key key;

    AbstractBinding(AccessibleObject accessibleObject, int argNo, Key key) {
      this.accessibleObject = accessibleObject;
      this.argNo = argNo;
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
    public String toString() {
      if(argNo == DECLARING_CLASS) {
        return "Declaring Class of [" + accessibleObject.toString() + "]";
      }
      if(accessibleObject instanceof Executable) {
        return "Parameter " + argNo + " of [" + accessibleObject.toString() + "]";
      }

      return "Field [" + accessibleObject.toString() + "]";
    }
  }

  private static final class HashSetBinding extends AbstractBinding {
    private final Set<Annotation> qualifiers;
    private final Type elementType;
    private final boolean optional;

    private HashSetBinding(AccessibleObject accessibleObject, int argNo, Type elementType, Set<Annotation> qualifiers, boolean optional) {
      super(accessibleObject, argNo, new Key(TypeUtils.parameterize(Set.class, elementType), Set.of()));

      this.qualifiers = qualifiers;
      this.elementType = elementType;
      this.optional = optional;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws InstanceCreationFailure {
      List<Object> instances = instantiator.getInstances(elementType, qualifiers.toArray());

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

    @Override
    public boolean isParameter() {
      return false;
    }
  }

  private static final class ArrayListBinding extends AbstractBinding {
    private final Type elementType;
    private final Set<Annotation> qualifiers;
    private final boolean optional;

    private ArrayListBinding(AccessibleObject accessibleObject, int argNo, Type elementType, Set<Annotation> qualifiers, boolean optional) {
      super(accessibleObject, argNo, new Key(TypeUtils.parameterize(List.class, elementType), Set.of()));

      this.elementType = elementType;
      this.qualifiers = qualifiers;
      this.optional = optional;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws InstanceCreationFailure {
      List<Object> instances = instantiator.getInstances(elementType, qualifiers.toArray());

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

    @Override
    public boolean isParameter() {
      return false;
    }
  }

  private static final class ProviderBinding extends AbstractBinding {
    private final Binding binding;

    private ProviderBinding(AccessibleObject accessibleObject, int argNo, Binding binding) {
      super(accessibleObject, argNo, binding.getKey());

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
      return true;
    }

    @Override
    public boolean isCollection() {
      return false;
    }

    @Override
    public boolean isDirect() {
      return false;
    }

    @Override
    public boolean isParameter() {
      return false;
    }
  }

  private static final class DirectBinding extends AbstractBinding {
    private final Key key;
    private final boolean optional;
    private final boolean isParameter;

    private DirectBinding(AccessibleObject accessibleObject, int argNo, Key requiredKey, boolean optional, boolean isParameter) {
      super(accessibleObject, argNo, requiredKey);

      this.key = requiredKey;
      this.optional = optional;
      this.isParameter = isParameter;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws InstanceCreationFailure, NoSuchInstance, MultipleInstances, OutOfScopeException {
      if(optional) {
        return instantiator.findInstance(key.getType(), (Object[])key.getQualifiersAsArray());
      }

      return instantiator.getInstance(key.getType(), (Object[])key.getQualifiersAsArray());
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

    @Override
    public boolean isParameter() {
      return isParameter;
    }
  }
}
