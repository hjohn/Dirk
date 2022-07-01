package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.util.List;

import org.int4.dirk.annotations.Opt;
import org.int4.dirk.api.TypeLiteral;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.DefaultInstanceResolverTest.TestScoped;
import org.int4.dirk.core.instantiation.InjectionTargetExtensions;
import org.int4.dirk.core.store.InjectableStore;
import org.int4.dirk.core.test.qualifiers.Green;
import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.core.util.Key;
import org.int4.dirk.spi.instantiation.Instance;
import org.int4.dirk.spi.scope.AbstractScopeResolver;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

public class InstanceFactoryTest {
  private final AbstractScopeResolver<String> scopeResolver = new AbstractScopeResolver<>() {
    @Override
    public Annotation getAnnotation() {
      return Annotations.of(TestScoped.class);
    }

    @Override
    public String getCurrentScope() {
      return currentScope;
    }
  };

  interface BadSupplierA<T> {
    T get();
  }

  interface BadSupplierB<T> {
    T get();
  }

  private final ScopeResolverManager scopeResolverManager = ScopeResolverManagers.create(scopeResolver);
  private final InjectableFactories injectableFactories = new InjectableFactories(scopeResolverManager, InjectionTargetExtensions.create());
  private final InjectableStore store = new InjectableStore(InjectableFactories.PROXY_STRATEGY);
  private final InstanceFactory factory = injectableFactories.getInstanceFactory();

  private String currentScope;

  @Nested
  class GivenAllCandidatesInStore {
    @BeforeEach
    void beforeEach() {
      store.putAll(List.of(
        injectableFactories.forClass().create(A.class),
        injectableFactories.forClass().create(B.class),
        injectableFactories.forClass().create(C.class),
        injectableFactories.forClass().create(D.class),
        injectableFactories.forClass().create(E.class),
        injectableFactories.forClass().create(F.class), // not in scope
        injectableFactories.forClass().create(G.class),
        injectableFactories.forClass().create(X.class)
      ));
    }

    @Nested
    class AndCreatingContextForClassA {
      private Instance<A> instance = factory.createInstance(store, new Key(A.class), false);

      @Nested
      class ThenContext {
        @Test
        void createShouldBeAmbiguous() {
          assertThatThrownBy(() -> instance.get())
            .isExactlyInstanceOf(AmbiguousResolutionException.class);
        }

        @Test
        void createAllShouldReturnAllInScopeInstances() {
          assertThat(instance.getAll()).hasSize(5);
        }

        @Test
        void selectShouldCreateSubcontexts() {
          Instance<B> bs = instance.select(B.class);

          assertThatThrownBy(() -> bs.get()).isExactlyInstanceOf(AmbiguousResolutionException.class);
          assertThat(bs.getAll()).hasSize(2);

          Instance<C> cs = instance.select(new TypeLiteral<C>() {});

          assertThat(cs.get()).isInstanceOf(C.class);
          assertThat(cs.getAll()).hasSize(1);

          Instance<A> reds = instance.select(Annotations.of(Red.class));

          assertThatThrownBy(() -> reds.get()).isExactlyInstanceOf(AmbiguousResolutionException.class);
          assertThat(reds.getAll()).hasSize(2);

          Instance<A> greens = instance.select(Annotations.of(Green.class));

          assertThatThrownBy(() -> greens.get()).isExactlyInstanceOf(AmbiguousResolutionException.class);  // Ambiguous as there could be a second one in scope sometimes
          assertThat(greens.getAll()).hasSize(1);  // Only 1 since other is not in scope
        }

        @Test
        void selectShouldRejectNonQualifiedAnnotations() {
          assertThatThrownBy(() -> instance.select(B.class, Annotations.of(Singleton.class)))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("@jakarta.inject.Singleton() is not a qualifier annotation");
        }
      }
    }

    @Nested
    class AndCreatingContextForScopedClassF {
      private Instance<F> instance = factory.createInstance(store, new Key(F.class), false);

      @Test
      void createShouldThrowScopeException() {
        assertThatThrownBy(() -> instance.get())
          .isExactlyInstanceOf(ScopeNotActiveException.class);
      }

      @Test
      void createAllShouldReturnNoInstances() {
        assertThat(instance.getAll()).isEmpty();
      }

      @Nested
      class AndTestScopeIsActive {
        {
          currentScope = "active";
        }

        @Test
        void createShouldBeSatisfied() {
          assertThat(instance.get()).isInstanceOf(F.class);
        }

        @Test
        void createAllShouldReturnOneInstance() {
          assertThat(instance.getAll()).hasSize(1);
        }
      }
    }

    @Nested
    class AndCreatingContextForClassE {
      private Instance<E> instance = factory.createInstance(store, new Key(E.class), false);

      @Test
      void createShouldBeSatisfied() {
        assertThat(instance.get()).isInstanceOf(E.class);
      }

      @Test
      void createShouldReturnUpdatedInstance() {
        E e = instance.get();

        assertThat(e.test).isEqualTo("default");

        store.putAll(List.of(injectableFactories.forInstance().create("set")));

        E e2 = instance.get();

        assertThat(e).isNotEqualTo(e2);
        assertThat(e2.test).isEqualTo("set");
      }
    }

    @Nested
    class AndCreatingContextForDependent {
      private Instance<E> instance = factory.createInstance(store, new Key(E.class), false);

      @Test
      void createShouldCallLifecycleMethods() {
        E.postConstructs = 0;
        G.postConstructs = 0;

        assertThat(instance.get()).isInstanceOf(E.class);
        assertThat(E.postConstructs).isEqualTo(1);
        assertThat(G.postConstructs).isEqualTo(1);
      }

      @Test
      void destroyShouldCallLifecycleMethods() {
        E.preDestroys = 0;
        G.preDestroys = 0;

        E e = instance.get();

        instance.destroy(e);

        assertThat(E.preDestroys).isEqualTo(1);
        assertThat(G.preDestroys).isEqualTo(1);
      }
    }

    @Nested
    class AndCreatingContextForSingleton {
      private Instance<X> instance = factory.createInstance(store, new Key(X.class), false);

      @Test
      void createShouldBeSatisfied() {
        assertThat(instance.get()).isInstanceOf(X.class);
      }

      @Test
      void createShouldCallLifecycleMethods() {
        X.postConstructs = 0;

        assertThat(instance.get()).isInstanceOf(X.class);
        assertThat(X.postConstructs).isEqualTo(1);
      }

      @Test
      void destroyShouldNotCallLifecycleMethods() {
        X.preDestroys = 0;

        X x = instance.get();

        instance.destroy(x);

        assertThat(X.preDestroys).isEqualTo(0);  // none expected, it is a singleton
      }
    }

    @Nested
    class WithOptionalStringContext {
      private Instance<String> instance = factory.createInstance(store, new Key(String.class), true);

      @Test
      void createShouldBeSatisfied() {
        assertThat(instance.get()).isNull();
      }

      @Test
      void createShouldReturnUpdatedInstance() {
        String initial = instance.get();

        assertThat(initial).isNull();

        store.putAll(List.of(injectableFactories.forInstance().create("set")));

        String afterRegistration = instance.get();

        assertThat(afterRegistration).isEqualTo("set");
      }
    }

    @Nested
    class AndCreatingContextForClassListB {
      private Instance<List<B>> instance = factory.createInstance(store, new Key(Types.parameterize(List.class, B.class)), false);

      @Nested
      class ThenContext {
        @Test
        void createShouldReturnListWithInScopeInstances() {
          assertThat(instance.get()).flatExtracting(Object::getClass).containsExactlyInAnyOrder(B.class, D.class);

          currentScope = "A";

          assertThat(instance.get()).flatExtracting(Object::getClass).containsExactlyInAnyOrder(B.class, D.class, F.class);
        }

        @Test
        void createAllForExtendedTypesShouldReturnEmptyList() {
          assertThat(instance.getAll()).isEmpty();
        }

        @Test
        void selectShouldCreateSubcontexts() {
          Instance<List<B>> reds = instance.select(Annotations.of(Red.class));

          assertThat(reds.get()).flatExtracting(Object::getClass).containsExactlyInAnyOrder(D.class);
          assertThat(reds.getAll()).isEmpty();
        }
      }
    }

    @Nested
    class AndCreatingContextsForBadSupplierInjectionTargetExtensions {
      private Instance<BadSupplierA<X>> instance = factory.createInstance(store, new Key(Types.parameterize(BadSupplierA.class, X.class)), false);

      @Nested
      class ThenContext {
        @Test
        void createAllShouldDiscoverBadInjectionTargetExtensionCallingCreate() {
          assertThat(instance.getAll()).isEmpty();
        }

        @Test
        void createAllShouldNotDiscoverBadInjectionTargetExtensionAsExtensionsAreNotSupported() {
          assertThat(instance.getAll()).isEmpty();
        }
      }
    }
  }

  public static class A {
    @Inject X x;
  }

  public static class B extends A {
  }

  @Red
  public static class C extends A {
  }

  @Red
  public static class D extends B {
  }

  @Green
  public static class E extends A {
    static int postConstructs;
    static int preDestroys;

    @Inject @Opt String test = "default";
    @Inject G g;

    @PostConstruct
    void postConstruct() {
      postConstructs++;
    }

    @PreDestroy
    void preDestroy() {
      preDestroys++;
    }
  }

  @Green
  @TestScoped
  public static class F extends B {
  }

  public static class G {
    static int postConstructs;
    static int preDestroys;

    @PostConstruct
    void postConstruct() {
      postConstructs++;
    }

    @PreDestroy
    void preDestroy() {
      preDestroys++;
    }
  }

  @Singleton
  public static class X {
    static int postConstructs;
    static int preDestroys;

    @PostConstruct
    void postConstruct() {
      postConstructs++;
    }

    @PreDestroy
    void preDestroy() {
      preDestroys++;
    }
  }
}
