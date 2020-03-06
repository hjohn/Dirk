package hs.ddif.plugins;

import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.store.Injectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.scan.test.qualifiers.Big;
import hs.ddif.scan.test.qualifiers.Red;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DependencySorterTest {

  @Test
  public void shouldCreateDirectedGraph() {
    InjectableStore<Injectable> store = new InjectableStore<>();
    Set<ClassInjectable> classInjectables = new HashSet<>();

    for(Class<?> cls : new Class<?>[] {H.class, G.class, F.class, E.class, D.class, C.class, B.class, A.class}) {
      ClassInjectable injectable = new ClassInjectable(cls);

      classInjectables.add(injectable);
      store.put(injectable);
    }

    List<Type> list = DependencySorter.getInTopologicalOrder(store, classInjectables);

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

  @Big @Red
  public static class A {
  }

  public static class B {
    @Inject @Big @Red Object injection;
  }

  public static class C {
    @Inject A injection1;
    @Inject B injection2;
  }

  public static class D {
    @Inject C injection;
  }

  public static class E {
    @Inject B injection;
  }

  public static class F {
    @Inject C injection1;
    @Inject E injection2;
  }

  public static class G {
    @Inject @Big Object injection1;
    @Inject C injection2;
  }

  public static class H {
    @Inject D injection1;
    @Inject F injection2;
    @Inject G injection3;
  }
}
