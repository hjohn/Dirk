package hs.ddif;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
        Set<Annotation> qualifiers = extractQualifiers(field);

        if(Modifier.isFinal(field.getModifiers())) {
          throw new DependencyException("Cannot inject final fields: " + field + " in: " + injectableClass);
        }

        bindings.put(field, createBinding(qualifiers, field.getGenericType()));
      }
    }

    boolean foundInjectableConstructor = false;

    for(final Constructor<?> constructor : injectableClass.getConstructors()) {
      Inject inject = constructor.getAnnotation(Inject.class);

      if(inject != null) {
        if(foundInjectableConstructor) {
          throw new DependencyException("Only one constructor is allowed to be annoted with @Inject: " + injectableClass);
        }

        foundInjectableConstructor = true;

        Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
        Class<?>[] parameterTypes = constructor.getParameterTypes();

        final List<Binding> constructorBindings = new ArrayList<>();
        final List<Key> requiredKeys = new ArrayList<>();

        for(int i = 0; i < parameterTypes.length; i++) {
          Class<?> cls = parameterTypes[i];

          Set<Annotation> qualifiers = extractQualifiers(parameterAnnotations[i]);

          Binding binding = createBinding(qualifiers, cls);

          constructorBindings.add(binding);
          requiredKeys.addAll(Arrays.asList(binding.getRequiredKeys()));
        }

        bindings.put(constructor, new Binding() {
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
    }


    return bindings;
  }

  private Binding createBinding(Set<Annotation> qualifiers, Type type) {
    final Class<?> cls = type instanceof Class ? (Class<?>)type : (Class<?>)((ParameterizedType)type).getRawType();

    if(Set.class.isAssignableFrom(cls)) {
      final Class<?> genericType = (Class<?>)getGenericType(type);
      final Key key = new Key(qualifiers, genericType);

      return new Binding() {
        @Override
        public Object getValue(Injector injector) {
          Set<Object> injectObject = new HashSet<>();

          for(Class<?> concreteClass : injector.getConcreteClasses(key)) {
            injectObject.add(injector.getInstance(concreteClass));
          }

          return injectObject;
        }

        @Override
        public Key[] getRequiredKeys() {
          return NO_REQUIRED_KEYS;
        }
      };
    }
    else if(Provider.class.isAssignableFrom(cls)) {
      final Type genericType = getGenericType(type);
      final Binding binding = createBinding(qualifiers, genericType);

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
      final Key key = new Key(qualifiers, cls);

      return new Binding() {
        @Override
        public Object getValue(Injector injector) {
          return injector.getInstance(key);
        }

        @Override
        public Key[] getRequiredKeys() {
          return new Key[] {key};
        }
      };
    }
  }

  private static Set<Annotation> extractQualifiers(Field field) {
    return extractQualifiers(field.getAnnotations());
  }

  private static Set<Annotation> extractQualifiers(Annotation[] annotations) {
    Set<Annotation> qualifiers = new HashSet<>();

    for(Annotation annotation : annotations) {
      if(annotation.annotationType().getAnnotation(Qualifier.class) != null) {
        qualifiers.add(annotation);
      }
    }

    return qualifiers;
  }

  private static Type getGenericType(Type type) {
    if(type instanceof ParameterizedType) {
      ParameterizedType genericType = (ParameterizedType)type;
      return genericType.getActualTypeArguments()[0];
    }

    throw new RuntimeException("Could not get generic type for: " + type);
  }
}
