package hs.ddif.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
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
  private static final Key[] NO_REQUIRED_KEYS = new Key[0];

  public Map<AccessibleObject, Binding> resolve(Class<?> injectableClass) {
    Map<AccessibleObject, Binding> bindings = new HashMap<>();

    for(final Field field : injectableClass.getDeclaredFields()) {
      Inject inject = field.getAnnotation(Inject.class);
      Nullable nullable = field.getAnnotation(Nullable.class);

      if(inject != null) {
        bindings.put(field, createBinding(field.getGenericType(), nullable != null, extractQualifiers(field)));
      }
    }

    Constructor<?> emptyConstructor = null;
    Constructor<?>[] constructors = injectableClass.getConstructors();
    boolean foundInjectableConstructor = false;

    for(final Constructor<?> constructor : constructors) {
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

  private Binding createConstructorBinding(Constructor<?> constructor) {
    Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
    Type[] genericParameterTypes = constructor.getGenericParameterTypes();

    final List<Binding> constructorBindings = new ArrayList<>();
    final List<Key> requiredKeys = new ArrayList<>();

    for(int i = 0; i < genericParameterTypes.length; i++) {
      Type type = genericParameterTypes[i];

      AnnotationDescriptor[] qualifiers = extractQualifiers(parameterAnnotations[i]);

      boolean optional = isOptional(parameterAnnotations[i]);
      Binding binding = createBinding(type, optional, qualifiers);

      constructorBindings.add(binding);

      if(!optional) {
        requiredKeys.addAll(Arrays.asList(binding.getRequiredKeys()));
      }
    }

    return new Binding() {
      @Override
      public Object getValue(Injector injector) {
        Object[] values = new Object[constructorBindings.size()];

        for(int i = 0; i < constructorBindings.size(); i++) {
          Binding binding = constructorBindings.get(i);

          try {
            values[i] = binding.getValue(injector);
          }
          catch(NoSuchBeanException e) {
            if(!binding.isOptional()) {
              throw e;
            }

            values[i] = null;
          }
        }

        return values;
      }

      @Override
      public boolean isOptional() {
        return false;
      }

      @Override
      public Key[] getRequiredKeys() {
        return requiredKeys.toArray(new Key[requiredKeys.size()]);
      }
    };
  }

  public static final Class<?> determineClassFromType(Type type) {
    if(type instanceof Class) {
      return (Class<?>)type;
    }
    else if(type instanceof ParameterizedType) {
      return (Class<?>)((ParameterizedType)type).getRawType();
    }
    else if(type instanceof TypeVariable) {
      System.err.println(type);
      System.err.println(Arrays.toString(((TypeVariable<?>)type).getBounds()));
      return (Class<?>)((TypeVariable<?>)type).getBounds()[0];
    }

    throw new IllegalArgumentException("Unsupported type: " + type);
  }

  private Binding createBinding(final Type type, final boolean optional, final AnnotationDescriptor... qualifiers) {
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
        public Key[] getRequiredKeys() {
          return NO_REQUIRED_KEYS;
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
        public Key[] getRequiredKeys() {
          return NO_REQUIRED_KEYS;
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
        public Key[] getRequiredKeys() {
          return binding.getRequiredKeys();
        }
      };
    }
    else {
      final Type finalType = type instanceof Class && ((Class<?>)type).isPrimitive() ? WRAPPER_CLASS_BY_PRIMITIVE_CLASS.get(type) : type;
      final Key[] requiredKeys = optional ? NO_REQUIRED_KEYS : new Key[] {new Key(finalType, qualifiers)};

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
        public Key[] getRequiredKeys() {
          return requiredKeys;
        }

        @Override
        public boolean isOptional() {
          return optional;
        }

        @Override
        public String toString() {
          return "DirectBinding[cls=" + cls + "; requiredKeys=" + Arrays.toString(getRequiredKeys()) + "]";
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
