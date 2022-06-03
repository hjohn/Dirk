package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.util.List;

import org.int4.dirk.annotations.Opt;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.DefaultInstanceResolverTest.TestScoped;
import org.int4.dirk.core.definition.InjectionTargetExtensionStore;
import org.int4.dirk.core.definition.Key;
import org.int4.dirk.core.store.InjectableStore;
import org.int4.dirk.core.test.qualifiers.Green;
import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.spi.instantiation.InstantiationContext;
import org.int4.dirk.spi.scope.AbstractScopeResolver;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

  private final ScopeResolverManager scopeResolverManager = ScopeResolverManagers.create(scopeResolver);
  private final InjectableFactories injectableFactories = new InjectableFactories(scopeResolverManager);
  private final InjectionTargetExtensionStore injectionTargetExtensionStore = injectableFactories.getInjectionTargetExtensionStore();
  private final InjectableStore store = new InjectableStore(InjectableFactories.PROXY_STRATEGY);
  private final InstantiationContextFactory factory = new InstantiationContextFactory(store, InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.PROXY_STRATEGY, injectionTargetExtensionStore);

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
        injectableFactories.forClass().create(X.class)  // not in scope
      ));
    }

    @Nested
    class AndCreatingContextForClassA {
      private InstantiationContext<A> context = factory.createContext(new Key(A.class), false);

      @Nested
      class ThenContext {
        @Test
        void createShouldBeAmbiguous() {
          assertThatThrownBy(() -> context.create())
            .isExactlyInstanceOf(AmbiguousResolutionException.class);
        }

        @Test
        void createAllShouldReturnAllInScopeInstances() {
          assertThat(context.createAll()).hasSize(5);
        }

        @Test
        void selectShouldCreateSubcontexts() {
          InstantiationContext<B> bs = context.select(B.class);

          assertThatThrownBy(() -> bs.create()).isExactlyInstanceOf(AmbiguousResolutionException.class);
          assertThat(bs.createAll()).hasSize(2);

          InstantiationContext<C> cs = context.select(C.class);

          assertThat(cs.create()).isInstanceOf(C.class);
          assertThat(cs.createAll()).hasSize(1);

          InstantiationContext<A> reds = context.select(Annotations.of(Red.class));

          assertThatThrownBy(() -> reds.create()).isExactlyInstanceOf(AmbiguousResolutionException.class);
          assertThat(reds.createAll()).hasSize(2);

          InstantiationContext<A> greens = context.select(Annotations.of(Green.class));

          assertThatThrownBy(() -> greens.create()).isExactlyInstanceOf(AmbiguousResolutionException.class);  // Ambiguous as there could be a second one in scope sometimes
          assertThat(greens.createAll()).hasSize(1);  // Only 1 since other is not in scope
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
      private InstantiationContext<F> context = factory.createContext(new Key(F.class), false);

      @Test
      void createShouldThrowScopeException() {
        assertThatThrownBy(() -> context.create())
          .isExactlyInstanceOf(ScopeNotActiveException.class);
      }

      @Test
      void createAllShouldReturnNoInstances() {
        assertThat(context.createAll()).isEmpty();
      }

      @Nested
      class AndTestScopeIsActive {
        {
          currentScope = "active";
        }

        @Test
        void createShouldBeSatisfied() {
          assertThat(context.create()).isInstanceOf(F.class);


        }

        @Test
        void createAllShouldReturnOneInstance() {
          assertThat(context.createAll()).hasSize(1);
        }
      }
    }

    @Nested
    class AndCreatingContextForClassE {
      private InstantiationContext<E> context = factory.createContext(new Key(E.class), false);

      @Test
      void createShouldBeSatisfied() {
        assertThat(context.create()).isInstanceOf(E.class);
      }

      @Test
      void createShouldReturnUpdatedInstance() {
        E e = context.create();

        assertThat(e.test).isEqualTo("default");

        store.putAll(List.of(injectableFactories.forInstance().create("set")));

        E e2 = context.create();

        assertThat(e).isNotEqualTo(e2);
        assertThat(e2.test).isEqualTo("set");
      }
    }

    @Nested
    class WithOptionalStringContext {
      private InstantiationContext<String> context = factory.createContext(new Key(String.class), true);

      @Test
      void createShouldBeSatisfied() {
        assertThat(context.create()).isNull();
      }

      @Test
      void createShouldReturnUpdatedInstance() {
        String initial = context.create();

        assertThat(initial).isNull();

        store.putAll(List.of(injectableFactories.forInstance().create("set")));

        String afterRegistration = context.create();

        assertThat(afterRegistration).isEqualTo("set");
      }
    }

    @Nested
    class AndCreatingContextForClassListB {
      private InstantiationContext<List<B>> context = factory.createContext(new Key(Types.parameterize(List.class, B.class)), false);

      @Nested
      class ThenContext {
        @Test
        void createShouldReturnListWithInScopeInstances() {
          assertThat(context.create()).flatExtracting(Object::getClass).containsExactlyInAnyOrder(B.class, D.class);

          currentScope = "A";

          assertThat(context.create()).flatExtracting(Object::getClass).containsExactlyInAnyOrder(B.class, D.class, F.class);
        }

        @Test
        void createAllForExtendedTypesShouldReturnEmptyList() {
          assertThat(context.createAll()).isEmpty();
        }

        @Test
        void selectShouldCreateSubcontexts() {
          InstantiationContext<List<B>> reds = context.select(Annotations.of(Red.class));

          assertThat(reds.create()).flatExtracting(Object::getClass).containsExactlyInAnyOrder(D.class);
          assertThat(reds.createAll()).isEmpty();
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
    @Inject @Opt String test = "default";
  }

  @Green
  @TestScoped
  public static class F extends B {
  }

  public static class X {
  }
}