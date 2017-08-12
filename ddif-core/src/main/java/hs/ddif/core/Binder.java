package hs.ddif.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
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
        Nullable nullable = field.getAnnotation(Nullable.class);

        if(inject != null) {
          bindings.put(field, new Binding[] {createBinding(field.getGenericType(), nullable != null, extractQualifiers(field))});
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

  public static final Class<?> determineClassFromType(Type type) {
    if(type instanceof Class) {
      return (Class<?>)type;
    }
    else if(type instanceof ParameterizedType) {
      return (Class<?>)((ParameterizedType)type).getRawType();
    }
    else if(type instanceof TypeVariable) {
      return (Class<?>)((TypeVariable<?>)type).getBounds()[0];
    }

    throw new IllegalArgumentException("Unsupported type: " + type);
  }

  private static Binding createBinding(final Type type, final boolean optional, final AnnotationDescriptor... qualifiers) {
    final Class<?> cls = determineClassFromType(type);

    if(Set.class.isAssignableFrom(cls)) {
      final Type elementType = getGenericType(type);

      return new Binding() {
        @Override
        public Object getValue(Injector injector) {
          return injector.getInstances(elementType, (Object[])qualifiers);
        }

        @Override
        public boolean isOptional() {
          return false;
        }

        @Override
        public Key getRequiredKey() {
          return null;
        }
      };
    }
    else if(List.class.isAssignableFrom(cls)) {
      final Type elementType = getGenericType(type);

      return new Binding() {
        @Override
        public Object getValue(Injector injector) {
          return new ArrayList<>(injector.getInstances(elementType, (Object[])qualifiers));
        }

        @Override
        public boolean isOptional() {
          return false;
        }

        @Override
        public Key getRequiredKey() {
          return null;
        }
      };
    }
    else if(Provider.class.isAssignableFrom(cls)) {
      final Type genericType = getGenericType(type);
      final Binding binding = createBinding(genericType, false, qualifiers);

      return new Binding() {
        @Override
        public Object getValue(final Injector injector) {
          Object injectObject = new Provider<Object>() {
            @Override
            public Object get() {
              return binding.getValue(injector);
            }
          };

          return injectObject;
        }

        @Override
        public boolean isOptional() {
          return false;
        }

        @Override
        public Key getRequiredKey() {
          return binding.getRequiredKey();
        }
      };
    }
    else {
      final Type finalType = type instanceof Class && ((Class<?>)type).isPrimitive() ? WRAPPER_CLASS_BY_PRIMITIVE_CLASS.get(type) : type;
      final Key requiredKey = optional ? null : new Key(finalType, qualifiers);

      return new Binding() {
        @Override
        public Object getValue(Injector injector) {
          if(optional) {
            try {
              return injector.getInstance(finalType, (Object[])qualifiers);
            }
            catch(NoSuchBeanException e) {
              return null;
            }
          }
          else {
            return injector.getInstance(finalType, (Object[])qualifiers);
          }
        }

        @Override
        public Key getRequiredKey() {
          return requiredKey;
        }

        @Override
        public boolean isOptional() {
          return optional;
        }

        @Override
        public String toString() {
          return "DirectBinding[cls=" + cls + "; key=" + getRequiredKey() + "]";
        }
      };
    }
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
      if(Nullable.class.isInstance(annotation)) {
        return true;
      }
    }

    return false;
  }

  public static Type getGenericType(Type type) {
    if(type instanceof ParameterizedType) {
      ParameterizedType genericType = (ParameterizedType)type;
      return genericType.getActualTypeArguments()[0];
    }
    else if(type instanceof Class) {
      Class<?> cls = (Class<?>)type;
      return cls.getTypeParameters()[0];
    }

    throw new RuntimeException("Could not get generic type for: " + type);
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
}
