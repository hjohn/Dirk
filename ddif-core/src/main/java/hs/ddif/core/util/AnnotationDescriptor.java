package hs.ddif.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

/**
 * Class to describe an {@link Annotation} that can be more easily created than
 * an instance of {@link Annotation}.
 */
public class AnnotationDescriptor {
  private final String description;
  private final Class<? extends Annotation> annotationType;

  /**
   * Creates a new AnnotationDescriptor for the given annotation and values.  For example:<p>
   *
   *   AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a")))
   *
   * @param annotation an annotation, cannot be null
   * @param annotationValues zero or more values that are part of the annotation
   * @return an {@link AnnotationDescriptor}, never null
   */
  public static AnnotationDescriptor describe(Class<? extends Annotation> annotation, Value... annotationValues) {
    return new AnnotationDescriptor(annotation, mapToString(describeAsMap(annotation, annotationValues)));
  }

  /**
   * Convience method that creates an AnnotationDescriptor for the {@link Named} annotation
   * with the given name.
   *
   * @param name a name, cannot be null
   * @return an {@link AnnotationDescriptor}, never null
   */
  public static AnnotationDescriptor named(String name) {
    return AnnotationDescriptor.describe(Named.class, new Value("value", name));
  }

  AnnotationDescriptor(Class<? extends Annotation> annotationType, String description) {
    if(description == null) {
      throw new IllegalArgumentException("parameter 'description' cannot be null");
    }

    this.annotationType = annotationType;
    this.description = description;
  }

  /**
   * Constructs a new instance directly from an {@link Annotation}.
   *
   * @param annotation an {@link Annotation}, cannot be null
   */
  public AnnotationDescriptor(Annotation annotation) {
    this(annotation.annotationType(), mapToString(annotationToMap(annotation)));
  }

  /**
   * Returns the type of the {@link Annotation}.
   *
   * @return tyhe type of the {@link Annotation}, never null
   */
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

  static Map<String, Object> describeAsMap(Class<? extends Annotation> annotation, Value... annotationValues) {
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
  static String mapToString(Map<String, Object> map) {
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

  static Map<String, Object> annotationToMap(Annotation annotation) {
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
}
