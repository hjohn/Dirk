package hs.ddif.core.config;

import hs.ddif.annotations.Produces;
import hs.ddif.core.config.gather.DiscoveryFailure;
import hs.ddif.core.config.standard.AutoDiscoveringGatherer;
import hs.ddif.core.config.standard.DefaultBinding;
import hs.ddif.core.config.standard.DefaultInjectable;
import hs.ddif.core.inject.bind.BindingException;
import hs.ddif.core.inject.bind.BindingProvider;
import hs.ddif.core.inject.injectable.ClassInjectableFactory;
import hs.ddif.core.inject.injectable.FieldInjectableFactory;
import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.inject.injectable.InjectableFactories;
import hs.ddif.core.inject.injectable.MethodInjectableFactory;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.QualifiedTypeStore;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;
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
  private final BindingProvider bindingProvider = new BindingProvider(DefaultBinding::new);
  private final QualifiedTypeStore<Injectable> store = new QualifiedTypeStore<>();
  private final ClassInjectableFactory classInjectableFactory = InjectableFactories.forClass();
  private final FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, DefaultInjectable::new);
  private final MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, DefaultInjectable::new);

  /*
   * Note: the produced Sets by the gatherer in these tests could be incomplete or contain multiple
   * injectables of the same type; if added to a store with a consistency policy which ensures
   * dependencies can always be met, these sets will be rejected. The gatherer however did its job
   * correctly and just returns all possible injectables it found.
   */

  @Nested
  class When_autoDiscovery_isDisabled {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(false, List.of(new ProducesGathererExtension(methodInjectableFactory, fieldInjectableFactory)), classInjectableFactory);

    @Nested
    class And_gather_With_Injectable_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.gather(store, Set.of(classInjectableFactory.create(A.class)))).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class)
        );
      }
    }

    @Nested
    class And_gather_With_Type_IsCalled {
      @Test
      void shouldAlwaysReturnEmptySet() throws DiscoveryFailure {
        assertThat(gatherer.gather(store, new Key(A.class))).isEmpty();
        assertThat(gatherer.gather(store, new Key(A.class, Set.of(Annotations.of(Red.class))))).isEmpty();
        assertThat(gatherer.gather(store, new Key(I.class))).isEmpty();
        assertThat(gatherer.gather(store, new Key(Bad_C.class))).isEmpty();
      }
    }
  }

  @Nested
  class When_autoDiscovery_isEnabled {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(true, List.of(new ProducesGathererExtension(methodInjectableFactory, fieldInjectableFactory)), classInjectableFactory);

    @Nested
    class And_gather_With_Injectable_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.gather(store, Set.of(classInjectableFactory.create(A.class)))).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          classInjectableFactory.create(D.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class)
        );
      }
    }

    @Nested
    class And_gather_With_Type_IsCalled {
      @Test
      void shouldDiscoverTypesAndFindProducedTypes() throws Exception {
        assertThat(gatherer.gather(store, new Key(A.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          classInjectableFactory.create(D.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class)
        );
      }

      @Test
      void shouldReturnEmptySetWhenTypeUnsuitableForAutoDiscovery() throws DiscoveryFailure {
        assertThat(gatherer.gather(store, new Key(A.class, Set.of(Annotations.of(Red.class))))).isEmpty();
      }

      @Test
      void shouldReturnEmptySetWhenTypeAlreadyResolvable() throws DiscoveryFailure {
        store.put(classInjectableFactory.create(A.class));

        assertThat(gatherer.gather(store, new Key(A.class))).isEmpty();
      }

      @Test
      void shouldRejectTypeThatIsAbstract() {
        assertThatThrownBy(() -> gatherer.gather(store, new Key(I.class)))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Exception during auto discovery: [interface hs.ddif.core.config.AutoDiscoveringGathererTest$I]")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessageStartingWith("Type cannot be injected: interface hs.ddif.core.config.AutoDiscoveringGathererTest$I")
          .hasNoCause();
      }

      @Test
      void shouldRejectTypeThatHasQualifiers() {
        assertThatThrownBy(() -> gatherer.gather(store, new Key(Bad_C.class)))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Exception during auto discovery: [class hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_C]")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("Unable to inject: Field [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.config.AutoDiscoveringGathererTest$J hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_C.j] with: [@hs.ddif.core.test.qualifiers.Red() class hs.ddif.core.config.AutoDiscoveringGathererTest$J]")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("Auto discovered class cannot be required to have qualifiers: [@hs.ddif.core.test.qualifiers.Red() class hs.ddif.core.config.AutoDiscoveringGathererTest$J]")
          .hasNoCause();
      }

      @Test
      void shouldRejectTypeWithUndiscoverableDependency() {
        assertThatThrownBy(() -> gatherer.gather(store, new Key(Bad_A.class)))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Exception during auto discovery: [class hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_A]")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("Unable to inject: Field [hs.ddif.core.config.AutoDiscoveringGathererTest$C hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_A.c] with: [class hs.ddif.core.config.AutoDiscoveringGathererTest$C]")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("No suitable constructor found; annotate a constructor or provide an empty public constructor: class hs.ddif.core.config.AutoDiscoveringGathererTest$C")
          .hasNoSuppressedExceptions()
          .hasNoCause();
      }

      @Test
      void shouldRejectTypeWithMultipleUndiscoverableDependenciesAndUseSuppressedExceptionsForDetails() {
        assertThatThrownBy(() -> gatherer.gather(store, new Key(Bad_B.class)))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Exception during auto discovery: [class hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_B]")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("Unable to resolve 2 binding(s) while processing extensions")
          .hasSuppressedException(new BindingException("Unable to inject: Field [hs.ddif.core.config.AutoDiscoveringGathererTest$E hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_B.e] with: [class hs.ddif.core.config.AutoDiscoveringGathererTest$E]", new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.config.AutoDiscoveringGathererTest$E")))
          .hasSuppressedException(new BindingException("Unable to inject: Field [hs.ddif.core.config.AutoDiscoveringGathererTest$C hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_B.c] with: [class hs.ddif.core.config.AutoDiscoveringGathererTest$C]", new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.config.AutoDiscoveringGathererTest$C")))
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
