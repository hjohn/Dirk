package hs.ddif.api.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.leangen.geantyref.AnnotationFormatException;
import jakarta.inject.Named;

public class AnnotationsTest {

  @Nested
  class When_of_IsCalled {
    @Test
    void shouldConstructAnnotation() throws NoSuchFieldException, SecurityException {
      assertThat(Annotations.of(Deepest.class))
        .isEqualTo(A.class.getDeclaredField("field1").getAnnotation(Deepest.class));
      assertThat(Annotations.of(Level.class, Map.of("value", 2)))
        .isEqualTo(Deeper.class.getDeclaredAnnotation(Level.class));
    }

    @Test
    void shouldRejectConstructingAnnotationsWithIncompatibleValues() {
      assertThatThrownBy(() -> Annotations.of(Level.class, Map.of("value", "bar")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Could not convert to annotation: interface hs.ddif.api.util.AnnotationsTest$Level {value=bar}")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(AnnotationFormatException.class)
        .hasMessage("Incompatible type(s) provided for value")
        .hasNoCause();
    }

    @Test
    void shouldRejectConstructingAnnotationsWithMissingRequiredValues() {
      assertThatThrownBy(() -> Annotations.of(Level.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Could not convert to annotation: interface hs.ddif.api.util.AnnotationsTest$Level {}")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(AnnotationFormatException.class)
        .hasMessage("Missing value(s) for value")
        .hasNoCause();
    }
  }

  @Nested
  class When_findAnnotations_IsCalled {

    @Test
    void shouldFindDeeplyNestedAnnotation() throws Exception {
      assertThat(Annotations.findAnnotations(A.class.getDeclaredField("field1"), Deepest.class))
        .containsExactlyInAnyOrder(Annotations.of(Deepest.class));
    }

    @Test
    void shouldFindSingleCopyOfExactDuplicateAnnotations() throws Exception {
      assertThat(Annotations.findAnnotations(A.class.getDeclaredField("field1"), Cold.class))
        .containsExactlyInAnyOrder(Annotations.of(Cold.class));
    }

    @Test
    void shouldFindMultipleAnnotationsOfSameTypeIfValuesDiffer() throws Exception {
      assertThat(Annotations.findAnnotations(A.class.getDeclaredField("field1"), Level.class))
        .containsExactlyInAnyOrder(
          Annotations.of(Level.class, Map.of("value", 1)),
          Annotations.of(Level.class, Map.of("value", 2)),
          Annotations.of(Level.class, Map.of("value", 3))
        );
    }
  }

  @Nested
  class When_findMetaAnnotatedAnnotations_IsCalled {

    @Test
    void shouldFindTwoAnnotationsMetaAnnotatedWithCold() throws Exception {
      assertThat(Annotations.findMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Annotations.of(Cold.class)))
        .containsExactlyInAnyOrder(Annotations.of(Deepest.class), Annotations.of(Wet.class));
    }

    @Test
    void shouldFindOnlyOneAnnotationAnnotatedWithLevel2() throws Exception {
      assertThat(Annotations.findMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Annotations.of(Level.class, Map.of("value", 2))))
        .containsExactlyInAnyOrder(Annotations.of(Deepest.class));
    }

    @Test
    void shouldNotFindItself() throws Exception {
      assertThat(Annotations.findMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Annotations.of(Deepest.class)))
        .isEmpty();
    }

    @Test
    void shouldFindItselfIfMetaAnnotatedWithItself() throws Exception {
      assertThat(Annotations.findMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Annotations.of(Hot.class)))
        .containsExactlyInAnyOrder(Annotations.of(Hot.class));
    }

    @Test
    void shouldNotFindAnnotationAnnotatedWithLevel2() throws Exception {
      assertThat(Annotations.findMetaAnnotatedAnnotations(A.class.getDeclaredField("field2"), Annotations.of(Level.class, Map.of("value", 2))))
        .isEmpty();
    }
  }

  @Nested
  class When_findAnnotations2AnnotatedWith_IsCalled {

    @Test
    void shouldFindAllAnnotationsDirectlyAnnotatedWithCold() throws Exception {
      assertThat(Annotations.findDirectlyMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Annotations.of(Cold.class)))
        .containsExactlyInAnyOrder(
          Annotations.of(Deeper.class),
          Annotations.of(Deep.class),
          Annotations.of(Wet.class)
        );

      assertThat(Annotations.findDirectlyMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Cold.class))
        .containsExactlyInAnyOrder(
          Annotations.of(Deeper.class),
          Annotations.of(Deep.class),
          Annotations.of(Wet.class)
        );
    }

    @Test
    void shouldFindOnlyOneAnnotationDirectlyAnnotatedWithLevel2() throws Exception {
      assertThat(Annotations.findDirectlyMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Annotations.of(Level.class, Map.of("value", 2))))
        .containsExactlyInAnyOrder(Annotations.of(Deeper.class));
    }

    @Test
    void shouldNotFindItself() throws Exception {
      assertThat(Annotations.findDirectlyMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Annotations.of(Deepest.class)))
        .isEmpty();

      assertThat(Annotations.findDirectlyMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Deepest.class))
        .isEmpty();
    }

    @Test
    void shouldFindItselfIfMetaAnnotatedWithItself() throws Exception {
      assertThat(Annotations.findDirectlyMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Annotations.of(Hot.class)))
        .containsExactlyInAnyOrder(Annotations.of(Hot.class));

      assertThat(Annotations.findDirectlyMetaAnnotatedAnnotations(A.class.getDeclaredField("field1"), Hot.class))
        .containsExactlyInAnyOrder(Annotations.of(Hot.class));
    }

    @Test
    void shouldNotFindAnnotationAnnotatedWithLevel2() throws Exception {
      assertThat(Annotations.findDirectlyMetaAnnotatedAnnotations(A.class.getDeclaredField("field2"), Annotations.of(Level.class, Map.of("value", 2))))
        .isEmpty();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({TYPE, METHOD, FIELD, PARAMETER})
  public @interface Level {
    int value();
  }

  @Wet
  @Retention(RetentionPolicy.RUNTIME)
  @Target({TYPE, METHOD, FIELD, PARAMETER})
  @interface Cold {
  }

  @Hot
  @Retention(RetentionPolicy.RUNTIME)
  @Target({TYPE, METHOD, FIELD, PARAMETER})
  @interface Hot {
  }

  @Cold
  @Retention(RetentionPolicy.RUNTIME)
  @Target({TYPE, METHOD, FIELD, PARAMETER})
  @interface Wet {
  }

  @Cold
  @Level(1)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({TYPE, METHOD, FIELD, PARAMETER})
  @interface Deep {
  }

  @Deep
  @Cold
  @Level(2)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({TYPE, METHOD, FIELD, PARAMETER})
  @interface Deeper {
  }

  @Deeper
  @Level(3)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({TYPE, METHOD, FIELD, PARAMETER})
  @interface Deepest {
  }

  private static class A {
    @Deepest @Wet @Hot public String field1;
    @Deep public String field2;
    @Named("me") public String named;
  }
}
