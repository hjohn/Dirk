package hs.ddif.core.inject.store;

import hs.ddif.core.bind.Key;
import hs.ddif.core.bind.Parameter;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;

public class ResolvableBindingProvider {

  // Binding array is returned because a constructor has 0 or more bindings, although fields always only have a single binding
  public static Map<AccessibleObject, ResolvableBinding[]> ofClass(Class<?> injectableClass) {
    Map<AccessibleObject, ResolvableBinding[]> bindings = new HashMap<>();
    Class<?> currentInjectableClass = injectableClass;

    while(currentInjectableClass != null) {
      for(final Field field : currentInjectableClass.getDeclaredFields()) {
        Inject inject = field.getAnnotation(Inject.class);

        if(inject != null) {
          Type type = GenericTypeReflector.getExactFieldType(field, injectableClass);

          bindings.put(field, new ResolvableBinding[] {createBinding(type, isOptional(field.getAnnotations()), field.getAnnotation(Parameter.class) != null, extractQualifiers(field))});
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
        bindings.put(constructor, ofExecutable(constructor));
      }
    }

    if(!foundInjectableConstructor && emptyConstructor != null) {
      bindings.put(emptyConstructor, ofExecutable(emptyConstructor));
    }

    for(AccessibleObject accessibleObject : bindings.keySet()) {
      accessibleObject.setAccessible(true);
    }

    return bindings;
  }

  public static ResolvableBinding[] ofExecutable(Executable executable) {
    Annotation[][] parameterAnnotations = executable.getParameterAnnotations();
    java.lang.reflect.Parameter[] parameters = executable.getParameters();
    Type[] genericParameterTypes = executable.getGenericParameterTypes();
    List<ResolvableBinding> bindings = new ArrayList<>();

    for(int i = 0; i < genericParameterTypes.length; i++) {
      Type type = genericParameterTypes[i];
      AnnotationDescriptor[] qualifiers = extractQualifiers(parameterAnnotations[i]);
      ResolvableBinding binding = createBinding(type, isOptional(parameterAnnotations[i]), parameters[i].getAnnotation(Parameter.class) != null, qualifiers);

      bindings.add(binding);
    }

    return bindings.toArray(new ResolvableBinding[bindings.size()]);
  }

  private static ResolvableBinding createBinding(Type type, boolean optional, boolean isParameter, AnnotationDescriptor... qualifiers) {
    return createBinding(false, type, optional, isParameter, qualifiers);
  }

  private static ResolvableBinding createBinding(boolean isProviderAlready, Type type, boolean optional, boolean isParameter, AnnotationDescriptor... qualifiers) {
    final Class<?> cls = TypeUtils.determineClassFromType(type);

    if(!isParameter) {
      if(Set.class.isAssignableFrom(cls)) {
        return new HashSetBinding(TypeUtils.getGenericType(type), qualifiers, optional);
      }
      if(List.class.isAssignableFrom(cls)) {
        return new ArrayListBinding(TypeUtils.getGenericType(type), qualifiers, optional);
      }
      if(Provider.class.isAssignableFrom(cls) && !isProviderAlready) {
        return new ProviderBinding(createBinding(true, TypeUtils.getGenericType(type), false, false, qualifiers));
      }
    }

    Type finalType = type instanceof Class && ((Class<?>)type).isPrimitive() ? WRAPPER_CLASS_BY_PRIMITIVE_CLASS.get(type) : type;

    return new DirectBinding(new Key(finalType, qualifiers), optional, isParameter);
  }

  private static AnnotationDescriptor[] extractQualifiers(Field field) {
    return extractQualifiers(field.getAnnotations());
  }

  private static AnnotationDescriptor[] extractQualifiers(Annotation[] annotations) {
    Set<AnnotationDescriptor> qualifiers = new HashSet<>();

    for(Annotation annotation : annotations) {
      if(annotation.annotationType().getAnnotation(Qualifier.class) != null) {
        qualifiers.add(new AnnotationDescriptor(annotation));
      }
    }

    return qualifiers.toArray(new AnnotationDescriptor[qualifiers.size()]);
  }

  private static boolean isOptional(Annotation[] annotations) {
    for(Annotation annotation : annotations) {
      if(annotation.annotationType().getSimpleName().equals("Nullable")) {
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

  private static final class HashSetBinding implements ResolvableBinding {
    private final AnnotationDescriptor[] qualifiers;
    private final Type elementType;
    private final boolean optional;

    private HashSetBinding(Type elementType, AnnotationDescriptor[] qualifiers, boolean optional) {
      this.qualifiers = qualifiers;
      this.elementType = elementType;
      this.optional = optional;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws BeanResolutionException {
      Set<Object> instances = instantiator.getInstances(elementType, (Object[])qualifiers);

      return instances.isEmpty() && optional ? null : instances;
    }

    @Override
    public boolean isProvider() {
      return false;
    }

    @Override
    public Type getType() {
      return Set.class;
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

  private static final class ArrayListBinding implements ResolvableBinding {
    private final Type elementType;
    private final AnnotationDescriptor[] qualifiers;
    private final boolean optional;

    private ArrayListBinding(Type elementType, AnnotationDescriptor[] qualifiers, boolean optional) {
      this.elementType = elementType;
      this.qualifiers = qualifiers;
      this.optional = optional;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws BeanResolutionException {
      Set<Object> instances = instantiator.getInstances(elementType, (Object[])qualifiers);

      return instances.isEmpty() && optional ? null : new ArrayList<>(instances);
    }

    @Override
    public boolean isProvider() {
      return false;
    }

    @Override
    public Type getType() {
      return ArrayList.class;
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

  private static final class ProviderBinding implements ResolvableBinding {
    private final ResolvableBinding binding;

    private ProviderBinding(ResolvableBinding binding) {
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
            throw new IllegalStateException("Exception while retrieving bean through provider", e);
          }
        }
      };
    }

    @Override
    public boolean isProvider() {
      return true;
    }

    @Override
    public Type getType() {
      return binding.getType();
    }

    @Override
    public Key getRequiredKey() {
      return binding.getRequiredKey();
    }

    @Override
    public boolean isParameter() {
      return false;
    }

    @Override
    public String toString() {
      return "ProviderBinding[binding=" + binding + "]";
    }
  }

  private static final class DirectBinding implements ResolvableBinding {
    private final Key key;
    private final boolean optional;
    private final boolean isParameter;

    private DirectBinding(Key requiredKey, boolean optional, boolean isParameter) {
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
    public Type getType() {
      return key.getType();
    }

    @Override
    public Key getRequiredKey() {
      return optional || isParameter ? null : key;
    }

    @Override
    public boolean isProvider() {
      return false;
    }

    @Override
    public boolean isParameter() {
      return isParameter;
    }

    @Override
    public String toString() {
      return "DirectBinding[cls=" + key.getType() + "; key=" + getRequiredKey() + "]";
    }
  }
}
