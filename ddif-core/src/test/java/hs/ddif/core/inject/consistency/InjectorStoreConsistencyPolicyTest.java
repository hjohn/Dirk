package hs.ddif.core.inject.consistency;

import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.ReplaceCamelCaseDisplayNameGenerator;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayNameGeneration(ReplaceCamelCaseDisplayNameGenerator.class)
class InjectorStoreConsistencyPolicyTest {
  InjectorStoreConsistencyPolicy<ScopedInjectable> policy = new InjectorStoreConsistencyPolicy<>();

  ClassInjectable a = new ClassInjectable(A.class);
  ClassInjectable b = new ClassInjectable(B.class);
  ClassInjectable c = new ClassInjectable(C.class);
  ClassInjectable d = new ClassInjectable(D.class);
  ClassInjectable e = new ClassInjectable(E.class);
  ClassInjectable f = new ClassInjectable(F.class);
  ClassInjectable g = new ClassInjectable(G.class);
  ClassInjectable h = new ClassInjectable(H.class);

  InjectableStore<ScopedInjectable> emptyStore = new InjectableStore<>();

  @Test
  void addAllShouldRejectInjectablesWithCyclicDependency() {
    CyclicDependencyException ex = assertThrows(CyclicDependencyException.class, () -> policy.addAll(emptyStore, List.of(e, b, c, d)));

    assertEquals(4, ex.getCycle().size());
  }

  @Test
  void addBShouldFailAsItHasUnresolvableDependency() {
    assertThrows(UnresolvableDependencyException.class, () -> policy.addAll(emptyStore, List.of(b)));
  }

  @Nested
  class WhenClassesAdded {
    InjectableStore<ScopedInjectable> store = new InjectableStore<>();

    @BeforeEach
    void beforeEach() {
      store.putAll(List.of(a, b, c, d));
      policy.addAll(store, List.of(b, d, c, a));
    }

    @Test
    void removeAllShouldWork() {
      policy.removeAll(store, List.of(a, b, c, d));
    }

    @Test
    void removeAShouldFail() {
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.removeAll(store, List.of(a)));
    }

    @Test
    void removeBShouldFail() {
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.removeAll(store, List.of(b)));
    }

    @Test
    void removeCShouldFail() {
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.removeAll(store, List.of(c)));
    }

    @Test
    void removeAAndCAndDShouldFail() {
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.removeAll(store, List.of(c, a, d)));
    }

    @Test
    void removeDShouldWork() {
      policy.removeAll(store, List.of(d));
    }

    @Test
    void removeCAndDShouldWork() {
      policy.removeAll(store, List.of(c, d));
    }

    @Test
    void addEShouldFailAsEWouldCreateCyclicDependency() {
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.addAll(store, List.of(e)));
    }

    @Test
    void addGAndFAndEShouldFailAsEWouldCreateCyclicDependency() {
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.addAll(store, List.of(g, f, e)));
    }

    @Test
    void addFShouldWork() {
      policy.addAll(store, List.of(f));
    }

    @Test
    void addGAndFShouldWork() {
      policy.addAll(store, List.of(g, f));
    }

    @Test
    void addHShouldFailAsZWouldBeProvidedTwice() {
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.addAll(store, List.of(h)));
    }

    @Test
    void addGAndFAndHShouldFailAsZWouldBeProvidedTwice() {
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.addAll(store, List.of(g, h, f)));
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
    @Inject Provider<D> d;  // not a circular dependency
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
}
