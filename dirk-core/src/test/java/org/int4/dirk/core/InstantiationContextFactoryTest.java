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
import org.int4.dirk.spi.instantiation.InstantiationContext;
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

public class InstantiationContextFactoryTest {
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
  private final InstantiationContextFactory factory = injectableFactories.getInstantiationContextFactory();

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
      private InstantiationContext<A> context = factory.createContext(store, new Key(A.class), false);

      @Nested
      class ThenContext {
        @Test
        void createShouldBeAmbiguous() {
          assertThatThrownBy(() -> context.get())
            .isExactlyInstanceOf(AmbiguousResolutionException.class);
        }

        @Test
        void createAllShouldReturnAllInScopeInstances() {
          assertThat(context.getAll()).hasSize(5);
        }

        @Test
        void selectShouldCreateSubcontexts() {
          InstantiationContext<B> bs = context.select(B.class);

          assertThatThrownBy(() -> bs.get()).isExactlyInstanceOf(AmbiguousResolutionException.class);
          assertThat(bs.getAll()).hasSize(2);

          InstantiationContext<C> cs = context.select(new TypeLiteral<C>() {});

          assertThat(cs.get()).isInstanceOf(C.class);
          assertThat(cs.getAll()).hasSize(1);

          InstantiationContext<A> reds = context.select(Annotations.of(Red.class));

          assertThatThrownBy(() -> reds.get()).isExactlyInstanceOf(AmbiguousResolutionException.class);
          assertThat(reds.getAll()).hasSize(2);

          InstantiationContext<A> greens = context.select(Annotations.of(Green.class));

          assertThatThrownBy(() -> greens.get()).isExactlyInstanceOf(AmbiguousResolutionException.class);  // Ambiguous as there could be a second one in scope sometimes
          assertThat(greens.getAll()).hasSize(1);  // Only 1 since other is not in scope
        }

        @Test
        void selectShouldRejectNonQualifiedAnnotations() {
          assertThatThrownBy(() -> context.select(B.class, Annotations.of(Singleton.class)))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("@jakarta.inject.Singleton() is not a qualifier annotation");
        }
      }
    }

    @Nested
    class AndCreatingContextForScopedClassF {
      private InstantiationContext<F> context = factory.createContext(store, new Key(F.class), false);

      @Test
      void createShouldThrowScopeException() {
        assertThatThrownBy(() -> context.get())
          .isExactlyInstanceOf(ScopeNotActiveException.class);
      }

      @Test
      void createAllShouldReturnNoInstances() {
        assertThat(context.getAll()).isEmpty();
      }

      @Nested
      class AndTestScopeIsActive {
        {
          currentScope = "active";
        }

        @Test
        void createShouldBeSatisfied() {
          assertThat(context.get()).isInstanceOf(F.class);
        }

        @Test
        void createAllShouldReturnOneInstance() {
          assertThat(context.getAll()).hasSize(1);
        }
      }
    }

    @Nested
    class AndCreatingContextForClassE {
      private InstantiationContext<E> context = factory.createContext(store, new Key(E.class), false);

      @Test
      void createShouldBeSatisfied() {
        assertThat(context.get()).isInstanceOf(E.class);
      }

      @Test
      void createShouldReturnUpdatedInstance() {
        E e = context.get();

        assertThat(e.test).isEqualTo("default");

        store.putAll(List.of(injectableFactories.forInstance().create("set")));

        E e2 = context.get();

        assertThat(e).isNotEqualTo(e2);
        assertThat(e2.test).isEqualTo("set");
      }
    }

    @Nested
    class AndCreatingContextForDependent {
      private InstantiationContext<E> context = factory.createContext(store, new Key(E.class), false);

      @Test
      void createShouldCallLifecycleMethods() {
        E.postConstructs = 0;
        G.postConstructs = 0;

        assertThat(context.get()).isInstanceOf(E.class);
        assertThat(E.postConstructs).isEqualTo(1);
        assertThat(G.postConstructs).isEqualTo(1);
      }

      @Test
      void destroyShouldCallLifecycleMethods() {
        E.preDestroys = 0;
        G.preDestroys = 0;

        E instance = context.get();

        context.destroy(instance);

        assertThat(E.preDestroys).isEqualTo(1);
        assertThat(G.preDestroys).isEqualTo(1);
      }
    }

    @Nested
    class AndCreatingContextForSingleton {
      private InstantiationContext<X> context = factory.createContext(store, new Key(X.class), false);

      @Test
      void createShouldBeSatisfied() {
        assertThat(context.get()).isInstanceOf(X.class);
      }

      @Test
      void createShouldCallLifecycleMethods() {
        X.postConstructs = 0;

        assertThat(context.get()).isInstanceOf(X.class);
        assertThat(X.postConstructs).isEqualTo(1);
      }

      @Test
      void destroyShouldNotCallLifecycleMethods() {
        X.preDestroys = 0;

        X instance = context.get();

        context.destroy(instance);

        assertThat(X.preDestroys).isEqualTo(0);  // none expected, it is a singleton
      }
    }

    @Nested
    class WithOptionalStringContext {
      private InstantiationContext<String> context = factory.createContext(store, new Key(String.class), true);

      @Test
      void createShouldBeSatisfied() {
        assertThat(context.get()).isNull();
      }

      @Test
      void createShouldReturnUpdatedInstance() {
        String initial = context.get();

        assertThat(initial).isNull();

        store.putAll(List.of(injectableFactories.forInstance().create("set")));

        String afterRegistration = context.get();

        assertThat(afterRegistration).isEqualTo("set");
      }
    }

    @Nested
    class AndCreatingContextForClassListB {
      private InstantiationContext<List<B>> context = factory.createContext(store, new Key(Types.parameterize(List.class, B.class)), false);

      @Nested
      class ThenContext {
        @Test
        void createShouldReturnListWithInScopeInstances() {
          assertThat(context.get()).flatExtracting(Object::getClass).containsExactlyInAnyOrder(B.class, D.class);

          currentScope = "A";

          assertThat(context.get()).flatExtracting(Object::getClass).containsExactlyInAnyOrder(B.class, D.class, F.class);
        }

        @Test
        void createAllForExtendedTypesShouldReturnEmptyList() {
          assertThat(context.getAll()).isEmpty();
        }

        @Test
        void selectShouldCreateSubcontexts() {
          InstantiationContext<List<B>> reds = context.select(Annotations.of(Red.class));

          assertThat(reds.get()).flatExtracting(Object::getClass).containsExactlyInAnyOrder(D.class);
          assertThat(reds.getAll()).isEmpty();
        }
      }
    }

    @Nested
    class AndCreatingContextsForBadSupplierInjectionTargetExtensions {
      private InstantiationContext<BadSupplierA<X>> context = factory.createContext(store, new Key(Types.parameterize(BadSupplierA.class, X.class)), false);

      @Nested
      class ThenContext {
        @Test
        void createAllShouldDiscoverBadInjectionTargetExtensionCallingCreate() {
          assertThat(context.getAll()).isEmpty();
        }

        @Test
        void createAllShouldNotDiscoverBadInjectionTargetExtensionAsExtensionsAreNotSupported() {
          assertThat(context.getAll()).isEmpty();
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
