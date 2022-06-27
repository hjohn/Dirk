package org.int4.dirk.core;

import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.int4.dirk.annotations.Produces;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.definition.BadQualifiedTypeException;
import org.int4.dirk.core.definition.ClassInjectableFactory;
import org.int4.dirk.core.definition.FieldInjectableFactory;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.definition.MethodInjectableFactory;
import org.int4.dirk.core.discovery.Discoverer;
import org.int4.dirk.core.store.QualifiedTypeStore;
import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.core.util.Key;
import org.int4.dirk.library.ProducesTypeRegistrationExtension;
import org.int4.dirk.test.util.ReplaceCamelCaseDisplayNameGenerator;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

@DisplayNameGeneration(ReplaceCamelCaseDisplayNameGenerator.class)
public class DefaultDiscovererFactoryTest {
  private final QualifiedTypeStore<Injectable<?>> store = new QualifiedTypeStore<>(i -> new Key(i.getType(), i.getQualifiers()), Injectable::getTypes);
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
    private final DefaultDiscovererFactory factory = new DefaultDiscovererFactory(false, List.of(new ProducesTypeRegistrationExtension(Produces.class)), classInjectableFactory, methodInjectableFactory, fieldInjectableFactory);

    @Nested
    class And_gather_With_Injectable_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(factory.create(store, classInjectableFactory.create(A.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class)
        );
      }
    }

    @Nested
    class And_gather_With_Types_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(factory.create(store, List.of(A.class, D.class)).discover()).containsExactlyInAnyOrder(
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
    private final DefaultDiscovererFactory factory = new DefaultDiscovererFactory(true, List.of(new ProducesTypeRegistrationExtension(Produces.class)), classInjectableFactory, methodInjectableFactory, fieldInjectableFactory);

    @Nested
    class And_gather_With_Injectable_IsCalled {
      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(factory.create(store, classInjectableFactory.create(A.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          classInjectableFactory.create(D.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class)
        );
      }
    }

    @Nested
    class And_gather_With_Types_IsCalled {

      @Test
      void shouldDiscoverTypesAndFindProducedTypes() throws Exception {
        assertThat(factory.create(store, List.of(A.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(A.class),
          fieldInjectableFactory.create(A.class.getDeclaredField("b"), A.class),
          classInjectableFactory.create(D.class),
          methodInjectableFactory.create(A.class.getDeclaredMethod("createC", D.class, E.class), A.class),
          fieldInjectableFactory.create(B.class.getDeclaredField("e"), B.class)
        );
      }

      @Test
      void shouldDiscoverThroughBindingsOfProducerMethod() throws Exception {
        assertThat(factory.create(store, List.of(F.class)).discover()).containsExactlyInAnyOrder(
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
        assertThat(factory.create(store, List.of(H.class)).discover()).containsExactlyInAnyOrder(
          fieldInjectableFactory.create(H.class.getDeclaredField("h"), H.class)
        );
      }

      @Test
      void shouldDiscoverThroughBindingsClassesWhichProduceThemselves() throws Exception {
        assertThat(factory.create(store, List.of(K.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(K.class),
          fieldInjectableFactory.create(H.class.getDeclaredField("h"), H.class)
        );
      }

      @Test
      void shouldDiscoverThroughBindingClassesWrappedInProvider() throws Exception {
        assertThat(factory.create(store, List.of(L.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(L.class),
          classInjectableFactory.create(G.class)
        );
      }

      @Test
      void shouldRejectTypeThatIsAbstract() {
        assertThatThrownBy(() -> factory.create(store, List.of(I.class)).discover())
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("[interface org.int4.dirk.core.DefaultDiscovererFactoryTest$I] cannot be abstract")
          .hasNoSuppressedExceptions()
          .hasNoCause();
      }

      @Test
      void shouldDiscoverAbstractTypeThatProducesItself() throws Exception {
        assertThat(factory.create(store, List.of(M.class)).discover()).containsExactlyInAnyOrder(
          fieldInjectableFactory.create(M.class.getDeclaredField("m"), M.class)
        );
      }

      @Test
      void shouldNotIncludeTypeThatHasQualifiers() throws DefinitionException {
        Discoverer discoverer = factory.create(store, List.of(Bad_C.class));

        assertThat(discoverer.discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_C.class)
          // J was not included as it was required to have qualifiers
        );

        assertThat(discoverer.getProblems()).containsExactlyInAnyOrder(
          "[@org.int4.dirk.core.test.qualifiers.Red() org.int4.dirk.core.DefaultDiscovererFactoryTest$J] required by [org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_C], via Field [@org.int4.dirk.core.test.qualifiers.Red() org.int4.dirk.core.DefaultDiscovererFactoryTest$J org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_C.j], is not registered and cannot be discovered (reason: [class org.int4.dirk.core.DefaultDiscovererFactoryTest$J] is missing the required qualifiers: [@org.int4.dirk.core.test.qualifiers.Red()])"
        );
      }

      @Test
      void shouldNotIncludeUndiscoverableDependency() throws DefinitionException {
        Discoverer discoverer = factory.create(store, List.of(Bad_A.class));

        assertThat(discoverer.discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_A.class)
          // C was not included as it has no suitable constructor
        );

        assertThat(discoverer.getProblems()).containsExactlyInAnyOrder(
          "[org.int4.dirk.core.DefaultDiscovererFactoryTest$C] required by [org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_A], via Field [org.int4.dirk.core.DefaultDiscovererFactoryTest$C org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_A.c], is not registered and cannot be discovered (reason: [class org.int4.dirk.core.DefaultDiscovererFactoryTest$C] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)"
        );
      }

      @Test
      void shouldNotIncludeUndiscoverableDependencyInDependency() throws DefinitionException {
        Discoverer discoverer = factory.create(store, List.of(Bad_D.class));

        assertThat(discoverer.discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_D.class),
          classInjectableFactory.create(Bad_A.class)
          // C through Bad_A was not included as it has no suitable constructor
        );

        assertThat(discoverer.getProblems()).containsExactlyInAnyOrder(
          "[org.int4.dirk.core.DefaultDiscovererFactoryTest$C] required by [org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_A] required by [org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_D], via Field [org.int4.dirk.core.DefaultDiscovererFactoryTest$C org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_A.c], is not registered and cannot be discovered (reason: [class org.int4.dirk.core.DefaultDiscovererFactoryTest$C] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)"
        );
      }

      @Test
      void shouldNotIncludeUndiscoverableDependencies() throws DefinitionException {
        Discoverer discoverer = factory.create(store, List.of(Bad_B.class));

        assertThat(discoverer.discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_B.class)
          // C and E are not included as neither has a suitable constructor
        );

        assertThat(discoverer.getProblems()).containsExactlyInAnyOrder(
          "[org.int4.dirk.core.DefaultDiscovererFactoryTest$E] required by [org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_B], via Field [org.int4.dirk.core.DefaultDiscovererFactoryTest$E org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_B.e], is not registered and cannot be discovered (reason: [class org.int4.dirk.core.DefaultDiscovererFactoryTest$E] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)",
          "[org.int4.dirk.core.DefaultDiscovererFactoryTest$C] required by [org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_B], via Field [org.int4.dirk.core.DefaultDiscovererFactoryTest$C org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_B.c], is not registered and cannot be discovered (reason: [class org.int4.dirk.core.DefaultDiscovererFactoryTest$C] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)"
        );
      }

      @Test
      void shouldReturnAllWaysTypeCanBeCreated() throws Exception {
        assertThat(factory.create(store, List.of(P.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(P.class),
          fieldInjectableFactory.create(P.class.getDeclaredField("p"), P.class)
        );
      }

      @Test
      void shouldFindProducedTypes() throws Exception {
        assertThat(factory.create(store, List.of(Y.class)).discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(Y.class),
          fieldInjectableFactory.create(W.class.getDeclaredField("w"), W.class),
          fieldInjectableFactory.create(W.class.getDeclaredField("x"), W.class),
          fieldInjectableFactory.create(W.class.getDeclaredField("z"), W.class)
        );
      }

      @Test
      void shouldThrowDefinitionExceptionWhenContainingBadProducer() {
        Discoverer discoverer = factory.create(store, List.of(Bad_E.class));

        assertThatThrownBy(discoverer::discover)
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("Method [void org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_E.bla()] has unsuitable type")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BadQualifiedTypeException.class)
          .hasMessage("[java.lang.Void] cannot be void or Void")
          .hasNoCause();

        assertThat(discoverer.getProblems()).isEmpty();
      }

      @Test
      void shouldFindAsManyTypesAsPossibleAndReturnProblems() throws DefinitionException {
        Discoverer discoverer = factory.create(store, List.of(Bad_A.class));

        assertThat(discoverer.discover()).containsExactlyInAnyOrder(
          classInjectableFactory.create(Bad_A.class)
        );

        assertThat(discoverer.getProblems()).containsExactlyInAnyOrder(
          "[org.int4.dirk.core.DefaultDiscovererFactoryTest$C] required by [org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_A], via Field [org.int4.dirk.core.DefaultDiscovererFactoryTest$C org.int4.dirk.core.DefaultDiscovererFactoryTest$Bad_A.c], "
          + "is not registered and cannot be discovered (reason: [class org.int4.dirk.core.DefaultDiscovererFactoryTest$C] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)"
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

        Discoverer discoverer = factory.create(store, List.of(X.class, Y.class, Z.class));

        assertThatThrownBy(() -> discoverer.discover())
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("[class org.int4.dirk.core.DefaultDiscovererFactoryTest$X] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
          .satisfies(throwable -> {
            assertThat(throwable.getSuppressed()[0])
              .isExactlyInstanceOf(DefinitionException.class)
              .hasMessage("[class org.int4.dirk.core.DefaultDiscovererFactoryTest$Z] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
              .hasNoSuppressedExceptions()
              .hasNoCause();
          })
          .hasNoCause();

        assertThat(discoverer.getProblems()).isEmpty();

        Discoverer discoverer2 = factory.create(store, List.of(X.class, Y.class));

        assertThatThrownBy(() -> discoverer2.discover())
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("[class org.int4.dirk.core.DefaultDiscovererFactoryTest$X] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
          .hasNoSuppressedExceptions()
          .hasNoCause();

        assertThat(discoverer2.getProblems()).isEmpty();
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

  public static class L {
    @Inject Provider<G> g;
  }

  public static class P {
    @Produces static P p = new P();
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
   * Bad because it has a producer that is not legal
   */
  public static class Bad_E {
    @Produces void bla() {
    }
  }

  /**
   * Bad because it has a producer that is not legal
   */
  public static class Bad_F {
    @Inject Bad_E badE;
  }

  interface I {
    void createJ();
  }

  public static class J {
  }

  interface M {  // abstract type, but produces itself...
    @Produces static M m = new M() {};
  }
}
