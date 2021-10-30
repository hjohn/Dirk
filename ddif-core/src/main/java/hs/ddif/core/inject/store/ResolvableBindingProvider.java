package hs.ddif.core.inject.store;

import hs.ddif.annotations.Opt;
import hs.ddif.annotations.Parameter;
import hs.ddif.core.bind.Key;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.RuntimeBeanResolutionException;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
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
import io.leangen.geantyref.TypeFactory;

public class ResolvableBindingProvider {

  /**
   * Returns all bindings for the given class.  Bindings are grouped by {@link AccessibleObject}
   * (fields, methods, constructors).  Fields only ever have a single binding.
   *
   * @param injectableClass a {@link Class} to examine for bindings, cannot be null
   * @return an immutable map of bindings, never null and never contains nulls, but can be empty
   */
  public static Map<AccessibleObject, List<ResolvableBinding>> ofClass(Class<?> injectableClass) {
    Map<AccessibleObject, List<ResolvableBinding>> bindings = new HashMap<>();
    Class<?> currentInjectableClass = injectableClass;

    while(currentInjectableClass != null) {
      for(final Field field : currentInjectableClass.getDeclaredFields()) {
        Inject inject = field.getAnnotation(Inject.class);

        if(inject != null) {
          Type type = GenericTypeReflector.getExactFieldType(field, injectableClass);

          bindings.put(field, List.of(createBinding(field, AbstractBinding.FIELD, type, isOptional(field.getAnnotations()), field.getAnnotation(Parameter.class) != null, extractQualifiers(field))));
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
  public static List<ResolvableBinding> ofExecutable(Executable executable, Type ownerType) {
    Annotation[][] parameterAnnotations = executable.getParameterAnnotations();
    java.lang.reflect.Parameter[] parameters = executable.getParameters();
    Type[] genericParameterTypes = executable.getGenericParameterTypes();
    List<ResolvableBinding> bindings = new ArrayList<>();

    for(int i = 0; i < genericParameterTypes.length; i++) {
      Type type = genericParameterTypes[i];
      ResolvableBinding binding = createBinding(executable, i, type, isOptional(parameterAnnotations[i]), parameters[i].getAnnotation(Parameter.class) != null, extractQualifiers(parameterAnnotations[i]));

      bindings.add(binding);
    }

    if(!Modifier.isStatic(executable.getModifiers()) && executable instanceof Method) {
      // For a non-static method, the class itself is also a required binding:
      bindings.add(createBinding(executable, AbstractBinding.DECLARING_CLASS, ownerType, false, false, Set.of()));
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
  public static List<ResolvableBinding> ofField(Field field, Type ownerType) {
    return Modifier.isStatic(field.getModifiers()) ? List.of() : List.of(createBinding(field, AbstractBinding.DECLARING_CLASS, ownerType, false, false, Set.of()));
  }

  private static ResolvableBinding createBinding(AccessibleObject accessibleObject, int argNo, Type type, boolean optional, boolean isParameter, Set<AnnotationDescriptor> qualifiers) {
    return createBinding(accessibleObject, argNo, false, type, optional, isParameter, qualifiers);
  }

  private static ResolvableBinding createBinding(AccessibleObject accessibleObject, int argNo, boolean isProviderAlready, Type type, boolean optional, boolean isParameter, Set<AnnotationDescriptor> qualifiers) {
    final Class<?> cls = TypeUtils.getRawType(type, null);

    if(!isParameter) {
      if(Set.class.isAssignableFrom(cls)) {
        Type elementType = TypeUtils.getTypeArguments(type, Set.class).get(Set.class.getTypeParameters()[0]);

        return new HashSetBinding(accessibleObject, argNo, elementType, qualifiers, optional);
      }
      if(List.class.isAssignableFrom(cls)) {
        Type elementType = TypeUtils.getTypeArguments(type, List.class).get(List.class.getTypeParameters()[0]);

        return new ArrayListBinding(accessibleObject, argNo, elementType, qualifiers, optional);
      }
      if(Provider.class.isAssignableFrom(cls) && !isProviderAlready) {
        Type providedType = TypeUtils.getTypeArguments(type, Provider.class).get(Provider.class.getTypeParameters()[0]);

        return new ProviderBinding(accessibleObject, argNo, createBinding(accessibleObject, argNo, true, providedType, false, false, qualifiers));
      }
    }

    Type finalType = type instanceof Class && ((Class<?>)type).isPrimitive() ? WRAPPER_CLASS_BY_PRIMITIVE_CLASS.get(type) : type;

    return new DirectBinding(accessibleObject, argNo, new Key(finalType, qualifiers), optional, isParameter);
  }

  private static Set<AnnotationDescriptor> extractQualifiers(Field field) {
    return extractQualifiers(field.getAnnotations());
  }

  private static Set<AnnotationDescriptor> extractQualifiers(Annotation[] annotations) {
    Set<AnnotationDescriptor> qualifiers = new HashSet<>();

    for(Annotation annotation : annotations) {
      if(annotation.annotationType().getAnnotation(Qualifier.class) != null) {
        qualifiers.add(new AnnotationDescriptor(annotation));
      }
    }

    return Collections.unmodifiableSet(qualifiers);
  }

  private static boolean isOptional(Annotation[] annotations) {
    for(Annotation annotation : annotations) {
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

  private static abstract class AbstractBinding implements ResolvableBinding {
    public static final int DECLARING_CLASS = -1;
    public static final int FIELD = -2;

    private final AccessibleObject accessibleObject;
    private final int argNo;
    private final Type type;

    AbstractBinding(AccessibleObject accessibleObject, int argNo, Type type) {
      this.accessibleObject = accessibleObject;
      this.argNo = argNo;
      this.type = type;
    }

    @Override
    public AccessibleObject getAccessibleObject() {
      return accessibleObject;
    }

    @Override
    public final Type getType() {
      return type;
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
    private final Set<AnnotationDescriptor> qualifiers;
    private final Type elementType;
    private final boolean optional;

    private HashSetBinding(AccessibleObject accessibleObject, int argNo, Type elementType, Set<AnnotationDescriptor> qualifiers, boolean optional) {
      super(accessibleObject, argNo, Set.class);

      this.qualifiers = qualifiers;
      this.elementType = elementType;
      this.optional = optional;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws BeanResolutionException {
      List<Object> instances = instantiator.getInstances(elementType, qualifiers.toArray());

      return instances.isEmpty() && optional ? null : new HashSet<>(instances);
    }

    @Override
    public Key getRequiredKey() {
      return null;
    }

    @Override
    public boolean isParameter() {
      return false;
    }
  }

  private static final class ArrayListBinding extends AbstractBinding {
    private final Type elementType;
    private final Set<AnnotationDescriptor> qualifiers;
    private final boolean optional;

    private ArrayListBinding(AccessibleObject accessibleObject, int argNo, Type elementType, Set<AnnotationDescriptor> qualifiers, boolean optional) {
      super(accessibleObject, argNo, List.class);

      this.elementType = elementType;
      this.qualifiers = qualifiers;
      this.optional = optional;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws BeanResolutionException {
      List<Object> instances = instantiator.getInstances(elementType, qualifiers.toArray());

      return instances.isEmpty() && optional ? null : instances;
    }

    @Override
    public Key getRequiredKey() {
      return null;
    }

    @Override
    public boolean isParameter() {
      return false;
    }
  }

  private static final class ProviderBinding extends AbstractBinding {
    private final ResolvableBinding binding;

    private ProviderBinding(AccessibleObject accessibleObject, int argNo, ResolvableBinding binding) {
      super(accessibleObject, argNo, binding.getType());

      this.binding = binding;
    }

    @Override
    public Object getValue(final Instantiator instantiator) {

      /*
       * When supplying a Provider<X>, check if such a provider is implemented by a concrete class first, otherwise
       * create one.
       */

      try {
        if(binding.getRequiredKey() != null) {
          Type searchType = TypeFactory.parameterizedClass(Provider.class, binding.getRequiredKey().getType());

          return instantiator.getInstance(searchType, (Object[])binding.getRequiredKey().getQualifiersAsArray());
        }
      }
      catch(BeanResolutionException e) {
        // Ignore, create Provider on demand below
      }

      return new Provider<>() {
        @Override
        public Object get() {
          try {
            return binding.getValue(instantiator);
          }
          catch(BeanResolutionException e) {
            throw new RuntimeBeanResolutionException(binding.getType(), e);
          }
        }
      };
    }

    @Override
    public Key getRequiredKey() {
      return null;  // nothing required, as providers are used to break cyclical dependencies
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
      super(accessibleObject, argNo, requiredKey.getType());

      this.key = requiredKey;
      this.optional = optional;
      this.isParameter = isParameter;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws BeanResolutionException {
      if(optional) {
        try {
          return instantiator.getInstance(key.getType(), (Object[])key.getQualifiersAsArray());
        }
        catch(BeanResolutionException e) {
          return null;
        }
      }
      else {
        return instantiator.getInstance(key.getType(), (Object[])key.getQualifiersAsArray());
      }
    }

    @Override
    public Key getRequiredKey() {
      return optional || isParameter ? null : key;
    }

    @Override
    public boolean isParameter() {
      return isParameter;
    }
  }
}
