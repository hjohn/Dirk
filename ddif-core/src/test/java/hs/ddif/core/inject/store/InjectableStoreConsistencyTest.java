package hs.ddif.core.inject.store;

import hs.ddif.annotations.Opt;
import hs.ddif.api.instantiation.InstantiatorFactory;
import hs.ddif.api.scope.UnknownScopeException;
import hs.ddif.core.InjectableFactories;
import hs.ddif.core.InstantiatorFactories;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.instantiation.TypeExtensions;
import hs.ddif.core.test.scope.TestScope;
import hs.ddif.core.util.Nullable;
import hs.ddif.test.util.ReplaceCamelCaseDisplayNameGenerator;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

@DisplayNameGeneration(ReplaceCamelCaseDisplayNameGenerator.class)
public class InjectableStoreConsistencyTest {
  private final ClassInjectableFactory classInjectableFactory = new InjectableFactories().forClass();

  private Injectable<A> a = classInjectableFactory.create(A.class);
  private Injectable<B> b = classInjectableFactory.create(B.class);
  private Injectable<C> c = classInjectableFactory.create(C.class);
  private Injectable<D> d = classInjectableFactory.create(D.class);
  private Injectable<E> e = classInjectableFactory.create(E.class);
  private Injectable<F> f = classInjectableFactory.create(F.class);
  private Injectable<G> g = classInjectableFactory.create(G.class);
  private Injectable<H> h = classInjectableFactory.create(H.class);
  private Injectable<I> i = classInjectableFactory.create(I.class);
  private Injectable<J> j = classInjectableFactory.create(J.class);
  private Injectable<L> l = classInjectableFactory.create(L.class);
  private Injectable<M> m = classInjectableFactory.create(M.class);
  private Injectable<N> n = classInjectableFactory.create(N.class);
  private Injectable<O> o = classInjectableFactory.create(O.class);

  private InstantiatorFactory instantiatorFactory = InstantiatorFactories.create(InjectableFactories.ANNOTATION_STRATEGY, TypeExtensions.create(InjectableFactories.ANNOTATION_STRATEGY));
  private InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
  private InjectableStore store = new InjectableStore(instantiatorBindingMap);

  @Test
  void shouldThrowExceptionWhenClassInjectableAddedWithUnknownScope() {
    assertThatThrownBy(() -> store.putAll(Set.of(classInjectableFactory.create(K.class))))
      .isExactlyInstanceOf(UnknownScopeException.class)
      .hasNoCause();
  }

  @Test
  void addAllShouldRejectInjectablesWithCyclicDependency() {
    CyclicDependencyException ex = assertThrows(CyclicDependencyException.class, () -> store.putAll(List.of(e, b, c, d)));

    assertEquals(4, ex.getCycle().size());
  }

  @Test
  void addBShouldFailAsItHasUnresolvableDependency() {
    assertThrows(UnresolvableDependencyException.class, () -> store.putAll(List.of(b)));
  }

  @Test
  void addAAndBAndHShouldFailsAsThereAreMultipleCandidatesOfZForB() {
    assertThrows(UnresolvableDependencyException.class, () -> store.putAll(List.of(a, b, h)));
  }

  @Test
  void addAAndBAndHShouldFailsAsItThereAreMultipleCandidatesOfZForB2() {
    assertThrows(UnresolvableDependencyException.class, () -> store.putAll(List.of(b, a, h)));
  }
  @Test
  void addAAndBAndHShouldFailsAsItThereAreMultipleCandidatesOfZForB3() {
    assertThrows(UnresolvableDependencyException.class, () -> store.putAll(List.of(a, h, b)));
  }

  @Test
  void addIShouldFail() {
    assertThrows(CyclicDependencyException.class, () -> store.putAll(List.of(i)));
  }

  @Test
  void addJShouldFail() {
    assertThrows(CyclicDependencyException.class, () -> store.putAll(List.of(j)));
  }

  @Test
  void addLShouldWork() {
    store.putAll(List.of(l));
  }

  @Test
  void addLAndAShouldWork() {
    store.putAll(List.of(l, a));
  }

  @Nested
  class WhenClasses_A_And_H_AreAdded {
    {
      store.putAll(List.of(a, h));
    }

    @Test
    void add_L_ShouldFailBecause_Z_IsProvidedTwice() {
      assertThrows(UnresolvableDependencyException.class, () -> store.putAll(List.of(l)));
    }
  }

  @Nested
  class When_L_IsAdded {
    {
      store.putAll(List.of(l));
    }

    @Test
    void remove_L_ShouldWork() {
      store.removeAll(List.of(l));
    }

    @Test
    void add_A_ShouldWork() {
      store.putAll(List.of(a));
    }

    @Test
    void add_A_and_H_ShouldFailBecause_Z_IsProvidedTwice() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.putAll(List.of(a, h)));
    }
  }

  @Nested
  class WhenClassesAAndBAndCAndDAreAdded {
    {
      store.putAll(List.of(a, b, c, d));
    }

    @Test
    void removeAllShouldWork() {
      store.removeAll(List.of(a, b, c, d));
    }

    @Test
    void removeAShouldFail() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.removeAll(List.of(a)));
    }

    @Test
    void removeBShouldFail() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.removeAll(List.of(b)));
    }

    @Test
    void removeCShouldFail() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.removeAll(List.of(c)));
    }

    @Test
    void removeAAndCAndDShouldFail() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.removeAll(List.of(c, a, d)));
    }

    @Test
    void removeDShouldWork() {
      store.removeAll(List.of(d));
    }

    @Test
    void removeCAndDShouldWork() {
      store.removeAll(List.of(c, d));
    }

    @Test
    void addEShouldFailAsEWouldCreateCyclicDependency() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.putAll(List.of(e)));
    }

    @Test
    void addGAndFAndEShouldFailAsEWouldCreateCyclicDependency() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.putAll(List.of(g, f, e)));
    }

    @Test
    void addFShouldWork() {
      store.putAll(List.of(f));
    }

    @Test
    void addGAndFShouldWork() {
      store.putAll(List.of(g, f));
    }

    @Test
    void addHShouldFailAsZWouldBeProvidedTwice() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.putAll(List.of(h)));
    }

    @Test
    void addGAndFAndHShouldFailAsZWouldBeProvidedTwice() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.putAll(List.of(g, h, f)));
    }

    @Test
    void addLShouldWork() {
      store.putAll(List.of(l));
    }
  }

  @Nested
  class WhenClassesMAndNAndOAreAdded {
    {
      store.putAll(List.of(m, n, o));
    }

    @Test
    void removeMShouldFailAsRequiredByN() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.removeAll(List.of(m)));
    }

    @Test
    void removeNShouldFailAsRequiredByM() {
      assertThrows(ViolatesSingularDependencyException.class, () -> store.removeAll(List.of(n)));
    }

    @Test
    void removeOShouldWorkAsMOnlyDependsOnItOptionally() {
      store.removeAll(List.of(o));
    }
  }

  interface Z {
  }

  public static class A implements Z {
  }

  public static class E extends A {
    @Inject D d;
  }

  public static class B {
    @Inject Z z;
  }

  public static class C {
    @Inject B b;
    @Inject @Opt Provider<D> d;  // not a circular dependency, and not required
  }

  public static class D {
    @Inject C c;
  }

  public static class F {
    @Inject B b;
    @Inject C c;
  }

  public static class G {
    @Inject F f;
  }

  public static class H implements Z {
  }

  public static class I {
    @Inject I i;
  }

  public static class J implements Z {
    @Inject Z z;
  }

  @TestScope
  public static class K {
  }

  public static class L {
    @Inject @Nullable Z z;
  }

  public static class M {
    @Inject Provider<N> n;
    @Inject @Opt Provider<O> o;
  }

  public static class N {
    @Inject M m;
  }

  public static class O {
    @Inject M m;
  }
}
