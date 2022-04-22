package hs.ddif.jsr330;

import hs.ddif.annotations.Opt;
import hs.ddif.api.instantiation.InstanceCreationException;
import hs.ddif.api.instantiation.Key;
import hs.ddif.api.instantiation.MultipleInstancesException;
import hs.ddif.api.instantiation.NoSuchInstanceException;
import hs.ddif.api.util.Annotations;
import hs.ddif.core.InstantiatorFactories;
import hs.ddif.core.config.ConfigurableAnnotationStrategy;
import hs.ddif.org.apache.commons.lang3.reflect.TypeUtils;
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.Instantiator;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.test.util.ReplaceCamelCaseDisplayNameGenerator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Qualifier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(ReplaceCamelCaseDisplayNameGenerator.class)
public class InstanceTypeExtensionTest {
  private final AnnotationStrategy annotationStrategy = new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, null);
  private final InstantiationContext context = mock(InstantiationContext.class);

  @BeforeEach
  void beforeEach() throws InstanceCreationException, MultipleInstancesException {
    when(context.create(new Key(Number.class))).thenReturn(42);
    when(context.create(new Key(Number.class, Set.of(Annotations.of(Red.class))))).thenReturn(746);
    when(context.create(new Key(Double.class))).thenReturn(42.42);
    when(context.createAll(new Key(Number.class))).thenReturn(List.of(43, 42, 41));
  }

  @Nested
  class GivenAn_Instance_Of_Number_ {
    InstantiatorFactory factory = InstantiatorFactories.create(annotationStrategy, Map.of(Instance.class, new InstanceTypeExtension<>(annotationStrategy)));
    Instantiator<Instance<Number>> instantiator = factory.getInstantiator(new Key(TypeUtils.parameterize(Instance.class, Number.class)), null);
    Instance<Number> instance = assertDoesNotThrow(() -> instantiator.getInstance(context));

    @Test
    void _get_ShouldProvide_Number_WhenCalled() {
      assertEquals(42, instance.get());
    }

    @Test
    void _get_ShouldThrowExceptionWhenNoInstanceAvailable() throws InstanceCreationException, MultipleInstancesException {
      when(context.create(new Key(Number.class))).thenReturn(null);

      assertThatThrownBy(() -> instance.get())
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasMessage("No such instance: [java.lang.Number]")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasMessage("No such instance: [java.lang.Number]")
        .hasNoCause();
    }

//    @Test
//    void _get_ShouldThrowExceptionWhenMultipleInstancesAvailable() throws InstanceCreationException, MultipleInstancesException {
//      when(context.create(new Key(Number.class))).thenThrow(new MultipleInstancesException("boo", null));
//
//      assertThatThrownBy(() -> instance.get())
//        .isExactlyInstanceOf(MultipleInstancesException.class)
//        .hasMessage("boo")
//        .hasNoCause();
//    }

    @Test
    void iterate_ShouldReturnAllOptions() {
      assertThat(instance.iterator()).toIterable().containsExactlyInAnyOrder(41, 42, 43);
    }

//    @Test
//    void _iterate_ShouldThrowExceptionWhenUnableToCreateInstances() throws InstanceCreationException {
//      when(context.createAll(new Key(Number.class))).thenThrow(new InstanceCreationException("boo", null));
//
//      assertThatThrownBy(() -> instance.iterator())
//        .isExactlyInstanceOf(InstanceCreationException.class)
//        .hasMessage("boo")
//        .hasNoCause();
//    }

    @Test
    void _select_ShouldAllowSelectingSubtype() {
      Instance<Number> sub = instance.select(Double.class);

      sub.get();

      assertEquals(42.42, sub.get());
    }

    @Test
    void _select_ShouldAllowSelectingSameType() {
      Instance<Number> sub = instance.select(Number.class);

      sub.get();

      assertEquals(42, sub.get());
    }

    @Test
    void _select_ShouldNotAllowSelectingNonSubtype() {
      assertThatThrownBy(() -> instance.select(Boolean.class))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasNoCause();

      assertThatThrownBy(() -> instance.select(Object.class))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasNoCause();
    }

    @Test
    void _select_ShouldAllowSelectingQualifiedType() {
      Instance<Number> red = instance.select(Annotations.of(Red.class));

      assertEquals(746, red.get());
    }
  }

  @Nested
  class GivenAnOptional_Instance_Of_Number_ {
    @Opt String annotatedField;  // used to create AnnotatedElement from

    InstantiatorFactory factory = InstantiatorFactories.create(annotationStrategy, Map.of(Instance.class, new InstanceTypeExtension<>(annotationStrategy)));
    Instantiator<Instance<Number>> instantiator = factory.getInstantiator(new Key(TypeUtils.parameterize(Instance.class, Number.class)), assertDoesNotThrow(() -> getClass().getDeclaredField("annotatedField")));
    Instance<Number> instance = assertDoesNotThrow(() -> instantiator.getInstance(context));

    @Test
    void _get_ShouldProvide_Number_WhenCalled() {
      assertEquals(42, instance.get());
    }

    @Test
    @Disabled  // FIXME
    void _get_ShouldReturn_null_WhenNoInstanceAvailable() throws InstanceCreationException, MultipleInstancesException {
      when(context.create(new Key(Number.class))).thenReturn(null);

      assertNull(instance.get());
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface Red {
  }
}
