package hs.ddif.core.inject.store;

import hs.ddif.annotations.Produces;
import hs.ddif.core.ProducesGathererExtension;
import hs.ddif.core.inject.instantiator.DiscoveryFailure;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.ReplaceCamelCaseDisplayNameGenerator;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(ReplaceCamelCaseDisplayNameGenerator.class)
public class AutoDiscoveringGathererTest {
  private final InjectableStore<ResolvableInjectable> store = new InjectableStore<>();

  /*
   * Note: the produced Sets by the gatherer in these tests could be incomplete or contain multiple
   * injectables of the same type; if added to a store with a consistency policy which ensures
   * dependencies can always be met, these sets will be rejected. The gatherer however did its job
   * correctly and just returns all possible injectables it found.
   */

  @Nested
  class When_autoDiscovery_isDisabled {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(store, false, List.of(new ProducesGathererExtension()));

    @Nested
    class And_gather_With_ResolvableInjectable_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.gather(Set.of(new ClassInjectable(A.class)))).containsExactlyInAnyOrder(
          new ClassInjectable(A.class),
          new FieldInjectable(A.class.getDeclaredField("b"), A.class),
          new MethodInjectable(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          new FieldInjectable(B.class.getDeclaredField("e"), B.class)
        );
      }
    }

    @Nested
    class And_gather_With_Type_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.gather(A.class)).containsExactlyInAnyOrder(
          new ClassInjectable(A.class),
          new FieldInjectable(A.class.getDeclaredField("b"), A.class),
          new MethodInjectable(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          new FieldInjectable(B.class.getDeclaredField("e"), B.class)
        );
      }

      @Test
      void shouldRejectTypeThatIsAbstract() {
        assertThatThrownBy(() -> gatherer.gather(I.class))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Exception during auto discovery: interface hs.ddif.core.inject.store.AutoDiscoveringGathererTest$I")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("Type cannot be abstract: interface hs.ddif.core.inject.store.AutoDiscoveringGathererTest$I")
          .hasNoCause();
      }

      @Test
      void shouldNotDoDiscovery() throws DiscoveryFailure {
        assertThat(gatherer.gather(Bad_C.class)).containsExactlyInAnyOrder(
          new ClassInjectable(Bad_C.class)
        );
      }
    }
  }


  @Nested
  class When_autoDiscovery_isEnabled {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(store, true, List.of(new ProducesGathererExtension()));

    @Nested
    class And_gather_With_ResolvableInjectable_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.gather(Set.of(new ClassInjectable(A.class)))).containsExactlyInAnyOrder(
          new ClassInjectable(A.class),
          new FieldInjectable(A.class.getDeclaredField("b"), A.class),
          new ClassInjectable(D.class),
          new MethodInjectable(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          new FieldInjectable(B.class.getDeclaredField("e"), B.class)
        );
      }
    }

    @Nested
    class And_gather_With_Type_IsCalled {
      @Test
      void shouldDiscoverTypesAndFindProducedTypes() throws Exception {
        assertThat(gatherer.gather(A.class)).containsExactlyInAnyOrder(
          new ClassInjectable(A.class),
          new FieldInjectable(A.class.getDeclaredField("b"), A.class),
          new ClassInjectable(D.class),
          new MethodInjectable(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          new FieldInjectable(B.class.getDeclaredField("e"), B.class)
        );
      }

      @Test
      void shouldRejectTypeThatIsAbstract() {
        assertThatThrownBy(() -> gatherer.gather(I.class))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Exception during auto discovery: interface hs.ddif.core.inject.store.AutoDiscoveringGathererTest$I")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("Type cannot be abstract: interface hs.ddif.core.inject.store.AutoDiscoveringGathererTest$I")
          .hasNoCause();
      }

      @Test
      void shouldRejectTypeThatHasQualifiers() {
        assertThatThrownBy(() -> gatherer.gather(Bad_C.class))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Exception during auto discovery: class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$Bad_C")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("Unable to inject: Field [hs.ddif.core.inject.store.AutoDiscoveringGathererTest$J hs.ddif.core.inject.store.AutoDiscoveringGathererTest$Bad_C.j] with: [@hs.ddif.core.test.qualifiers.Red class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$J]")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("Auto discovered class cannot be required to have qualifiers: [@hs.ddif.core.test.qualifiers.Red class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$J]")
          .hasNoCause();
      }

      @Test
      void shouldRejectTypeWithUndiscoverableDependency() {
        assertThatThrownBy(() -> gatherer.gather(Bad_A.class))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Exception during auto discovery: class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$Bad_A")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("Unable to inject: Field [hs.ddif.core.inject.store.AutoDiscoveringGathererTest$C hs.ddif.core.inject.store.AutoDiscoveringGathererTest$Bad_A.c] with: [class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$C]")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$C")
          .hasNoSuppressedExceptions()
          .hasNoCause();
      }

      @Test
      void shouldRejectTypeWithMultipleUndiscoverableDependenciesAndUseSuppressedExceptionsForDetails() {
        assertThatThrownBy(() -> gatherer.gather(Bad_B.class))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Exception during auto discovery: class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$Bad_B")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("Unable to resolve 2 binding(s) while processing extensions")
          .hasSuppressedException(new BindingException("Unable to inject: Field [hs.ddif.core.inject.store.AutoDiscoveringGathererTest$E hs.ddif.core.inject.store.AutoDiscoveringGathererTest$Bad_B.e] with: [class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$E]", new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$E")))
          .hasSuppressedException(new BindingException("Unable to inject: Field [hs.ddif.core.inject.store.AutoDiscoveringGathererTest$C hs.ddif.core.inject.store.AutoDiscoveringGathererTest$Bad_B.c] with: [class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$C]", new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.inject.store.AutoDiscoveringGathererTest$C")))
          .hasNoCause();
      }
    }
  }

  /*
   * Test classes:
   */

  public static class A {
    @Produces B b = new B();
    @Produces C createC(D d, E e) {
      return new C(d, e);
    }
  }

  public static class B {
    @Produces static E e = new E("!");
  }

  public static class C {
    final D d;
    final E e;

    public C(D d, E e) {
      this.d = d;
      this.e = e;
    }
  }

  public static class D {
  }

  public static class E {
    final String important;

    public E(String important) {
      this.important = important;
    }
  }

  public static class Bad_A {
    @Inject C c;
  }

  public static class Bad_B {
    @Inject E e;
    @Inject C c;
  }

  public static class Bad_C {
    @Inject @Red J j;  // auto discovery a class J with qualifier Red is not possible
  }

  interface I {
    void createJ();
  }

  public static class J {
  }
}
