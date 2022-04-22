package hs.ddif.cdi;

import hs.ddif.api.Injector;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

public class InjectorsTest {

  public static class AnnotationsSupport {
    private final Injector injector = Injectors.manual();

    @Test
    void shouldAutomaticallyAddDefaultAndAnyAnnotations() throws Exception {
      injector.register(B.class);
      injector.register(C.class);
      injector.register(A.class);

      A a = injector.getInstance(A.class);

      assertThat(a.defaultImplementation).isInstanceOf(B.class);
      assertThat(a.redImplementation).isInstanceOf(C.class);
      assertThat(a.allImplementations).extracting("class").containsExactlyInAnyOrder(B.class, C.class);

      injector.remove(A.class);
      injector.remove(B.class);
      injector.remove(C.class);
    }

    public static class A {
      @Inject I defaultImplementation;
      @Inject @Red I redImplementation;
      @Inject @Any List<I> allImplementations;
    }

    interface I {
    }

    public static class B implements I {
    }

    @Red
    public static class C implements I {
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Red {
    }
  }

  public static class ProducesAnnotationSupport {
    private final Injector injector = Injectors.manual();

    @Test
    void shouldSupportProducesAnnotation() throws Exception {
      injector.register(A.class);

      assertNotNull(injector.getInstance(B.class));
    }

    public static class A {
      @Produces B b = new B();
    }

    public static class B {
    }
  }
}
