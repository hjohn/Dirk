package hs.ddif.core.inject.consistency;

import hs.ddif.annotations.Opt;
import hs.ddif.core.TestScope;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.Nullable;
import hs.ddif.core.util.ReplaceCamelCaseDisplayNameGenerator;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

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
  private ResolvableInjectable l = classInjectableFactory.create(L.class);
  private ResolvableInjectable m = classInjectableFactory.create(M.class);
  private ResolvableInjectable n = classInjectableFactory.create(N.class);
  private ResolvableInjectable o = classInjectableFactory.create(O.class);

  private InjectableStore<ResolvableInjectable> store = new InjectableStore<>();

  @Test
  void shouldThrowExceptionWhenClassInjectableAddedWithUnknownScope() {
    assertThatThrownBy(() -> policy.addAll(store, Set.of(classInjectableFactory.create(K.class))))
      .isExactlyInstanceOf(UnknownScopeException.class)
      .hasNoCause();
  }

  @Test
  void addAllShouldRejectInjectablesWithCyclicDependency() {
    store.putAll(List.of(e, b, c, d));

    CyclicDependencyException ex = assertThrows(CyclicDependencyException.class, () -> policy.addAll(store, List.of(e, b, c, d)));

    assertEquals(4, ex.getCycle().size());
  }

  @Test
  void addBShouldFailAsItHasUnresolvableDependency() {
    store.put(b);

    assertThrows(UnresolvableDependencyException.class, () -> policy.addAll(store, List.of(b)));
  }

  @Test
  void addAAndBAndHShouldFailsAsThereAreMultipleCandidatesOfZForB() {
    store.putAll(List.of(a, b, h));

    assertThrows(UnresolvableDependencyException.class, () -> policy.addAll(store, List.of(a, b, h)));
  }

  @Test
  void addAAndBAndHShouldFailsAsItThereAreMultipleCandidatesOfZForB2() {
    store.putAll(List.of(b, a, h));

    assertThrows(UnresolvableDependencyException.class, () -> policy.addAll(store, List.of(b, a, h)));
  }
  @Test
  void addAAndBAndHShouldFailsAsItThereAreMultipleCandidatesOfZForB3() {
    store.putAll(List.of(a, h, b));

    assertThrows(UnresolvableDependencyException.class, () -> policy.addAll(store, List.of(a, h, b)));
  }

  @Test
  void addIShouldFail() {
    store.put(i);

    assertThrows(CyclicDependencyException.class, () -> policy.addAll(store, List.of(i)));
  }

  @Test
  void addJShouldFail() {
    store.put(j);

    assertThrows(CyclicDependencyException.class, () -> policy.addAll(store, List.of(j)));
  }

  @Test
  void addLShouldWork() {
    store.put(l);
    policy.addAll(store, List.of(l));
  }

  @Test
  void addLAndAShouldWork() {
    store.putAll(List.of(l, a));
    policy.addAll(store, List.of(l, a));
  }

  @Nested
  class WhenClasses_A_And_H_AreAdded {
    {
      store.putAll(List.of(a, h));
      policy.addAll(store, List.of(a, h));
    }

    @Test
    void add_L_ShouldFailBecause_Z_IsProvidedTwice() {
      store.putAll(List.of(l));
      assertThrows(UnresolvableDependencyException.class, () -> policy.addAll(store, List.of(l)));
    }
  }

  @Nested
  class When_L_IsAdded {
    {
      store.putAll(List.of(l));
      policy.addAll(store, List.of(l));
    }

    @Test
    void remove_L_ShouldWork() {
      store.remove(l);
      policy.removeAll(store, List.of(l));
    }

    @Test
    void add_A_ShouldWork() {
      store.putAll(List.of(a));
      policy.addAll(store, List.of(a));
    }

    @Test
    void add_A_and_H_ShouldFailBecause_Z_IsProvidedTwice() {
      store.putAll(List.of(a, h));
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.addAll(store, List.of(a, h)));
    }
  }

  @Nested
  class WhenClassesAAndBAndCAndDAreAdded {
    {
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

    @Test
    void addLShouldWork() {
      store.putAll(List.of(l));
      policy.addAll(store, List.of(l));
    }
  }

  @Nested
  class WhenClassesMAndNAndOAreAdded {
    {
      store.putAll(List.of(m, n, o));
      policy.addAll(store, List.of(m, n, o));
    }

    @Test
    void removeMShouldFailAsRequiredByN() {
      store.remove(m);
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.removeAll(store, List.of(m)));
    }

    @Test
    void removeNShouldFailAsRequiredByM() {
      store.remove(n);
      assertThrows(ViolatesSingularDependencyException.class, () -> policy.removeAll(store, List.of(n)));
    }

    @Test
    void removeOShouldWorkAsMOnlyDependsOnItOptionally() {
      store.remove(o);
      policy.removeAll(store, List.of(o));
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
