package hs.ddif.core.config;

import hs.ddif.annotations.Produces;
import hs.ddif.core.config.standard.DefaultDiscovererFactory;
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
import hs.ddif.test.util.ReplaceCamelCaseDisplayNameGenerator;

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
public class DefaultDiscovererFactoryTest {
  private final QualifiedTypeStore<Injectable<?>> store = new QualifiedTypeStore<>(i -> new Key(i.getType(), i.getQualifiers()), t -> true);
  private final InjectableFactories injectableFactories = new InjectableFactories();
  private final ClassInjectableFactory classInjectableFactory = injectableFactories.forClass();
  private final FieldInjectableFactory fieldInjectableFactory = injectableFactories.forField();
  private final MethodInjectableFactory methodInjectableFactory = injectableFactories.forMethod();

  /*
   * Note: the produced Sets by the gatherer in these tests could be incomplete or contain multiple
   * injectables of the same type; if added to a store with a consistency policy which ensures
   * dependencies can always be met, these sets will be rejected. The gatherer however did its job
   * correctly and just returns all possible injectables it found.
   */

  @Nested
  class When_autoDiscovery_isDisabled {
    private final DefaultDiscovererFactory gatherer = new DefaultDiscovererFactory(false, List.of(new ProducesInjectableExtension(methodInjectableFactory, fieldInjectableFactory, Produces.class)), classInjectableFactory);

    @Nested
    class And_gather_With_Injectable_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.create(store, classInjectableFactory.create(A.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class)
        );
      }
    }

    @Nested
    class And_gather_With_Key_IsCalled {
      @Test
      void shouldAlwaysReturnEmptySet() {
        assertThat(gatherer.create(store, new Key(A.class)).discover()).isEmpty();
        assertThat(gatherer.create(store, new Key(A.class, Set.of(Annotations.of(Red.class)))).discover()).isEmpty();
        assertThat(gatherer.create(store, new Key(I.class)).discover()).isEmpty();
        assertThat(gatherer.create(store, new Key(Bad_C.class)).discover()).isEmpty();
      }
    }

    @Nested
    class And_gather_With_Types_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.create(store, List.of(A.class, D.class)).discover()).containsExactlyInAnyOrder(
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
    private final DefaultDiscovererFactory gatherer = new DefaultDiscovererFactory(true, List.of(new ProducesInjectableExtension(methodInjectableFactory, fieldInjectableFactory, Produces.class)), classInjectableFactory);

    @Nested
    class And_gather_With_Injectable_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.create(store, classInjectableFactory.create(A.class)).discover()).containsExactlyInAnyOrder(
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
        assertThat(gatherer.create(store, new Key(A.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          classInjectableFactory.create(D.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class)
        );
      }

      @Test
      void shouldDiscoverThroughBindingsOfProducerMethod() throws Exception {
        assertThat(gatherer.create(store, new Key(F.class)).discover()).containsExactlyInAnyOrder(
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
        assertThat(gatherer.create(store, new Key(H.class)).discover()).containsExactlyInAnyOrder(
          fieldInjectableFactory.create(H.class.getDeclaredField("h"), H.class)
        );
      }

      @Test
      void shouldDiscoverThroughBindingsClassesWhichProduceThemselves() throws Exception {
        assertThat(gatherer.create(store, new Key(K.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(K.class),
          fieldInjectableFactory.create(H.class.getDeclaredField("h"), H.class)
        );
      }

      @Test
      void shouldReturnEmptySetWhenTypeAlreadyResolvable() {
        store.put(classInjectableFactory.create(A.class));

        assertThat(gatherer.create(store, new Key(A.class)).discover()).isEmpty();
      }

      @Test
      void shouldRejectWhenDiscoveredTypeMissesRequiredQualifiers() {
        assertThatThrownBy(() -> gatherer.create(store, new Key(A.class, Set.of(Annotations.of(Red.class)))).discover())
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("Exception occurred during discovery via path: [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.config.DefaultDiscovererFactoryTest$A]")
          .satisfies(throwable -> {
            assertThat(throwable.getSuppressed()).hasSize(1);
            assertThat(throwable.getSuppressed()[0])
              .isExactlyInstanceOf(DefinitionException.class)
              .hasMessage("[class hs.ddif.core.config.DefaultDiscovererFactoryTest$A] found during auto discovery is missing qualifiers required by: [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.config.DefaultDiscovererFactoryTest$A]")
              .hasNoCause();
          })
          .hasNoCause();
      }

      @Test
      void shouldRejectTypeThatIsAbstract() {
        assertThatThrownBy(() -> gatherer.create(store, new Key(I.class)).discover())
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("Exception occurred during discovery via path: [hs.ddif.core.config.DefaultDiscovererFactoryTest$I]")
          .satisfies(throwable -> {
            assertThat(throwable.getSuppressed()).hasSize(1);
            assertThat(throwable.getSuppressed()[0])
              .isExactlyInstanceOf(DefinitionException.class)
              .hasMessage("[interface hs.ddif.core.config.DefaultDiscovererFactoryTest$I] cannot be abstract")
              .hasNoCause();
          })
          .hasNoCause();
      }

      @Test
      void shouldNotIncludeTypeThatHasQualifiers() {
        assertThat(gatherer.create(store, new Key(Bad_C.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_C.class)
          // J was not included as it was required to have qualifiers
        );
      }

      @Test
      void shouldNotIncludeUndiscoverableDependency() {
        assertThat(gatherer.create(store, new Key(Bad_A.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_A.class)
          // C was not included as it has no suitable constructor
        );
      }

      @Test
      void shouldNotIncludeUndiscoverableDependencyInDependency() {
        assertThat(gatherer.create(store, new Key(Bad_D.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_D.class),
          classInjectableFactory.create(Bad_A.class)
          // C through Bad_A was not included as it has no suitable constructor
        );
      }

      @Test
      void shouldNotIncludeUndiscoverableDependencies() {
        assertThat(gatherer.create(store, new Key(Bad_B.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_B.class)
          // C and E are not included as neither has a suitable constructor
        );
      }

      @Test
      void shouldRejectTypeWhichCanBeDerivedOrConstructedInMultipleWays() {
        assertThatThrownBy(() -> gatherer.create(store, new Key(Bad_H.class)).discover())
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("Exception occurred during discovery via path: [hs.ddif.core.config.DefaultDiscovererFactoryTest$Bad_H]")
          .hasSuppressedException(new DefinitionException("[class hs.ddif.core.config.DefaultDiscovererFactoryTest$Bad_H] creation is ambiguous, there are multiple ways to create it", null))
          .hasNoCause();
      }
    }

    @Nested
    class And_gather_With_Types_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(gatherer.create(store, List.of(Y.class)).discover()).containsExactlyInAnyOrder(
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

        assertThatThrownBy(() -> gatherer.create(store, List.of(X.class, Y.class, Z.class)).discover())
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("Exception occurred during discovery via path: [hs.ddif.core.config.DefaultDiscovererFactoryTest$X]")
          .satisfies(throwable -> {
            assertThat(throwable.getSuppressed()).hasSize(2);
            assertThat(throwable.getSuppressed()[0])
              .isExactlyInstanceOf(DefinitionException.class)
              .hasMessage("[class hs.ddif.core.config.DefaultDiscovererFactoryTest$X] could not be bound")
              .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
              .isExactlyInstanceOf(BindingException.class)
              .hasMessage("[class hs.ddif.core.config.DefaultDiscovererFactoryTest$X] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
              .hasNoCause();
            assertThat(throwable.getSuppressed()[1])
              .isExactlyInstanceOf(DefinitionException.class)
              .hasMessage("[class hs.ddif.core.config.DefaultDiscovererFactoryTest$Z] could not be bound")
              .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
              .isExactlyInstanceOf(BindingException.class)
              .hasMessage("[class hs.ddif.core.config.DefaultDiscovererFactoryTest$Z] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
              .hasNoCause();
          })
          .hasNoCause();

        assertThatThrownBy(() -> gatherer.create(store, List.of(X.class, Y.class)).discover())
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("Exception occurred during discovery via path: [hs.ddif.core.config.DefaultDiscovererFactoryTest$X]")
          .satisfies(throwable -> {
            assertThat(throwable.getSuppressed()).hasSize(1);
            assertThat(throwable.getSuppressed()[0])
              .isExactlyInstanceOf(DefinitionException.class)
              .hasMessage("[class hs.ddif.core.config.DefaultDiscovererFactoryTest$X] could not be bound")
              .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
              .isExactlyInstanceOf(BindingException.class)
              .hasMessage("[class hs.ddif.core.config.DefaultDiscovererFactoryTest$X] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
              .hasNoCause();
          })
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

  interface I {
    void createJ();
  }

  public static class J {
  }
}
