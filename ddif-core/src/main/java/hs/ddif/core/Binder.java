package hs.ddif.core;

import com.googlecode.gentyref.GenericTypeReflector;

import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

public class Binder {

  public static Map<AccessibleObject, Binding[]> resolve(Class<?> injectableClass) {
    Map<AccessibleObject, Binding[]> bindings = new HashMap<>();

    Class<?> currentInjectableClass = injectableClass;

    while(currentInjectableClass != null) {
      for(final Field field : currentInjectableClass.getDeclaredFields()) {
        Inject inject = field.getAnnotation(Inject.class);

        if(inject != null) {
          Type type = GenericTypeReflector.getExactFieldType(field, injectableClass);

          bindings.put(field, new Binding[] {createBinding(type, isOptional(field.getAnnotations()), extractQualifiers(field))});
        }
      }

      currentInjectableClass = currentInjectableClass.getSuperclass();
    }

    Constructor<?> emptyConstructor = null;
    Constructor<?>[] constructors = injectableClass.getConstructors();
    boolean foundInjectableConstructor = false;

    for(Constructor<?> constructor : constructors) {
      Inject inject = constructor.getAnnotation(Inject.class);

      if(constructor.getParameterTypes().length == 0) {
        emptyConstructor = constructor;
      }

      if(inject != null) {
        foundInjectableConstructor = true;
        bindings.put(constructor, createConstructorBinding(constructor));
      }
    }

    if(!foundInjectableConstructor && emptyConstructor != null) {
      bindings.put(emptyConstructor, createConstructorBinding(emptyConstructor));
    }

    return bindings;
  }

  private static Binding[] createConstructorBinding(Constructor<?> constructor) {
    Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
    Type[] genericParameterTypes = constructor.getGenericParameterTypes();
    List<Binding> constructorBindings = new ArrayList<>();

    for(int i = 0; i < genericParameterTypes.length; i++) {
      Type type = genericParameterTypes[i];
      AnnotationDescriptor[] qualifiers = extractQualifiers(parameterAnnotations[i]);
      Binding binding = createBinding(type, isOptional(parameterAnnotations[i]), qualifiers);

      constructorBindings.add(binding);
    }

    return constructorBindings.toArray(new Binding[constructorBindings.size()]);
  }

  private static Binding createBinding(final Type type, final boolean optional, final AnnotationDescriptor... qualifiers) {
    return createBinding(false, type, optional, qualifiers);
  }

  private static Binding createBinding(boolean isProviderAlready, final Type type, final boolean optional, final AnnotationDescriptor... qualifiers) {
    final Class<?> cls = TypeUtils.determineClassFromType(type);

    if(Set.class.isAssignableFrom(cls)) {
      return new HashSetBinding(TypeUtils.getGenericType(type), qualifiers, optional);
    }
    if(List.class.isAssignableFrom(cls)) {
      return new ArrayListBinding(TypeUtils.getGenericType(type), qualifiers, optional);
    }
    if(Provider.class.isAssignableFrom(cls) && !isProviderAlready) {
      return new ProviderBinding(createBinding(true, TypeUtils.getGenericType(type), false, qualifiers));
    }

    Type finalType = type instanceof Class && ((Class<?>)type).isPrimitive() ? WRAPPER_CLASS_BY_PRIMITIVE_CLASS.get(type) : type;

    return new DirectBinding(new Key(finalType, qualifiers), optional);
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

  private static final class HashSetBinding implements Binding {
    private final AnnotationDescriptor[] qualifiers;
    private final Type elementType;
    private final boolean optional;

    private HashSetBinding(Type elementType, AnnotationDescriptor[] qualifiers, boolean optional) {
      this.qualifiers = qualifiers;
      this.elementType = elementType;
      this.optional = optional;
    }

    @Override
    public Object getValue(Injector injector) {
      Set<Object> instances = injector.getInstances(elementType, (Object[])qualifiers);

      return instances.isEmpty() && optional ? null : instances;
    }

    @Override
    public boolean isProvider() {
      return false;
    }

    @Override
    public Key getRequiredKey() {
      return null;
    }
  }

  private static final class ArrayListBinding implements Binding {
    private final Type elementType;
    private final AnnotationDescriptor[] qualifiers;
    private final boolean optional;

    private ArrayListBinding(Type elementType, AnnotationDescriptor[] qualifiers, boolean optional) {
      this.elementType = elementType;
      this.qualifiers = qualifiers;
      this.optional = optional;
    }

    @Override
    public Object getValue(Injector injector) {
      Set<Object> instances = injector.getInstances(elementType, (Object[])qualifiers);

      return instances.isEmpty() && optional ? null : new ArrayList<>(instances);
    }

    @Override
    public boolean isProvider() {
      return false;
    }

    @Override
    public Key getRequiredKey() {
      return null;
    }
  }

  private static final class ProviderBinding implements Binding {
    private final Binding binding;

    private ProviderBinding(Binding binding) {
      this.binding = binding;
    }

    @Override
    public Object getValue(final Injector injector) {

      /*
       * When supplying a Provider<X>, check if such a provider is implemented by a concrete class first, otherwise
       * create one.
       */

      try {
        if(binding.getRequiredKey() != null) {
          Type searchType = org.apache.commons.lang3.reflect.TypeUtils.parameterize(Provider.class, binding.getRequiredKey().getType());

          return injector.getInstance(searchType, (Object[])binding.getRequiredKey().getQualifiersAsArray());
        }
      }
      catch(NoSuchBeanException e) {
        // Ignore
      }

      return new Provider<Object>() {
        @Override
        public Object get() {
          return binding.getValue(injector);
        }
      };
    }

    @Override
    public boolean isProvider() {
      return true;
    }

    @Override
    public Key getRequiredKey() {
      return binding.getRequiredKey();
    }

    @Override
    public String toString() {
      return "ProviderBinding[binding=" + binding + "]";
    }
  }

  private static final class DirectBinding implements Binding {
    private final Key key;
    private final boolean optional;

    private DirectBinding(Key requiredKey, boolean optional) {
      this.key = requiredKey;
      this.optional = optional;
    }

    @Override
    public Object getValue(Injector injector) {
      if(optional) {
        try {
          return injector.getInstance(key.getType(), (Object[])key.getQualifiersAsArray());
        }
        catch(NoSuchBeanException e) {
          return null;
        }
      }
      else {
        return injector.getInstance(key.getType(), (Object[])key.getQualifiersAsArray());
      }
    }

    @Override
    public Key getRequiredKey() {
      return optional ? null : key;
    }

    @Override
    public boolean isProvider() {
      return false;
    }

    @Override
    public String toString() {
      return "DirectBinding[cls=" + key.getType() + "; key=" + getRequiredKey() + "]";
    }
  }
}
