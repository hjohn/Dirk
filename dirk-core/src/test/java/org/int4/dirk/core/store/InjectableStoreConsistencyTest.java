package org.int4.dirk.core.store;

import java.util.List;
import java.util.Set;

import org.int4.dirk.annotations.Opt;
import org.int4.dirk.api.definition.AmbiguousDependencyException;
import org.int4.dirk.api.definition.AmbiguousRequiredDependencyException;
import org.int4.dirk.api.definition.CyclicDependencyException;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.api.definition.DependencyException;
import org.int4.dirk.api.definition.UnsatisfiedDependencyException;
import org.int4.dirk.api.definition.UnsatisfiedRequiredDependencyException;
import org.int4.dirk.core.InjectableFactories;
import org.int4.dirk.core.definition.ClassInjectableFactory;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.test.scope.TestScope;
import org.int4.dirk.core.util.Nullable;
import org.int4.dirk.spi.scope.UnknownScopeException;
import org.int4.dirk.test.util.ReplaceCamelCaseDisplayNameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

@DisplayNameGeneration(ReplaceCamelCaseDisplayNameGenerator.class)
public class InjectableStoreConsistencyTest {
  private final ClassInjectableFactory classInjectableFactory = new InjectableFactories().forClass();

  private Injectable<A> a;
  private Injectable<B> b;
  private Injectable<C> c;
  private Injectable<D> d;
  private Injectable<E> e;
  private Injectable<F> f;
  private Injectable<G> g;
  private Injectable<H> h;
  private Injectable<I> i;
  private Injectable<J> j;
  private Injectable<L> l;
  private Injectable<M> m;
  private Injectable<N> n;
  private Injectable<O> o;

  private InjectableStore store = new InjectableStore(InjectableFactories.PROXY_STRATEGY);

  @BeforeEach
  void beforeEach() throws DefinitionException {
    a = classInjectableFactory.create(A.class);
    b = classInjectableFactory.create(B.class);
    c = classInjectableFactory.create(C.class);
    d = classInjectableFactory.create(D.class);
    e = classInjectableFactory.create(E.class);
    f = classInjectableFactory.create(F.class);
    g = classInjectableFactory.create(G.class);
    h = classInjectableFactory.create(H.class);
    i = classInjectableFactory.create(I.class);
    j = classInjectableFactory.create(J.class);
    l = classInjectableFactory.create(L.class);
    m = classInjectableFactory.create(M.class);
    n = classInjectableFactory.create(N.class);
    o = classInjectableFactory.create(O.class);
  }

  @Test
  void shouldThrowExceptionWhenClassInjectableAddedWithUnknownScope() {
    assertThatThrownBy(() -> store.putAll(Set.of(classInjectableFactory.create(K.class))))
      .isExactlyInstanceOf(UnknownScopeException.class)
      .hasNoCause();
  }

  @Test
  void addAllShouldRejectInjectablesWithCyclicDependency() {
    assertThrows(CyclicDependencyException.class, () -> store.putAll(List.of(e, b, c, d)));
  }

  @Test
  void addBShouldFailAsItHasUnresolvableDependency() {
    assertThrows(UnsatisfiedDependencyException.class, () -> store.putAll(List.of(b)));
  }

  @Test
  void addAAndBAndHShouldFailsAsThereAreMultipleCandidatesOfZForB() {
    assertThrows(AmbiguousDependencyException.class, () -> store.putAll(List.of(a, b, h)));
  }

  @Test
  void addAAndBAndHShouldFailsAsItThereAreMultipleCandidatesOfZForB2() {
    assertThrows(AmbiguousDependencyException.class, () -> store.putAll(List.of(b, a, h)));
  }
  @Test
  void addAAndBAndHShouldFailsAsItThereAreMultipleCandidatesOfZForB3() {
    assertThrows(AmbiguousDependencyException.class, () -> store.putAll(List.of(a, h, b)));
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
  void addLShouldWork() throws DependencyException {
    store.putAll(List.of(l));
  }

  @Test
  void addLAndAShouldWork() throws DependencyException {
    store.putAll(List.of(l, a));
  }

  @Nested
  class WhenClasses_A_And_H_AreAdded {
    @BeforeEach
    void beforeEach() throws DependencyException {
      store.putAll(List.of(a, h));
    }

    @Test
    void add_L_ShouldFailBecause_Z_IsProvidedTwice() {
      assertThrows(AmbiguousDependencyException.class, () -> store.putAll(List.of(l)));
    }
  }

  @Nested
  class When_L_IsAdded {
    @BeforeEach
    void beforeEach() throws DependencyException {
      store.putAll(List.of(l));
    }

    @Test
    void remove_L_ShouldWork() throws DependencyException {
      store.removeAll(List.of(l));
    }

    @Test
    void add_A_ShouldWork() throws DependencyException {
      store.putAll(List.of(a));
    }

    @Test
    void add_A_and_H_ShouldFailBecause_Z_IsProvidedTwice() {
      assertThrows(AmbiguousRequiredDependencyException.class, () -> store.putAll(List.of(a, h)));
    }
  }

  @Nested
  class WhenClassesAAndBAndCAndDAreAdded {
    @BeforeEach
    void beforeEach() throws DependencyException {
      store.putAll(List.of(a, b, c, d));
    }

    @Test
    void removeAllShouldWork() throws DependencyException {
      store.removeAll(List.of(a, b, c, d));
    }

    @Test
    void removeAShouldFail() {
      assertThrows(UnsatisfiedRequiredDependencyException.class, () -> store.removeAll(List.of(a)));
    }

    @Test
    void removeBShouldFail() {
      assertThrows(UnsatisfiedRequiredDependencyException.class, () -> store.removeAll(List.of(b)));
    }

    @Test
    void removeCShouldFail() {
      assertThrows(UnsatisfiedRequiredDependencyException.class, () -> store.removeAll(List.of(c)));
    }

    @Test
    void removeAAndCAndDShouldFail() {
      assertThrows(UnsatisfiedRequiredDependencyException.class, () -> store.removeAll(List.of(c, a, d)));
    }

    @Test
    void removeDShouldWork() throws DependencyException {
      store.removeAll(List.of(d));
    }

    @Test
    void removeCAndDShouldWork() throws DependencyException {
      store.removeAll(List.of(c, d));
    }

    @Test
    void addEShouldFailAsEWouldCreateCyclicDependency() {
      assertThrows(AmbiguousRequiredDependencyException.class, () -> store.putAll(List.of(e)));
    }

    @Test
    void addGAndFAndEShouldFailAsEWouldCreateCyclicDependency() {
      assertThrows(AmbiguousRequiredDependencyException.class, () -> store.putAll(List.of(g, f, e)));
    }

    @Test
    void addFShouldWork() throws DependencyException {
      store.putAll(List.of(f));
    }

    @Test
    void addGAndFShouldWork() throws DependencyException {
      store.putAll(List.of(g, f));
    }

    @Test
    void addHShouldFailAsZWouldBeProvidedTwice() {
      assertThrows(AmbiguousRequiredDependencyException.class, () -> store.putAll(List.of(h)));
    }

    @Test
    void addGAndFAndHShouldFailAsZWouldBeProvidedTwice() {
      assertThrows(AmbiguousRequiredDependencyException.class, () -> store.putAll(List.of(g, h, f)));
    }

    @Test
    void addLShouldWork() throws DependencyException {
      store.putAll(List.of(l));
    }
  }

  @Nested
  class WhenClassesMAndNAndOAreAdded {
    @BeforeEach
    void beforeEach() throws DependencyException {
      store.putAll(List.of(m, n, o));
    }

    @Test
    void removeMShouldFailAsRequiredByN() {
      assertThrows(UnsatisfiedRequiredDependencyException.class, () -> store.removeAll(List.of(m)));
    }

    @Test
    void removeNShouldWorkAsOnlyIndirectlyRequiredByM() {
      store.removeAll(List.of(n));
    }

    @Test
    void removeOShouldWorkAsMOnlyDependsOnItOptionally() throws DependencyException {
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
