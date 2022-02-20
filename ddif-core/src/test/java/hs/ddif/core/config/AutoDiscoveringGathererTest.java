package hs.ddif.core.config;

import hs.ddif.annotations.Produces;
import hs.ddif.core.config.gather.DiscoveryFailure;
import hs.ddif.core.config.standard.AutoDiscoveringGatherer;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.DefinitionException;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.InjectableFactories;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.definition.bind.BindingException;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.QualifiedTypeStore;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;
import hs.ddif.core.util.ReplaceCamelCaseDisplayNameGenerator;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(ReplaceCamelCaseDisplayNameGenerator.class)
public class AutoDiscoveringGathererTest {
  private final QualifiedTypeStore<Injectable> store = new QualifiedTypeStore<>(i -> new Key(i.getType(), i.getQualifiers()));
  private final ClassInjectableFactory classInjectableFactory = InjectableFactories.forClass();
  private final FieldInjectableFactory fieldInjectableFactory = InjectableFactories.forField();
  private final MethodInjectableFactory methodInjectableFactory = InjectableFactories.forMethod();

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
        assertThat(gatherer.gather(store, classInjectableFactory.create(A.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class)
        );
      }

      @Test
      void shouldRejectNestedProvider() {
        assertThatThrownBy(() -> gatherer.gather(store, classInjectableFactory.create(Bad_P.class)))
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("Method [javax.inject.Provider hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_P.oops()] cannot have a return type with a nested Provider")
          .hasNoCause();
      }
    }

    @Nested
    class And_gather_With_Key_IsCalled {
      @Test
      void shouldAlwaysReturnEmptySet() throws DiscoveryFailure {
        assertThat(gatherer.gather(store, new Key(A.class))).isEmpty();
        assertThat(gatherer.gather(store, new Key(A.class, Set.of(Annotations.of(Red.class))))).isEmpty();
        assertThat(gatherer.gather(store, new Key(I.class))).isEmpty();
        assertThat(gatherer.gather(store, new Key(Bad_C.class))).isEmpty();
      }
    }

    @Nested
    class And_gather_With_Types_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.gather(store, List.of(A.class, D.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class),
          classInjectableFactory.create(D.class)
        );
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
        assertThat(gatherer.gather(store, classInjectableFactory.create(A.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          classInjectableFactory.create(D.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class)
        );
      }
    }

    @Nested
    class And_gather_With_Key_IsCalled {
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
      void shouldDiscoverThroughBindingsOfProducerMethod() throws Exception {
        assertThat(gatherer.gather(store, new Key(F.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          classInjectableFactory.create(D.class),
          classInjectableFactory.create(F.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class),
          methodInjectableFactory.create(F.class.getDeclaredMethod("takes", B.class, A.class), F.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class)
        );
      }

      @Test
      void shouldDiscoverClassWhichProducesItself() throws Exception {
        assertThat(gatherer.gather(store, new Key(H.class))).containsExactlyInAnyOrder(
          fieldInjectableFactory.create(H.class.getDeclaredField("h"), H.class)
        );
      }

      @Test
      void shouldDiscoverThroughBindingsClassesWhichProduceThemselves() throws Exception {
        assertThat(gatherer.gather(store, new Key(K.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(K.class),
          fieldInjectableFactory.create(H.class.getDeclaredField("h"), H.class)
        );
      }

      @Test
      void shouldReturnEmptySetWhenTypeAlreadyResolvable() throws DiscoveryFailure {
        store.put(classInjectableFactory.create(A.class));

        assertThat(gatherer.gather(store, new Key(A.class))).isEmpty();
      }

      @Test
      void shouldRejectWhenDiscoveredTypeMissesRequiredQualifiers() {
        assertThatThrownBy(() -> gatherer.gather(store, new Key(A.class, Set.of(Annotations.of(Red.class)))))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Path [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.config.AutoDiscoveringGathererTest$A]: [class hs.ddif.core.config.AutoDiscoveringGathererTest$A] found during auto discovery is missing qualifiers required by: [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.config.AutoDiscoveringGathererTest$A]")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("[class hs.ddif.core.config.AutoDiscoveringGathererTest$A] found during auto discovery is missing qualifiers required by: [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.config.AutoDiscoveringGathererTest$A]")
          .hasNoCause();
      }

      @Test
      void shouldRejectTypeThatIsAbstract() {
        assertThatThrownBy(() -> gatherer.gather(store, new Key(I.class)))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Path [hs.ddif.core.config.AutoDiscoveringGathererTest$I]: [interface hs.ddif.core.config.AutoDiscoveringGathererTest$I] cannot be injected; failures:\n"
            + " - Type cannot be abstract: interface hs.ddif.core.config.AutoDiscoveringGathererTest$I"
          )
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("[interface hs.ddif.core.config.AutoDiscoveringGathererTest$I] cannot be injected; failures:\n"
            + " - Type cannot be abstract: interface hs.ddif.core.config.AutoDiscoveringGathererTest$I"
          )
          .hasNoCause();
      }

      @Test
      void shouldNotIncludeTypeThatHasQualifiers() throws DiscoveryFailure {
        assertThat(gatherer.gather(store, new Key(Bad_C.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_C.class)
          // J was not included as it was required to have qualifiers
        );
      }

      @Test
      void shouldNotIncludeUndiscoverableDependency() throws DiscoveryFailure {
        assertThat(gatherer.gather(store, new Key(Bad_A.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_A.class)
          // C was not included as it has no suitable constructor
        );
      }

      @Test
      void shouldNotIncludeUndiscoverableDependencyInDependency() throws DiscoveryFailure {
        assertThat(gatherer.gather(store, new Key(Bad_D.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_D.class),
          classInjectableFactory.create(Bad_A.class)
          // C through Bad_A was not included as it has no suitable constructor
        );
      }

      @Test
      void shouldNotIncludeUndiscoverableDependencies() throws DiscoveryFailure {
        assertThat(gatherer.gather(store, new Key(Bad_B.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_B.class)
          // C and E are not included as neither has a suitable constructor
        );
      }

      @Test
      void shouldRejectTypeWhichCanBeDerivedOrConstructedInMultipleWays() {
        assertThatThrownBy(() -> gatherer.gather(store, new Key(Bad_H.class)))
          .isExactlyInstanceOf(DiscoveryFailure.class)
          .hasMessage("Path [hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_H]: [class hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_H] creation is ambiguous, there are multiple ways to create it")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("[class hs.ddif.core.config.AutoDiscoveringGathererTest$Bad_H] creation is ambiguous, there are multiple ways to create it")
          .hasNoCause();
      }
    }

    @Nested
    class And_gather_With_Types_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.gather(store, List.of(Y.class))).containsExactlyInAnyOrder(
          classInjectableFactory.create(Y.class),
          fieldInjectableFactory.create(W.class.getDeclaredField("w"), W.class),
          fieldInjectableFactory.create(W.class.getDeclaredField("x"), W.class),
          fieldInjectableFactory.create(W.class.getDeclaredField("z"), W.class)
        );
      }

      @Test
      void shouldRejectTypesThatCannotBeCreatedWithoutBindingDiscovery() {

        /*
         * This is a complicated case. If auto discovery is on, the following happens:
         *
         *  - X, Y and Z need to be created
         *  - No producers are found initially
         *  - Injectable discovery takes place, X and Z fail as they have no suitable constructor
         *  - Y however is created
         *
         * Now through Y a binding to W can be found which will locate producers for X and Z,
         * however, even though this would indeed result in injectables for X and Z, this is not
         * what was intended when a call is made to register X, Y and Z -- it is expected that
         * simple directly constructable injectables should result for all inputs. If only
         * registering Y then this is acceptable as X and Z could then be considered "derived"
         * from Y.
         */

        assertThatThrownBy(() -> gatherer.gather(store, List.of(X.class, Y.class, Z.class)))
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("Path [hs.ddif.core.config.AutoDiscoveringGathererTest$X]: [class hs.ddif.core.config.AutoDiscoveringGathererTest$X] cannot be injected")
          .hasSuppressedException(new DefinitionException("Path [hs.ddif.core.config.AutoDiscoveringGathererTest$Z]: [class hs.ddif.core.config.AutoDiscoveringGathererTest$Z] cannot be injected", null))
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("[class hs.ddif.core.config.AutoDiscoveringGathererTest$X] cannot be injected")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("[class hs.ddif.core.config.AutoDiscoveringGathererTest$X] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
          .hasNoCause();

        assertThatThrownBy(() -> gatherer.gather(store, List.of(X.class, Y.class)))
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("Path [hs.ddif.core.config.AutoDiscoveringGathererTest$X]: [class hs.ddif.core.config.AutoDiscoveringGathererTest$X] cannot be injected")
          .hasNoSuppressedExceptions()
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("[class hs.ddif.core.config.AutoDiscoveringGathererTest$X] cannot be injected")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("[class hs.ddif.core.config.AutoDiscoveringGathererTest$X] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
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

  public static class F {
    @SuppressWarnings("unused")
    @Produces G takes(B b, A a) {
      return null;
    }
  }

  public static class G {
  }

  public static class H {
    @Produces static H h = new H(2);

    int i;

    private H(int i) {
      this.i = i;
    }
  }

  public static class K {
    @Inject H h;
  }

  public static class W {
    @Produces static W w = new W(2);
    @Produces X x = new X("hello");
    @Produces Z z = new Z("world");

    private W(@SuppressWarnings("unused") int i) {
    }
  }

  public static class X {
    public X(@SuppressWarnings("unused") String important) {
    }
  }

  public static class Y {
    @Inject W w;
  }

  public static class Z {
    public Z(@SuppressWarnings("unused") String important) {
    }
  }

  /**
   * Bad because C cannot be discovered (C has no suitable constructor).
   */
  public static class Bad_A {
    @Inject C c;
  }

  /**
   * Bad because neither C or E can be discovered (no suitable constructors).
   */
  public static class Bad_B {
    @Inject E e;
    @Inject C c;
  }

  /**
   * Bad because J, although constructable, does not have the Red qualifier.
   */
  public static class Bad_C {
    @Inject @Red J j;  // auto discovery a class J with qualifier Red is not possible
  }

  /**
   * Bad because Bad_A, although discoverable, has problems of its own.
   */
  public static class Bad_D {
    @Inject Bad_A a;
  }

  /**
   * Bad because there are two ways available to construct it, through the static
   * producer and through its constructor.
   */
  public static class Bad_H {
    @Produces static Bad_H h = new Bad_H();
  }

  /**
   * Bad because it uses a nested Provider.
   */
  public static class Bad_P {
    @Produces Provider<Provider<String>> oops() {
      return null;
    }
  }

  interface I {
    void createJ();
  }

  public static class J {
  }
}
