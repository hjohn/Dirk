package hs.ddif;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;

public class Binder {
  private static final Key[] NO_REQUIRED_KEYS = new Key[0];

  public Map<AccessibleObject, Binding> resolve(Class<?> injectableClass) {
    Map<AccessibleObject, Binding> bindings = new HashMap<>();

    for(final Field field : injectableClass.getDeclaredFields()) {
      Inject inject = field.getAnnotation(Inject.class);

      if(inject != null) {
        if(Modifier.isFinal(field.getModifiers())) {
          throw new DependencyException("Cannot inject final fields: " + field + " in: " + injectableClass);
        }

        bindings.put(field, createBinding(field.getGenericType(), extractQualifiers(field)));
      }
    }

    Constructor<?> suitableConstructor = null;
    boolean foundInjectableConstructor = false;

    for(final Constructor<?> constructor : injectableClass.getConstructors()) {
      Inject inject = constructor.getAnnotation(Inject.class);

      if(constructor.getParameterTypes().length == 0) {
        suitableConstructor = constructor;
      }
      if(inject != null) {
        if(foundInjectableConstructor) {
          throw new DependencyException("Only one constructor is allowed to be annoted with @Inject: " + injectableClass);
        }

        foundInjectableConstructor = true;
        suitableConstructor = constructor;
      }
    }

    if(suitableConstructor != null) {
      Annotation[][] parameterAnnotations = suitableConstructor.getParameterAnnotations();
      Type[] genericParameterTypes = suitableConstructor.getGenericParameterTypes();

      final List<Binding> constructorBindings = new ArrayList<>();
      final List<Key> requiredKeys = new ArrayList<>();

      for(int i = 0; i < genericParameterTypes.length; i++) {
        Type type = genericParameterTypes[i];

        AnnotationDescriptor[] qualifiers = extractQualifiers(parameterAnnotations[i]);

        Binding binding = createBinding(type, qualifiers);

        constructorBindings.add(binding);
        requiredKeys.addAll(Arrays.asList(binding.getRequiredKeys()));
      }

      bindings.put(suitableConstructor, new Binding() {
        @Override
        public Object getValue(Injector injector) {
          Object[] values = new Object[constructorBindings.size()];

          for(int i = 0; i < constructorBindings.size(); i++) {
            Binding binding = constructorBindings.get(i);

            values[i] = binding.getValue(injector);
          }

          return values;
        }

        @Override
        public Key[] getRequiredKeys() {
          return requiredKeys.toArray(new Key[requiredKeys.size()]);
        }
      });
    }

    return bindings;
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

  private Binding createBinding(final Type type, final AnnotationDescriptor... qualifiers) {
    final Class<?> cls = determineClassFromType(type);

    if(Set.class.isAssignableFrom(cls)) {
      final Type elementType = getGenericType(type);

      return new Binding() {
        @Override
        public Object getValue(Injector injector) {
          return injector.getInstances(elementType, (Object[])qualifiers);
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
        public Key[] getRequiredKeys() {
          return NO_REQUIRED_KEYS;
        }
      };
    }
    else if(Provider.class.isAssignableFrom(cls)) {
      final Type genericType = getGenericType(type);
      final Binding binding = createBinding(genericType, qualifiers);

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
        public Key[] getRequiredKeys() {
          return binding.getRequiredKeys();
        }
      };
    }
    else {
      final Key key = new Key(type, qualifiers);

      return new Binding() {
        @Override
        public Object getValue(Injector injector) {
          return injector.getInstance(type, (Object[])qualifiers);
        }

        @Override
        public Key[] getRequiredKeys() {
          return new Key[] {key};
        }

        @Override
        public String toString() {
          return "DirectBinding[cls=" + cls + "; requiredKeys=" + key + "]";
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
}
