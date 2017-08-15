package hs.ddif.core.store;

import hs.ddif.core.Binding;
import hs.ddif.core.ClassInjectable;
import hs.ddif.core.InstanceInjectable;
import hs.ddif.core.Key;
import hs.ddif.core.test.injectables.BeanWithBigRedInjection;
import hs.ddif.core.test.injectables.BigRedBean;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.AnnotationUtils;
import hs.ddif.core.util.Value;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Random;
import java.util.RandomAccess;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class InjectableStoreTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private InjectableStore<Injectable> store;

  @Before
  public void before() {
    this.store = new InjectableStore<>();
  }

  @Test
  public void shouldStore() {
    ClassInjectable injectable = new ClassInjectable(BeanWithBigRedInjection.class);

    store.put(injectable);

    for(Map.Entry<AccessibleObject, Binding[]> entry : injectable.getBindings().entrySet()) {
      if(!(entry.getKey() instanceof Constructor)) {
        Key requiredKey = entry.getValue()[0].getRequiredKey();

        assertThat(store.resolve(requiredKey.getType(), (Object[])requiredKey.getQualifiersAsArray()), empty());
      }
    }

    injectable = new ClassInjectable(BigRedBean.class);

    store.put(injectable);

    for(Map.Entry<AccessibleObject, Binding[]> entry : injectable.getBindings().entrySet()) {
      if(!(entry.getKey() instanceof Constructor)) {
        assertThat(store.resolve(entry.getValue()[0].getRequiredKey().getType()), hasSize(1));
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

  private void setupStore() {
    store.put(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))));
    store.put(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-b")), AnnotationDescriptor.describe(Red.class)));
    store.put(new InstanceInjectable("c", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))));
    store.put(new InstanceInjectable(4L));
    store.put(new InstanceInjectable(2));
    store.put(new InstanceInjectable(6L, AnnotationDescriptor.describe(Red.class)));
    store.put(new InstanceInjectable(8));
    store.put(new InstanceInjectable(new Random()));
  }

  @Test
  public void shouldResolve() {
    setupStore();

    // All Strings
    assertEquals(3, store.resolve(String.class).size());

    // All Strings with a specific annotation
    assertEquals(1, store.resolve(String.class, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-b"))).size());

    // All Numbers
    assertEquals(4, store.resolve(Number.class).size());

    // All Objects
    assertEquals(8, store.resolve(Object.class).size());

    // All Numbers (using Matcher)
    assertEquals(4, store.resolve(Object.class, new Matcher() {
      @Override
      public boolean matches(Class<?> cls) {
        return Number.class.isAssignableFrom(cls);
      }
    }).size());

    // All Red Objects
    assertEquals(2, store.resolve(Object.class, AnnotationDescriptor.describe(Red.class)).size());

    // All Red Objects (using annotation)
    assertEquals(2, store.resolve(Object.class, AnnotationUtils.of(Red.class)).size());

    // All Red Numbers
    assertEquals(1, store.resolve(Number.class, AnnotationDescriptor.describe(Red.class)).size());

    // All Comparable Serializables
    assertEquals(7, store.resolve(Serializable.class, Comparable.class).size());

    // All RandomAccess Serializables
    assertEquals(0, store.resolve(Serializable.class, RandomAccess.class).size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void resolveShouldThrowExceptionWhenCriteriaIsUnsupported() {
    setupStore();
    store.resolve(Object.class, "Unsupported");
  }

  @Test(expected = IllegalArgumentException.class)
  public void putShouldThrowExceptionWhenInjectableIsNull() {
    store.put(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void removeShouldThrowExceptionWhenInjectableIsNull() {
    store.remove(null);
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
