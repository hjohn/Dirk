package hs.ddif.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Qualifier;

public class AnnotationDescriptor {
  private final String description;
  private final Class<? extends Annotation> annotationType;

  public static AnnotationDescriptor describe(Class<? extends Annotation> annotation, Value... annotationValues) {
    return new AnnotationDescriptor(annotation, mapToString(describeAsMap(annotation, annotationValues)));
  }

  AnnotationDescriptor(Class<? extends Annotation> annotationType, String description) {
    if(description == null) {
      throw new IllegalArgumentException("parameter 'description' cannot be null");
    }

    this.annotationType = annotationType;
    this.description = description;
  }

  public AnnotationDescriptor(Annotation annotation) {
    this(annotation.annotationType(), mapToString(annotationToMap(annotation)));
  }

  public Class<? extends Annotation> annotationType() {
    return annotationType;
  }

  @Override
  public int hashCode() {
    return description.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    return description.equals(((AnnotationDescriptor)obj).description);
  }

  @Override
  public String toString() {
    return description;
  }

  public static Map<String, Object> describeAsMap(Class<? extends Annotation> annotation, Value... annotationValues) {
    Map<String, Object> map = new HashMap<>();

    map.put("", annotation.getName());

    Method[] methods = annotation.getDeclaredMethods();

    for(Value annotationValue : annotationValues) {
      Method matchingMethod = null;

      for(Method method : methods) {
        if(method.getName().equals(annotationValue.getKey())) {
          matchingMethod = method;
        }
      }

      if(matchingMethod == null) {
        throw new IllegalArgumentException("No such field \"" + annotationValue.getKey() + "\" in: " + annotation);
      }

      Object value = annotationValue.getValue();

      if(value == null) {
        throw new IllegalArgumentException("Annotations cannot contain null values: field \"" + annotationValue.getKey() + "\" in: " + annotation);
      }

      if(value.getClass().isArray()) {
        List<Object> list = new ArrayList<>();

        for(Object element : (Object[])value) {
          list.add(element);
        }

        map.put(annotationValue.getKey(), list);
      }
      else if(!value.equals(matchingMethod.getDefaultValue())) {
        map.put(annotationValue.getKey(), value);
      }
    }

    return map;
  }

  @SuppressWarnings("unchecked")
  public static String mapToString(Map<String, Object> map) {
    StringBuilder builder = new StringBuilder();
    List<String> keys = new ArrayList<>(map.keySet());

    Collections.sort(keys);

    for(String key : keys) {
      if(!key.isEmpty()) {
        Object value = map.get(key);

        if(builder.length() == 0) {
          builder.append("[");
        }
        else {
          builder.append(", ");
        }

        if(value instanceof List) {
          boolean firstItem = true;

          builder.append(key).append("=").append("{");

          for(Object obj : (List<?>)value) {
            if(!firstItem) {
              builder.append(", ");
            }

            if(obj instanceof Map) {
              builder.append(mapToString((Map<String, Object>)obj));
            }
            else {
              builder.append(obj);
            }
            firstItem = false;
          }

          builder.append("}");
        }
        else {
          builder.append(key).append("=").append(value);
        }
      }
    }

    if(builder.length() > 0) {
      builder.append("]");
    }

    return "@" + map.get("") + builder.toString();
  }

  public static Map<String, Object> annotationToMap(Annotation annotation) {
    Map<String, Object> map = new HashMap<>();

    map.put("", annotation.annotationType().getName());

    for(Method method : annotation.annotationType().getDeclaredMethods()) {
      Object value = callMethod(annotation, method);

      if(!value.equals(method.getDefaultValue())) {
        Class<?> valueClass = value.getClass();

        if(valueClass.isArray()) {
          List<Object> list = new ArrayList<>();

          for(Object element : (Object[])value) {
            if(element != null && valueClass.getComponentType().isAnnotation()) {
              element = annotationToMap((Annotation)element);
            }

            list.add(element);
          }

          value = list;
        }
        else if(valueClass.equals(Annotation.class)) {
          value = annotationToMap((Annotation)value);
        }

        map.put(method.getName(), value);
      }
    }

    return map;
  }

  private static Object callMethod(Object instance, Method method) {
    try {
      method.setAccessible(true);

      return method.invoke(instance);
    }
    catch(IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(method.toString(), e);
    }
  }

  public static Set<AnnotationDescriptor> extractQualifiers(Class<?> cls) {
    return extractQualifiers(cls.getAnnotations());
  }

  private static Set<AnnotationDescriptor> extractQualifiers(Annotation[] annotations) {
    Set<AnnotationDescriptor> qualifiers = new HashSet<>();

    for(Annotation annotation : annotations) {
      if(annotation.annotationType().getAnnotation(Qualifier.class) != null) {
        qualifiers.add(new AnnotationDescriptor(annotation));
      }
    }

    return qualifiers;
  }
}
