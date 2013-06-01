package hs.ddif;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import hs.ddif.test.injectables.BeanWithBigRedInjection;
import hs.ddif.test.injectables.BigRedBean;
import hs.ddif.test.qualifiers.Big;
import hs.ddif.test.qualifiers.Red;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

public class InjectableStoreTest {
  private InjectableStore store;

  @Before
  public void before() {
    this.store = new InjectableStore();
  }

  @Test
  public void shouldStore() {
    store.put(new ClassInjectable(BeanWithBigRedInjection.class));

    Map<AccessibleObject, Binding> bindings = store.getBindings(BeanWithBigRedInjection.class);

    for(Map.Entry<AccessibleObject, Binding> entry : bindings.entrySet()) {
      if(!(entry.getKey() instanceof Constructor)) {
        assertThat(store.resolve(entry.getValue().getRequiredKeys()[0]), empty());
      }
    }

    store.put(new ClassInjectable(BigRedBean.class));

    bindings = store.getBindings(BeanWithBigRedInjection.class);

    for(Map.Entry<AccessibleObject, Binding> entry : bindings.entrySet()) {
      if(!(entry.getKey() instanceof Constructor)) {
        assertThat(store.resolve(entry.getValue().getRequiredKeys()[0]), hasSize(1));
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldCreateDG() {
    assertThat(getInTopologicalOrder(BeanWithBigRedInjection.class, BigRedBean.class), contains(BigRedBean.class, BeanWithBigRedInjection.class));
  }

  @Test
  public void shouldCreateDG2() {
    List<Class<?>> list = getInTopologicalOrder(H.class, G.class, F.class, E.class, D.class, C.class, B.class, A.class);

    assertTrue(list.size() == 8);
    assertTrue(list.indexOf(A.class) == 0);
    assertTrue(list.indexOf(B.class) == 1);
    assertTrue(list.indexOf(C.class) < list.indexOf(D.class));
    assertTrue(list.indexOf(C.class) < list.indexOf(F.class));
    assertTrue(list.indexOf(C.class) < list.indexOf(G.class));
    assertTrue(list.indexOf(C.class) < list.indexOf(H.class));
    assertTrue(list.indexOf(D.class) < list.indexOf(H.class));
    assertTrue(list.indexOf(E.class) < list.indexOf(F.class));
    assertTrue(list.indexOf(E.class) < list.indexOf(H.class));
    assertTrue(list.indexOf(F.class) < list.indexOf(H.class));
    assertTrue(list.indexOf(G.class) < list.indexOf(H.class));
    assertTrue(list.indexOf(H.class) == list.size() - 1);
  }

  public static List<Class<?>> getInTopologicalOrder(Class<?>... classes) {
    InjectableStore store = new InjectableStore();
    DirectedGraph<Class<?>> dg = new DirectedGraph<>();

    for(Class<?> cls : classes) {
      dg.addNode(cls);
      store.put(new ClassInjectable(cls));
    }

    for(Class<?> cls : classes) {
      for(Binding binding : store.getBindings(cls).values()) {
        Key[] requiredKeys = binding.getRequiredKeys();

        for(Key requiredKey : requiredKeys) {
          for(Injectable injectable : store.resolve(requiredKey)) {
            dg.addEdge(injectable.getInjectableClass(), cls);
          }
        }
      }
    }

    return TopologicalSort.sort(dg);
  }

  @Big @Red
  public static class A {
  }

  public static class B {
    @Inject @Big @Red
    Object injection;
  }

  public static class C {
    @Inject
    A injection1;

    @Inject
    B injection2;
  }

  public static class D {
    @Inject
    C injection;
  }

  public static class E {
    @Inject
    B injection;
  }

  public static class F {
    @Inject
    C injection1;

    @Inject
    E injection2;
  }

  public static class G {
    @Inject @Big
    Object injection1;

    @Inject
    C injection2;
  }

  public static class H {
    @Inject
    D injection1;

    @Inject
    F injection2;

    @Inject
    G injection3;
  }
}
