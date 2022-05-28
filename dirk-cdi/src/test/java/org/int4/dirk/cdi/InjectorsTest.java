package org.int4.dirk.cdi;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import org.int4.dirk.api.Injector;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.spi.scope.AbstractScopeResolver;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

public class InjectorsTest {

  public static class AnnotationsSupport {
    private final Injector injector = Injectors.manual();

    @Test
    void shouldAddStandardAnnotationsToUnannotatedSource() throws Exception {
      injector.registerInstance("A");

      assertThat(injector.getInstances(Object.class, Default.class)).hasSize(1);
      assertThat(injector.getInstances(Object.class, Any.class)).hasSize(1);

      injector.removeInstance("A");
    }

    @Test
    void shouldAddStandardAnnotationToSourceAnnotatedOnlyWithDefault() throws Exception {
      injector.registerInstance("A", Annotations.of(Default.class));

      assertThat(injector.getInstances(Object.class, Default.class)).hasSize(1);
      assertThat(injector.getInstances(Object.class, Any.class)).hasSize(1);

      injector.removeInstance("A", Annotations.of(Default.class));
    }

    @Test
    void shouldAddStandardAnnotationToSourceAnnotatedOnlyWithAny() throws Exception {
      injector.registerInstance("A", Annotations.of(Any.class));

      assertThat(injector.getInstances(Object.class, Default.class)).hasSize(1);
      assertThat(injector.getInstances(Object.class, Any.class)).hasSize(1);

      injector.removeInstance("A", Annotations.of(Any.class));
    }

    @Test
    void shouldAddStandardAnnotationToSourceAnnotatedOnlyWithNamed() throws Exception {
      Named namedAnnotation = Annotations.of(Named.class, Map.of("value", "ord"));

      injector.registerInstance("A", namedAnnotation);

      assertThat(injector.getInstances(Object.class, Default.class)).hasSize(1);
      assertThat(injector.getInstances(Object.class, Any.class)).hasSize(1);
      assertThat(injector.getInstances(Object.class, namedAnnotation)).hasSize(1);

      injector.removeInstance("A", namedAnnotation);
    }

    @Test
    void shouldAddStandardAnnotationToSourceAnnotateWithNamedAndAny() throws Exception {
      Named namedAnnotation = Annotations.of(Named.class, Map.of("value", "ord"));

      injector.registerInstance("A", namedAnnotation, Annotations.of(Any.class));

      assertThat(injector.getInstances(Object.class, Default.class)).hasSize(1);
      assertThat(injector.getInstances(Object.class, Any.class)).hasSize(1);
      assertThat(injector.getInstances(Object.class, namedAnnotation)).hasSize(1);

      injector.removeInstance("A", namedAnnotation, Annotations.of(Any.class));
    }

    @Test
    void shouldAutomaticallyAddDefaultAnnotationsForFields() throws Exception {
      injector.register(B.class);
      injector.register(C.class);
      injector.register(A.class);

      A a = injector.getInstance(A.class);

      assertThat(a.defaultImplementation).isInstanceOf(B.class);
      assertThat(a.explicitDefaultImplementation).isInstanceOf(B.class);
      assertThat(a.explicitAnyDefaultImplementation).isInstanceOf(B.class);
      assertThat(a.redImplementation).isInstanceOf(C.class);
      assertThat(a.anyRedImplementation).isInstanceOf(C.class);

      injector.remove(A.class);
      injector.remove(B.class);
      injector.remove(C.class);
    }

    @Test
    void shouldAutomaticallyAddDefaultAnnotationsForParameters() throws Exception {
      injector.register(B.class);
      injector.register(C.class);
      injector.register(D.class);

      D d = injector.getInstance(D.class);

      assertThat(d.defaultImplementation).isInstanceOf(B.class);
      assertThat(d.explicitDefaultImplementation).isInstanceOf(B.class);
      assertThat(d.explicitAnyDefaultImplementation).isInstanceOf(B.class);
      assertThat(d.redImplementation).isInstanceOf(C.class);
      assertThat(d.anyRedImplementation).isInstanceOf(C.class);

      injector.remove(D.class);
      injector.remove(B.class);
      injector.remove(C.class);
    }

    public static class A {
      @Inject I defaultImplementation;     // @Default makes "I" unambiguous as it only matches B
      @Inject @Default I explicitDefaultImplementation;  // Explicit @Default makes "I" unambiguous as it only matches B
      @Inject @Default @Any I explicitAnyDefaultImplementation;  // Explicit @Any + @Default makes "I" unambiguous as it only matches B
      @Inject @Red I redImplementation;    // @Red makes it unambiguous as it only matches C
      @Inject @Any @Red I anyRedImplementation; // @Any + @Red makes it unambiguous as it only matches C
    }

    public static class D {
      I defaultImplementation;
      I explicitDefaultImplementation;
      I explicitAnyDefaultImplementation;
      I redImplementation;
      I anyRedImplementation;

      @Inject
      void setStuff(
        I defaultImplementation,     // @Default makes "I" unambiguous as it only matches B
        @Default I explicitDefaultImplementation,  // Explicit @Default makes "I" unambiguous as it only matches B
        @Default @Any I explicitAnyDefaultImplementation,  // Explicit @Any + @Default makes "I" unambiguous as it only matches B
        @Red I redImplementation,    // @Red makes it unambiguous as it only matches C
        @Any @Red I anyRedImplementation   // @Any + @Red makes it unambiguous as it only matches C
      ) {
        this.defaultImplementation = defaultImplementation;
        this.explicitDefaultImplementation = explicitDefaultImplementation;
        this.explicitAnyDefaultImplementation = explicitAnyDefaultImplementation;
        this.redImplementation = redImplementation;
        this.anyRedImplementation = anyRedImplementation;
      }
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

  public static class ScopeAnnotationSupport {
    private static String currentRequestScope;

    private final Injector injector = Injectors.manual(new AbstractScopeResolver<String>() {
      @Override
      public Annotation getAnnotation() {
        return Annotations.of(RequestScoped.class);
      }

      @Override
      protected String getCurrentScope() {
        return currentRequestScope;
      }
    });

    @Test
    void shouldInjectProxyIntoDependentTargetForNormalScopeSource() throws Exception {
      injector.register(A.class);
      injector.register(B.class);

      B b = injector.getInstance(B.class);
      assertThatThrownBy(() -> injector.getInstance(A.class)).isExactlyInstanceOf(ScopeNotActiveException.class);

      assertThat(b.a).isNotNull();
      assertThatThrownBy(() -> b.a.getScopeNameAtCreation()).isExactlyInstanceOf(ScopeNotActiveException.class);

      currentRequestScope = "1";

      assertThat(b.a.getScopeNameAtCreation()).isEqualTo("1");

      currentRequestScope = "2";

      assertThat(b.a.getScopeNameAtCreation()).isEqualTo("2");
    }

    @RequestScoped
    public static class A {
      private final String scopeNameAtCreation;

      public A() {
        this.scopeNameAtCreation = currentRequestScope;
      }

      public String getScopeNameAtCreation() {
        return scopeNameAtCreation;
      }
    }

    public static class B {
      @Inject A a;
    }
  }

  public static class InstanceSupport {
    private final Injector injector = Injectors.manual();

    @Test
    void shouldSupportInstanceProvider() throws Exception {
      Instance<Number> numbers = injector.getInstance(Types.parameterize(Instance.class, Number.class));

      // check unsupported methods:
      assertThatThrownBy(() -> numbers.destroy(null)).isExactlyInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> numbers.getHandle()).isExactlyInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> numbers.handles()).isExactlyInstanceOf(UnsupportedOperationException.class);

      // Asserts when no Numbers exist:
      assertThat(numbers.isAmbiguous()).isFalse();
      assertThat(numbers.isResolvable()).isFalse();
      assertThat(numbers.isUnsatisfied()).isTrue();
      assertThatThrownBy(() -> numbers.get()).isExactlyInstanceOf(UnsatisfiedResolutionException.class);
      assertThat(numbers.iterator()).isExhausted();

      injector.registerInstance(42);

      // Asserts when one Number exists:
      assertThat(numbers.isAmbiguous()).isFalse();
      assertThat(numbers.isResolvable()).isTrue();
      assertThat(numbers.isUnsatisfied()).isFalse();
      assertThat(numbers.get()).isEqualTo(42);
      assertThat(numbers.iterator()).toIterable().containsExactlyInAnyOrder(42);

      injector.registerInstance(84);

      // Asserts when two Numbers exists:
      assertThat(numbers.isAmbiguous()).isTrue();
      assertThat(numbers.isResolvable()).isFalse();
      assertThat(numbers.isUnsatisfied()).isFalse();
      assertThatThrownBy(() -> numbers.get()).isExactlyInstanceOf(AmbiguousResolutionException.class);
      assertThat(numbers.iterator()).toIterable().containsExactlyInAnyOrder(42, 84);
    }

    @Test
    void shouldSupportSelectWithInstanceProvider() throws Exception {
      Instance<Number> numbers = injector.getInstance(Types.parameterize(Instance.class, Number.class));

      injector.registerInstance(1);
      injector.registerInstance(2, Annotations.of(Red.class));
      injector.registerInstance(3, Annotations.of(Green.class));
      injector.registerInstance(0.0);
      injector.registerInstance(0.1, Annotations.of(Red.class));
      injector.registerInstance(0.2);

      Instance<Number> redNumbers = numbers.select(Annotations.of(Red.class));

      assertThat(redNumbers.isAmbiguous()).isTrue();
      assertThat(redNumbers.isResolvable()).isFalse();
      assertThat(redNumbers.isUnsatisfied()).isFalse();
      assertThatThrownBy(() -> redNumbers.get()).isExactlyInstanceOf(AmbiguousResolutionException.class);
      assertThat(redNumbers.iterator()).toIterable().containsExactlyInAnyOrder(2, 0.1);

      Instance<Integer> redIntegers = redNumbers.select(int.class);

      assertThat(redIntegers.isAmbiguous()).isFalse();
      assertThat(redIntegers.isResolvable()).isTrue();
      assertThat(redIntegers.isUnsatisfied()).isFalse();
      assertThat(redIntegers.get()).isEqualTo(2);
      assertThat(redIntegers.iterator()).toIterable().containsExactlyInAnyOrder(2);

      Instance<Double> doubles = numbers.select(new TypeLiteral<Double>() {});

      assertThat(doubles.isAmbiguous()).isTrue();
      assertThat(doubles.isResolvable()).isFalse();
      assertThat(doubles.isUnsatisfied()).isFalse();
      assertThatThrownBy(() -> doubles.get()).isExactlyInstanceOf(AmbiguousResolutionException.class);
      assertThat(doubles.iterator()).toIterable().containsExactlyInAnyOrder(0.0, 0.1, 0.2);

      Instance<Double> greenDoubles = doubles.select(Annotations.of(Green.class));

      assertThat(greenDoubles.isAmbiguous()).isFalse();
      assertThat(greenDoubles.isResolvable()).isFalse();
      assertThat(greenDoubles.isUnsatisfied()).isTrue();
      assertThatThrownBy(() -> greenDoubles.get()).isExactlyInstanceOf(UnsatisfiedResolutionException.class);
      assertThat(greenDoubles.iterator()).isExhausted();
    }

    @Test
    void shouldRejectIllegalSelects() throws Exception {
      Instance<Number> numbers = injector.getInstance(Types.parameterize(Instance.class, Number.class));

      assertThatThrownBy(() -> numbers.select(Annotations.of(Singleton.class)))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage("@jakarta.inject.Singleton() is not a qualifier annotation")
        .hasNoSuppressedExceptions()
        .hasNoCause();
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Red {
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Green {
    }
  }
}
