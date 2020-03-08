package hs.ddif.core.store;

import hs.ddif.core.ProvidedInjectable;
import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.inject.store.InstanceInjectable;
import hs.ddif.core.test.injectables.BeanWithBigRedInjection;
import hs.ddif.core.test.injectables.BigRedBean;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.test.qualifiers.Small;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.AnnotationUtils;
import hs.ddif.core.util.TypeReference;
import hs.ddif.core.util.Value;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.RandomAccess;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.leangen.geantyref.TypeFactory;

public class InjectableStoreTest {
  @Rule @SuppressWarnings("deprecation")
  public ExpectedException thrown = ExpectedException.none();

  private InjectableStore<Injectable> store;

  @Before
  public void before() {
    this.store = new InjectableStore<>();
  }

  @Test
  public void shouldStore() {
    store.put(new ClassInjectable(BeanWithBigRedInjection.class));

    assertThat(store.resolve(Object.class, Big.class, Red.class)).isEmpty();

    store.put(new ClassInjectable(BigRedBean.class));

    assertThat(store.resolve(Object.class, Big.class, Red.class)).hasSize(1);
  }

  @Test
  public void shouldStoreWithQualifier() {
    store.put(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))));
    store.put(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-b"))));
    store.put(new InstanceInjectable("c", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))));
    store.put(new ProvidedInjectable(new StringProvider(), AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))));
    store.put(new ProvidedInjectable(new StringProvider(), AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-e"))));

    assertThat(store.resolve(String.class), hasSize(5));
    assertThat(store.resolve(String.class, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))), hasSize(1));
    assertThat(store.resolve(String.class, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-b"))), hasSize(1));
    assertThat(store.resolve(String.class, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))), hasSize(2));
    assertThat(store.resolve(String.class, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-d"))), hasSize(0));
    assertThat(store.resolve(String.class, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-e"))), hasSize(1));
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
    StringProvider provider1 = new StringProvider();
    StringProvider provider2 = new StringProvider();

    store.put(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))));
    store.put(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-b"))));
    store.put(new InstanceInjectable("c", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))));
    store.put(new ProvidedInjectable(provider1, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))));
    store.put(new ProvidedInjectable(provider2, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-e"))));

    store.remove(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-a"))));
    store.remove(new InstanceInjectable("a", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-b"))));
    store.remove(new InstanceInjectable("c", AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))));
    store.remove(new ProvidedInjectable(provider1, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-c"))));
    store.remove(new ProvidedInjectable(provider2, AnnotationDescriptor.describe(Named.class, new Value("value", "parameter-e"))));
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

    // All Serializables
    assertEquals(8, store.resolve(Serializable.class).size());

    // All Comparable
    assertEquals(7, store.resolve(Comparable.class).size());

    // All Comparable<Long>
    assertEquals(2, store.resolve(new TypeReference<Comparable<Long>>() {}.getType()).size());

    // All Comparable<String> Serializables (unsupported for now)
    //    assertEquals(3, store.resolve(Serializable.class, new TypeReference<Comparable<String>>() {}.getType()).size());

    // All RandomAccess Serializables
    assertEquals(0, store.resolve(Serializable.class, RandomAccess.class).size());
  }

  @Test
  public void resolveShouldFindInjectablesWhenCriteriaIsAnAnnotationClass() {  // Tests that Annotation classes are converted to a descriptor internally
    setupStore();

    assertEquals(2, store.resolve(Object.class, Red.class).size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void resolveShouldThrowExceptionWhenCriteriaIsUnsupported() {
    setupStore();
    store.resolve(Object.class, "Unsupported");
  }

  @Test(expected = NullPointerException.class)
  public void putShouldThrowExceptionWhenInjectableIsNull() {
    store.put(null);
  }

  @Test(expected = NullPointerException.class)
  public void removeShouldThrowExceptionWhenInjectableIsNull() {
    store.remove(null);
  }

  @Test
  public void putShouldRejectDuplicateBeans() {
    try {
      store.put(new ClassInjectable(A.class));
      store.put(new ClassInjectable(A.class));
      fail();
    }
    catch(DuplicateBeanException e) {
      // expected, check if store is intact:
      assertTrue(store.contains(A.class));
      assertEquals(1, store.resolve(A.class).size());
    }
  }

  @Test
  public void putAllShouldRejectDuplicateBeans() {
    try {
      store.putAll(List.of(new ClassInjectable(A.class), new ClassInjectable(A.class)));
      fail();
    }
    catch(DuplicateBeanException e) {
      // expected, check if store is intact:
      assertFalse(store.contains(A.class));
    }
  }

  @Test
  public void putAllShouldRejectDuplicateBeansWhenOnePresentAlready() {
    try {
      store.put(new ClassInjectable(A.class));
      store.putAll(List.of(new ClassInjectable(B.class), new ClassInjectable(A.class)));
      fail();
    }
    catch(DuplicateBeanException e) {
      // expected, check if store is intact:
      assertTrue(store.contains(A.class));
      assertFalse(store.contains(B.class));
      assertEquals(1, store.resolve(A.class).size());
    }
  }

  @Test
  public void containsShouldWork() {
    store.put(new ClassInjectable(A.class));

    assertTrue(store.contains(A.class, Big.class));
    assertTrue(store.contains(A.class, Red.class));
    assertTrue(store.contains(A.class, Big.class, Red.class));
    assertFalse(store.contains(B.class, Big.class, Red.class));
    assertFalse(store.contains(A.class, Small.class, Red.class));

    store.put(new ClassInjectable(StringProvider.class));

    assertTrue(store.contains(TypeFactory.parameterizedClass(Provider.class, String.class)));
    assertTrue(store.contains(new TypeReference<Provider<String>>() {}.getType()));
    assertFalse(store.contains(new TypeReference<Provider<Long>>() {}.getType()));
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

  interface StringProviderInterface extends Provider<String> {
  }

  public static class StringProvider implements Provider<String> {
    @Override
    public String get() {
      return "string";
    }
  }
}
