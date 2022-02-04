package hs.ddif.core.config.consistency;

import hs.ddif.annotations.Opt;
import hs.ddif.core.config.standard.DefaultInjectable;
import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.instantiation.InstanceFactories;
import hs.ddif.core.instantiation.InstantiatorBindingMap;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.ScopeResolverManager;
import hs.ddif.core.instantiation.ScopeResolverManagers;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.QualifiedTypeStore;
import hs.ddif.core.util.Annotations;
import hs.ddif.core.util.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

public class InjectorStoreConsistencyPolicyLargeGraphTest {
  private final Random rnd = new Random(4);

  @Test
  void largeGraphTest() {
    InstantiatorFactory instantiatorFactory = InstanceFactories.create();
    InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
    ScopeResolverManager scopeResolverManager = ScopeResolverManagers.create();
    InjectorStoreConsistencyPolicy<Injectable> policy = new InjectorStoreConsistencyPolicy<>(instantiatorBindingMap, scopeResolverManager);
    QualifiedTypeStore<Injectable> store = new QualifiedTypeStore<>(policy);
    List<Injectable> knownInjectables = new ArrayList<>();
    List<Class<?>> classes = List.of(String.class, Integer.class, A.class, B.class, C.class, D.class, E.class, F.class, G.class, H.class, I.class, J.class);

    Singleton annotation = Annotations.of(Singleton.class);

    for(int i = 0; i < 10000; i++) {
      policy.checkInvariants();

      int randomBindings = Math.min(rnd.nextInt(4), knownInjectables.size());
      List<Binding> bindings = new ArrayList<>();

      for(int j = 0; j < randomBindings; j++) {
        Injectable target = knownInjectables.get(rnd.nextInt(knownInjectables.size()));

        bindings.add(new SimpleBinding(new Key(target.getType(), target.getQualifiers())));
      }

      Injectable injectable = new DefaultInjectable(
        classes.get(rnd.nextInt(classes.size())),
        Set.of(Annotations.named("instance-" + i)),
        bindings,
        annotation,
        null,
        injections -> null
      );

      store.put(injectable);

      knownInjectables.add(injectable);
    }
  }

  private static class SimpleBinding implements Binding {
    private final Key key;

    public SimpleBinding(Key key) {
      this.key = key;
    }

    @Override
    public Key getKey() {
      return key;
    }

    @Override
    public AccessibleObject getAccessibleObject() {
      return null;
    }

    @Override
    public Parameter getParameter() {
      return null;
    }
  }

  interface Z {
  }

  public static class A {
  }

  public static class E extends A {
  }

  public static class B {
    @Inject Z z;  // cyclic if it is D, not if it is H or K
  }

  public static class C {
    @Inject B b;
    @Inject @Opt Provider<D> d;  // not a circular dependency, and not required
  }

  public static class D implements Z {
    @Inject C c;
  }

  public static class F {
    @Inject B b;
    @Inject C c;
    @Inject @Nullable Z z;
  }

  public static class G {
    @Inject K k;
  }

  public static class H implements Z {
  }

  public static class I {
    @Inject Z z;
    @Inject E e;
  }

  public static class J {
    @Inject Provider<Z> h;
  }

  public static class K implements Z {
    @Inject A a;
  }
}
