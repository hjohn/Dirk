package hs.ddif.core.store;

import hs.ddif.core.api.Matcher;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.inject.store.FieldInjectableFactory;
import hs.ddif.core.inject.store.InstanceInjectableFactory;
import hs.ddif.core.inject.store.MethodInjectableFactory;
import hs.ddif.core.test.injectables.BeanWithBigRedInjection;
import hs.ddif.core.test.injectables.BigRedBean;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.test.qualifiers.Small;
import hs.ddif.core.util.Annotations;
import hs.ddif.core.util.TypeReference;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.leangen.geantyref.TypeFactory;

public class InjectableStoreTest {
  private static final Annotation RED = Annotations.of(Red.class);
  private static final Annotation BIG = Annotations.of(Big.class);

  private final ClassInjectableFactory classInjectableFactory = new ClassInjectableFactory(ResolvableInjectable::new);
  private final FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(ResolvableInjectable::new);
  private final MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(ResolvableInjectable::new);
  private final InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(ResolvableInjectable::new);

  private InjectableStore<Injectable> store;

  @Before
  public void before() {
    this.store = new InjectableStore<>();
  }

  @Test
  public void shouldStore() {
    store.put(classInjectableFactory.create(BeanWithBigRedInjection.class));

    assertThat(store.resolve(new Key(Object.class, Set.of(BIG, RED)))).isEmpty();

    store.put(classInjectableFactory.create(BigRedBean.class));

    assertThat(store.resolve(new Key(Object.class, Set.of(BIG, RED)))).hasSize(1);
  }

  @Test
  public void shouldNotAllowRemovingInjectablesThatWereNeverAdded() {
    store.put(classInjectableFactory.create(Y.class));

    assertThat(store.resolve(new Key(Y.class))).isNotNull();
    assertThat(store.resolve(new Key(X.class))).isNotNull();

    assertThrows(NoSuchInjectableException.class, () -> store.remove(classInjectableFactory.create(X.class)));
  }

  @Test
  public void shouldStoreWithQualifier() {
    store.put(instanceInjectableFactory.create("a", Annotations.named("parameter-a")));
    store.put(instanceInjectableFactory.create("a", Annotations.named("parameter-b")));
    store.put(instanceInjectableFactory.create("c", Annotations.named("parameter-c")));
    store.put(instanceInjectableFactory.create("d", Annotations.named("parameter-c")));
    store.put(instanceInjectableFactory.create("f", Annotations.named("parameter-e")));

    assertThat(store.resolve(new Key(String.class))).hasSize(5);
    assertThat(store.resolve(new Key(String.class, Set.of(Annotations.named("parameter-a"))))).hasSize(1);
    assertThat(store.resolve(new Key(String.class, Set.of(Annotations.named("parameter-b"))))).hasSize(1);
    assertThat(store.resolve(new Key(String.class, Set.of(Annotations.named("parameter-c"))))).hasSize(2);
    assertThat(store.resolve(new Key(String.class, Set.of(Annotations.named("parameter-d"))))).hasSize(0);
    assertThat(store.resolve(new Key(String.class, Set.of(Annotations.named("parameter-e"))))).hasSize(1);
  }

  @Test
  public void shouldThrowExceptionWhenStoringSameInstanceWithSameQualifier() {
    store.put(instanceInjectableFactory.create(new String("a"), Annotations.named("parameter-a")));

    assertThatThrownBy(() -> store.put(instanceInjectableFactory.create(new String("a"), Annotations.named("parameter-a"))))
      .isExactlyInstanceOf(DuplicateInjectableException.class)
      .hasMessage("class java.lang.String already registered for: Injectable[@javax.inject.Named(value=parameter-a) java.lang.String]")
      .hasNoCause();
  }

  @Test
  public void shouldRemoveWithQualifier() {
    store.put(instanceInjectableFactory.create("a", Annotations.named("parameter-a")));
    store.put(instanceInjectableFactory.create("a", Annotations.named("parameter-b")));
    store.put(instanceInjectableFactory.create("c", Annotations.named("parameter-c")));

    store.remove(instanceInjectableFactory.create("a", Annotations.named("parameter-a")));
    store.remove(instanceInjectableFactory.create("a", Annotations.named("parameter-b")));
    store.remove(instanceInjectableFactory.create("c", Annotations.named("parameter-c")));
  }

  private void setupStore() {
    store.put(instanceInjectableFactory.create("a", Annotations.named("parameter-a")));
    store.put(instanceInjectableFactory.create("a", Annotations.named("parameter-b"), Annotations.of(Red.class)));
    store.put(instanceInjectableFactory.create("c", Annotations.named("parameter-c")));
    store.put(instanceInjectableFactory.create(4L));
    store.put(instanceInjectableFactory.create(2));
    store.put(instanceInjectableFactory.create(6L, Annotations.of(Red.class)));
    store.put(instanceInjectableFactory.create(8));
    store.put(instanceInjectableFactory.create(new Random()));
  }

  @Test
  public void shouldResolve() {
    setupStore();

    // All Strings
    assertEquals(3, store.resolve(new Key(String.class)).size());

    // All Strings with a specific annotation
    assertEquals(1, store.resolve(new Key(String.class, Set.of(Annotations.named("parameter-b")))).size());

    // All Numbers
    assertEquals(4, store.resolve(new Key(Number.class)).size());

    // All Objects
    assertEquals(8, store.resolve(new Key(Object.class)).size());

    // All Numbers (using Matcher)
    assertEquals(4, store.resolve(new Key(Object.class), new Criteria(Set.of(), Set.of(new Matcher() {
      @Override
      public boolean matches(Class<?> cls) {
        return Number.class.isAssignableFrom(cls);
      }
    }))).size());

    // All Red Objects
    assertEquals(2, store.resolve(new Key(Object.class, Set.of(RED))).size());

    // All Red Numbers
    assertEquals(1, store.resolve(new Key(Number.class, Set.of(RED))).size());

    // All Serializables
    assertEquals(8, store.resolve(new Key(Serializable.class)).size());

    // All Comparable
    assertEquals(7, store.resolve(new Key(Comparable.class)).size());

    // All Comparable<Long>
    assertEquals(2, store.resolve(new Key(new TypeReference<Comparable<Long>>() {}.getType())).size());

    // All Comparable<String> Serializables (unsupported for now)
    //    assertEquals(3, store.resolve(Serializable.class, new TypeReference<Comparable<String>>() {}.getType()).size());

    // All RandomAccess Serializables
    assertEquals(0, store.resolve(new Key(Serializable.class), new Criteria(Set.of(RandomAccess.class), Set.of())).size());
  }

  @Test
  public void resolveShouldFindInjectablesWhenCriteriaIsAnAnnotationClass() {  // Tests that Annotation classes are converted to a descriptor internally
    setupStore();

    assertEquals(2, store.resolve(new Key(Object.class, Set.of(RED))).size());
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
      store.put(classInjectableFactory.create(A.class));
      store.put(classInjectableFactory.create(A.class));
      fail();
    }
    catch(DuplicateInjectableException e) {
      // expected, check if store is intact:
      assertTrue(store.contains(new Key(A.class)));
      assertEquals(1, store.resolve(new Key(A.class)).size());
    }
  }

  @Test
  public void putAllShouldRejectDuplicateBeans() {
    try {
      store.putAll(List.of(classInjectableFactory.create(A.class), classInjectableFactory.create(A.class)));
      fail();
    }
    catch(DuplicateInjectableException e) {
      // expected, check if store is intact:
      assertFalse(store.contains(new Key(A.class)));
    }
  }

  @Test
  public void putAllShouldRejectDuplicateBeansWhenOnePresentAlready() {
    try {
      store.put(classInjectableFactory.create(A.class));
      store.putAll(List.of(classInjectableFactory.create(B.class), classInjectableFactory.create(A.class)));
      fail();
    }
    catch(DuplicateInjectableException e) {
      // expected, check if store is intact:
      assertTrue(store.contains(new Key(A.class)));
      assertFalse(store.contains(new Key(B.class)));
      assertEquals(1, store.resolve(new Key(A.class)).size());
    }
  }

  @Test
  public void containsShouldWork() {
    store.put(classInjectableFactory.create(A.class));

    assertTrue(store.contains(new Key(A.class, Set.of(BIG))));
    assertTrue(store.contains(new Key(A.class, Set.of(RED))));
    assertTrue(store.contains(new Key(A.class, Set.of(BIG, RED))));
    assertFalse(store.contains(new Key(B.class, Set.of(BIG, RED))));
    assertFalse(store.contains(new Key(A.class, Set.of(Annotations.of(Small.class), RED))));

    store.put(classInjectableFactory.create(StringProvider.class));

    assertTrue(store.contains(new Key(StringProvider.class)));

    // Ensure lookup by Provider is not possible:
    assertFalse(store.contains(new Key(TypeFactory.parameterizedClass(Provider.class, String.class))));
    assertFalse(store.contains(new Key(new TypeReference<Provider<String>>() {}.getType())));
    assertFalse(store.contains(new Key(new TypeReference<Provider<Long>>() {}.getType())));
  }

  @Test
  public void shouldAllowRegistrationOfMethodsAndFieldsThatProvideTheExactSameType() throws Exception {
    store.put(fieldInjectableFactory.create(P.class.getDeclaredField("a"), P.class));
    store.put(fieldInjectableFactory.create(P.class.getDeclaredField("b"), P.class));

    store.put(methodInjectableFactory.create(P.class.getDeclaredMethod("a"), P.class));
    store.put(methodInjectableFactory.create(P.class.getDeclaredMethod("b"), P.class));

    assertThat(store.resolve(new Key(A.class))).hasSize(4);
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

  public static class P {
    public A a = new A();
    public A b = new A();

    public A a() {
      return new A();
    }

    public A b() {
      return new A();
    }
  }

  public static class X {
  }

  public static class Y extends X implements Z {
  }

  public interface Z {
  }
}
