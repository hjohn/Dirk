package org.int4.dirk.library;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.spi.config.LifeCycleCallbacks;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class AnnotationBasedLifeCycleCallbacksFactoryTest {
  private AnnotationBasedLifeCycleCallbacksFactory factory = new AnnotationBasedLifeCycleCallbacksFactory(PostConstruct.class, PreDestroy.class);

  @Test
  void shouldCallLifecycleMethods() throws InvocationTargetException {
    LifeCycleCallbacks callbacks = factory.create(A.class);

    A a = new A();

    callbacks.postConstruct(a);

    assertThat(a.postConstructsA).isEqualTo(1);
    assertThat(a.postConstructsB).isEqualTo(1);
    assertThat(a.postConstructsC).isEqualTo(1);
    assertThat(a.preDestroysA).isEqualTo(0);
    assertThat(a.preDestroysB).isEqualTo(0);
    assertThat(a.preDestroysC).isEqualTo(0);

    callbacks.preDestroy(a);

    assertThat(a.postConstructsA).isEqualTo(1);
    assertThat(a.postConstructsB).isEqualTo(1);
    assertThat(a.postConstructsC).isEqualTo(1);
    assertThat(a.preDestroysA).isEqualTo(1);
    assertThat(a.preDestroysB).isEqualTo(1);
    assertThat(a.preDestroysC).isEqualTo(1);
  }

  static class A extends B {
    int postConstructsA = 0;
    int preDestroysA = 0;

    @PostConstruct
    void postConstructA() {
      postConstructsA++;
    }

    @PreDestroy
    void preDestroyA() {
      preDestroysA++;
    }
  }

  static class B extends C {
    int postConstructsB = 0;
    int preDestroysB = 0;

    @PostConstruct
    void postConstructB() {
      postConstructsB++;
    }

    @PreDestroy
    void preDestroyB() {
      preDestroysB++;
    }
  }

  static class C {
    int postConstructsC = 0;
    int preDestroysC = 0;

    @PostConstruct
    void postConstructC() {
      postConstructsC++;
    }

    @PreDestroy
    void preDestroyC() {
      preDestroysC++;
    }
  }

  @Test
  void shouldRejectLifecycleMethodsWithParameters() {
    assertThatThrownBy(() -> factory.create(BadA.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [void org.int4.dirk.library.AnnotationBasedLifeCycleCallbacksFactoryTest$BadA.bad(java.lang.String)] cannot have parameters when annotated as a lifecycle method (post construct or pre destroy)")
      .hasNoSuppressedExceptions()
      .hasNoCause();
  }

  static class BadA {
    @PostConstruct
    void bad(String a) {
      System.out.println(a);
    }
  }

  @Test
  void shouldForwardExceptionsDuringPostConstruct() {
    LifeCycleCallbacks callbacks = factory.create(BadB.class);

    assertThatThrownBy(() -> callbacks.postConstruct(new BadB()))
      .isExactlyInstanceOf(InvocationTargetException.class)
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NoSuchElementException.class)
      .hasNoCause();
  }

  @Test
  void shouldSkipExceptionsDuringPreDestroy() {
    LifeCycleCallbacks callbacks = factory.create(BadB.class);

    assertThatCode(() -> callbacks.preDestroy(new BadB())).doesNotThrowAnyException();
  }

  static class BadB {
    @PostConstruct
    void postConstruct() {
      throw new NoSuchElementException("oops");
    }

    @PreDestroy
    void preDestroy() {
      throw new NoSuchElementException("oops");
    }
  }
}
