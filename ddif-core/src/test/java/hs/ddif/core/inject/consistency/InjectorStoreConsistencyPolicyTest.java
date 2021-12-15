package hs.ddif.core.inject.consistency;

import hs.ddif.core.TestScope;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.ReplaceCamelCaseDisplayNameGenerator;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayNameGeneration(ReplaceCamelCaseDisplayNameGenerator.class)
public class InjectorStoreConsistencyPolicyTest {
  private final ClassInjectableFactory classInjectableFactory = new ClassInjectableFactory(ResolvableInjectable::new);

  private InjectorStoreConsistencyPolicy<ResolvableInjectable> policy = new InjectorStoreConsistencyPolicy<>();

  private ResolvableInjectable a = classInjectableFactory.create(A.class);
  private ResolvableInjectable b = classInjectableFactory.create(B.class);
  private ResolvableInjectable c = classInjectableFactory.create(C.class);
  private ResolvableInjectable d = classInjectableFactory.create(D.class);
  private ResolvableInjectable e = classInjectableFactory.create(E.class);
  private ResolvableInjectable f = classInjectableFactory.create(F.class);
  private ResolvableInjectable g = classInjectableFactory.create(G.class);
  private ResolvableInjectable h = classInjectableFactory.create(H.class);
  private ResolvableInjectable i = classInjectableFactory.create(I.class);
  private ResolvableInjectable j = classInjectableFactory.create(J.class);

  private InjectableStore<ResolvableInjectable> emptyStore = new InjectableStore<>();

  @Test
  void shouldThrowExceptionWhenClassInjectableAddedWithUnknownScope() {
    assertThatThrownBy(() -> policy.addAll(emptyStore, Set.of(classInjectableFactory.create(K.class))))
      .isExactlyInstanceOf(UnknownScopeException.class)
      .hasNoCause();
  }

  @Test
  void addAllShouldRejectInjectablesWithCyclicDependency() {
    emptyStore.putAll(List.of(e, b, c, d));

    CyclicDependencyException ex = assertThrows(CyclicDependencyException.class, () -> policy.addAll(emptyStore, List.of(e, b, c, d)));

    assertEquals(4, ex.getCycle().size());
  }

  @Test
  void addBShouldFailAsItHasUnresolvableDependency() {
    emptyStore.put(b);

    assertThrows(UnresolvableDependencyException.class, () -> policy.addAll(emptyStore, List.of(b)));
  }

  @Test
  void addAAndBAndHShouldFailsAsItThereAreMultipleCandidatesOfZForB() {
    emptyStore.putAll(List.of(a, b, h));

    assertThrows(UnresolvableDependencyException.class, () -> policy.addAll(emptyStore, List.of(a, b, h)));
  }

  @Test
  void addAAndBAndHShouldFailsAsItThereAreMultipleCandidatesOfZForB2() {
    emptyStore.putAll(List.of(b, a, h));

    assertThrows(UnresolvableDependencyException.class, () -> policy.addAll(emptyStore, List.of(b, a, h)));
  }
  @Test
  void addAAndBAndHShouldFailsAsItThereAreMultipleCandidatesOfZForB3() {
    emptyStore.putAll(List.of(a, h, b));

    assertThrows(UnresolvableDependencyException.class, () -> policy.addAll(emptyStore, List.of(a, h, b)));
  }

  @Test
  void addIShouldFail() {
    emptyStore.put(i);

    assertThrows(CyclicDependencyException.class, () -> policy.addAll(emptyStore, List.of(i)));
  }

  @Test
  void addJShouldFail() {
    emptyStore.put(j);

    assertThrows(CyclicDependencyException.class, () -> policy.addAll(emptyStore, List.of(j)));
  }

  @Nested
  class WhenClassesAdded {
    InjectableStore<ResolvableInjectable> store = new InjectableStore<>();

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
      store.put(e);
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.addAll(store, List.of(e)));
    }

    @Test
    void addGAndFAndEShouldFailAsEWouldCreateCyclicDependency() {
      store.putAll(List.of(g, f, e));
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.addAll(store, List.of(g, f, e)));
    }

    @Test
    void addFShouldWork() {
      store.put(f);
      policy.addAll(store, List.of(f));
    }

    @Test
    void addGAndFShouldWork() {
      store.putAll(List.of(g, f));
      policy.addAll(store, List.of(g, f));
    }

    @Test
    void addHShouldFailAsZWouldBeProvidedTwice() {
      store.put(h);
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.addAll(store, List.of(h)));
    }

    @Test
    void addGAndFAndHShouldFailAsZWouldBeProvidedTwice() {
      store.putAll(List.of(g, h, f));
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

  public static class I {
    @Inject I i;
  }

  public static class J implements Z {
    @Inject Z z;
  }

  @TestScope
  public static class K {
  }
}
