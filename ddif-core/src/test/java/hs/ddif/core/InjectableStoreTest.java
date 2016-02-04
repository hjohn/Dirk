package hs.ddif.core;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import hs.ddif.core.test.injectables.BeanWithBigRedInjection;
import hs.ddif.core.test.injectables.BigRedBean;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Red;

public class InjectableStoreTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

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

  @Test
  public void shouldStoreWithQualifier() {
    store.put(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))));
    store.put(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-b"))));
    store.put(new InstanceInjectable("c", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))));

    assertThat(store.resolve(String.class), hasSize(3));
    assertThat(store.resolve(String.class, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))), hasSize(1));
    assertThat(store.resolve(String.class, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-b"))), hasSize(1));
    assertThat(store.resolve(String.class, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))), hasSize(1));
    assertThat(store.resolve(String.class, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-d"))), hasSize(0));
  }

  @Test
  public void shouldThrowExceptionWhenStoringSameInstanceWithSameQualifier() {
    store.put(new InstanceInjectable(new String("a"), AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))));

    thrown.expect(DuplicateBeanException.class);
    thrown.expectMessage(" already registered for: Injectable-Instance(class java.lang.String + ");

    store.put(new InstanceInjectable(new String("a"), AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))));
  }

  @Test
  public void shouldRemoveWithQualifier() {
    store.put(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))));
    store.put(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-b"))));
    store.put(new InstanceInjectable("c", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))));

    store.remove(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))));
    store.remove(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-b"))));
    store.remove(new InstanceInjectable("c", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))));
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
