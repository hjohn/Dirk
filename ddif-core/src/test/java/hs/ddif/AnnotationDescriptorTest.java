package hs.ddif;

import static hs.ddif.AnnotationDescriptor.describeAsMap;
import static org.junit.Assert.assertEquals;
import hs.ddif.test.qualifiers.Big;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class AnnotationDescriptorTest {
  private final Class<? extends Annotation> annotationClass;
  private final Map<String, Object> expectedMap;
  private final Map<String, Object> describedMap;
  private final String expectedString;

  public AnnotationDescriptorTest(Class<? extends Annotation> annotationClass, Map<String, Object> expectedMap, Map<String, Object> describedMap, String expectedString) {
    this.annotationClass = annotationClass;
    this.expectedMap = expectedMap;
    this.describedMap = describedMap;
    this.expectedString = expectedString;
  }

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    Object[][] data = new Object[][] {
      {
        Wierd.class,
        new HashMap<String, Object>() {{
          put("", "hs.ddif.AnnotationDescriptorTest$Wierd");
          put("power", 15L);
          put("type", String.class);
        }},
        describeAsMap(Wierd.class, new Value("power", 15L), new Value("type", String.class)),
        "[=hs.ddif.AnnotationDescriptorTest$Wierd, power=15, type=class java.lang.String]"
      },

      {
        Wierd.class,
        new HashMap<String, Object>() {{
          put("", "hs.ddif.AnnotationDescriptorTest$Wierd");
          put("power", 15L);
          put("type", String.class);
        }},
        describeAsMap(Wierd.class, new Value("level", 5), new Value("power", 15L), new Value("type", String.class)),  // level=5 is default, gets automatically removed
        "[=hs.ddif.AnnotationDescriptorTest$Wierd, power=15, type=class java.lang.String]"
      },

      {
        Big.class,
        new HashMap<String, Object>() {{
          put("", "hs.ddif.test.qualifiers.Big");
        }},
        describeAsMap(Big.class),
        "[=hs.ddif.test.qualifiers.Big]"
      },

      {
        Named.class,
        new HashMap<String, Object>() {{
          put("", "javax.inject.Named");
          put("value", "someName");
        }},
        describeAsMap(Named.class, new Value("value", "someName")),
        "[=javax.inject.Named, value=someName]"
      },

      {
        Key.class,
        new HashMap<String, Object>() {{
          put("", "hs.ddif.AnnotationDescriptorTest$Key");
          put("pairs", new ArrayList<Map<String, Object>>() {{
            add(new HashMap<String, Object>() {{
              put("", "hs.ddif.AnnotationDescriptorTest$KeyValue");
              put("key", "a");
              put("value", "1");
            }});
            add(new HashMap<String, Object>() {{
              put("", "hs.ddif.AnnotationDescriptorTest$KeyValue");
              put("key", "b");
              put("value", "2");
            }});
          }});
        }},
        describeAsMap(Key.class, new Value("pairs", new Map[] {
          describeAsMap(KeyValue.class, new Value("key", "a"), new Value("value", "1")),
          describeAsMap(KeyValue.class, new Value("key", "b"), new Value("value", "2"))
        })),
        "[=hs.ddif.AnnotationDescriptorTest$Key, pairs={[=hs.ddif.AnnotationDescriptorTest$KeyValue, key=a, value=1], [=hs.ddif.AnnotationDescriptorTest$KeyValue, key=b, value=2]}]"
      }
    };

    return Arrays.asList(data);
  }

  @Test
  public void describedMapShouldMatchExpectedMap() {
    assertEquals(annotationClass.toString(), expectedMap, describedMap);
  }

  @Test
  public void annotationToMapShouldMatchExpectedMap() {
    Annotation annotation = ClassWithTestAnnotations.class.getAnnotation(annotationClass);

    assertEquals(annotationClass.toString(), expectedMap, AnnotationDescriptor.annotationToMap(annotation));
  }

  @Test
  public void annotationToMapShouldMatchDescribedMap() {
    Annotation annotation = ClassWithTestAnnotations.class.getAnnotation(annotationClass);

    assertEquals(annotationClass.toString(), describedMap, AnnotationDescriptor.annotationToMap(annotation));
  }

  @Test
  public void annotationToMapToStringShouldMatchExpectedString() {
    Annotation annotation = ClassWithTestAnnotations.class.getAnnotation(annotationClass);

    assertEquals(annotationClass.toString(), expectedString, AnnotationDescriptor.mapToString(AnnotationDescriptor.annotationToMap(annotation)));
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface KeyValue {
    String key();
    String value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface Key {
    KeyValue[] pairs();
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface Wierd {
    int level() default 5;
    long power();
    Class<?> type();
  }

  @Big @Named("someName") @Key(pairs = {@KeyValue(key = "a", value = "1"), @KeyValue(key = "b", value = "2")}) @Wierd(type = String.class, power = 15)
  public static class ClassWithTestAnnotations {
  }
}
