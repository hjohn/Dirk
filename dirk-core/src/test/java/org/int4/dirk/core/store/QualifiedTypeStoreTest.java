package org.int4.dirk.core.store;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.Supplier;

import org.int4.dirk.api.TypeLiteral;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.api.definition.DuplicateDependencyException;
import org.int4.dirk.api.definition.MissingDependencyException;
import org.int4.dirk.core.InjectableFactories;
import org.int4.dirk.core.definition.ClassInjectableFactory;
import org.int4.dirk.core.definition.FieldInjectableFactory;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.definition.InstanceInjectableFactory;
import org.int4.dirk.core.definition.Key;
import org.int4.dirk.core.definition.MethodInjectableFactory;
import org.int4.dirk.core.test.injectables.BeanWithBigRedInjection;
import org.int4.dirk.core.test.injectables.BigRedBean;
import org.int4.dirk.core.test.qualifiers.Big;
import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.core.test.qualifiers.Small;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

public class QualifiedTypeStoreTest {
  private static final Annotation RED = Annotations.of(Red.class);
  private static final Annotation BIG = Annotations.of(Big.class);

  private final InjectableFactories injectableFactories = new InjectableFactories();
  private final ClassInjectableFactory classInjectableFactory = injectableFactories.forClass();
  private final MethodInjectableFactory methodInjectableFactory = injectableFactories.forMethod();
  private final FieldInjectableFactory fieldInjectableFactory = injectableFactories.forField();
  private final InstanceInjectableFactory instanceInjectableFactory = injectableFactories.forInstance();

  private QualifiedTypeStore<Injectable<?>> store;

  @BeforeEach
  public void beforeEach() {
    this.store = new QualifiedTypeStore<>(i -> new Key(i.getType(), i.getQualifiers()), Injectable::getTypes);
  }

  @Test
  public void shouldStore() throws Exception {
    store.put(classInjectableFactory.create(BeanWithBigRedInjection.class));

    assertThat(store.resolve(new Key(Object.class, Set.of(BIG, RED)))).isEmpty();

    store.put(classInjectableFactory.create(BigRedBean.class));

    assertThat(store.resolve(new Key(Object.class, Set.of(BIG, RED)))).hasSize(1);
  }

  @Test
  public void shouldNotAllowRemovingInjectablesThatWereNeverAdded() throws Exception {
    store.put(classInjectableFactory.create(Y.class));

    assertThat(store.resolve(new Key(Y.class))).isNotNull();
    assertThat(store.resolve(new Key(X.class))).isNotNull();

    assertThrows(MissingDependencyException.class, () -> store.remove(classInjectableFactory.create(X.class)));
  }

  @Test
  public void shouldStoreWithQualifier() throws Exception {
    store.put(instanceInjectableFactory.create("a", named("parameter-a")));
    store.put(instanceInjectableFactory.create("a", named("parameter-b")));
    store.put(instanceInjectableFactory.create("c", named("parameter-c")));
    store.put(instanceInjectableFactory.create("d", named("parameter-c")));
    store.put(instanceInjectableFactory.create("f", named("parameter-e")));

    assertThat(store.resolve(new Key(String.class))).hasSize(5);
    assertThat(store.resolve(new Key(String.class, Set.of(named("parameter-a"))))).hasSize(1);
    assertThat(store.resolve(new Key(String.class, Set.of(named("parameter-b"))))).hasSize(1);
    assertThat(store.resolve(new Key(String.class, Set.of(named("parameter-c"))))).hasSize(2);
    assertThat(store.resolve(new Key(String.class, Set.of(named("parameter-d"))))).hasSize(0);
    assertThat(store.resolve(new Key(String.class, Set.of(named("parameter-e"))))).hasSize(1);
  }

  @Test
  public void shouldStoreMultipleOfSameType() throws Exception {
    store.put(instanceInjectableFactory.create("a"));
    store.put(instanceInjectableFactory.create("b"));
    store.put(instanceInjectableFactory.create("c"));
    store.put(instanceInjectableFactory.create("d"));

    assertThat(store.resolve(new Key(String.class))).hasSize(4);
  }

  @Test
  public void shouldThrowExceptionWhenStoringSameInstanceWithSameQualifier() throws Exception {
    store.put(instanceInjectableFactory.create(new String("a"), named("parameter-a")));

    assertThatThrownBy(() -> store.put(instanceInjectableFactory.create(new String("a"), named("parameter-a"))))
      .isExactlyInstanceOf(DuplicateDependencyException.class)
      .hasMessage("[@jakarta.inject.Named(value=parameter-a) java.lang.String] already exists")
      .hasNoCause();
  }

  @Test
  public void shouldRemoveWithQualifier() throws Exception {
    store.put(instanceInjectableFactory.create("a", named("parameter-a")));
    store.put(instanceInjectableFactory.create("a", named("parameter-b")));
    store.put(instanceInjectableFactory.create("c", named("parameter-c")));

    store.remove(instanceInjectableFactory.create("a", named("parameter-a")));
    store.remove(instanceInjectableFactory.create("a", named("parameter-b")));
    store.remove(instanceInjectableFactory.create("c", named("parameter-c")));
  }

  private void setupStore() throws Exception {
    store.put(instanceInjectableFactory.create("a", named("parameter-a")));
    store.put(instanceInjectableFactory.create("a", named("parameter-b"), Annotations.of(Red.class)));
    store.put(instanceInjectableFactory.create("c", named("parameter-c")));
    store.put(instanceInjectableFactory.create(4L));
    store.put(instanceInjectableFactory.create(2));
    store.put(instanceInjectableFactory.create(6L, Annotations.of(Red.class)));
    store.put(instanceInjectableFactory.create(8));
    store.put(instanceInjectableFactory.create(new Random()));
    store.put(instanceInjectableFactory.create(new ComparableButNotSerializable(), Annotations.of(Red.class)));
    store.put(classInjectableFactory.create(ComparableButNotSerializable.class)); // Comparator<String> but not Serializable, as anonymous class to avoid losing type information
  }

  public static class ComparableButNotSerializable implements Comparable<String> {
    @Override
    public int compareTo(String o) {
      return 0;
    }
  }

  @Test
  public void shouldResolve() throws Exception {
    setupStore();

    // All Strings
    assertEquals(3, store.resolve(new Key(String.class)).size());

    // All Strings with a specific annotation
    assertEquals(1, store.resolve(new Key(String.class, Set.of(named("parameter-b")))).size());

    // All Numbers
    assertEquals(4, store.resolve(new Key(Number.class)).size());

    // All Objects
    assertEquals(10, store.resolve(new Key(Object.class)).size());

    // All Red Objects
    assertEquals(3, store.resolve(new Key(Object.class, Set.of(RED))).size());

    // All Red Numbers
    assertEquals(1, store.resolve(new Key(Number.class, Set.of(RED))).size());

    // All Serializables
    assertEquals(8, store.resolve(new Key(Serializable.class)).size());

    // All Comparable
    assertEquals(9, store.resolve(new Key(Comparable.class)).size());

    // All Comparable<Long>
    assertEquals(2, store.resolve(new Key(new TypeLiteral<Comparable<Long>>() {}.getType())).size());

    // All Comparable<String> Serializables
    assertEquals(3, store.resolve(new Key(Types.wildcardExtends(Serializable.class, new TypeLiteral<Comparable<String>>() {}.getType()))).size());

    // All Red Comparable<String> Serializables
    assertEquals(1, store.resolve(new Key(Types.wildcardExtends(Serializable.class, new TypeLiteral<Comparable<String>>() {}.getType()), Set.of(RED))).size());

    // All Comparable<String>
    assertEquals(5, store.resolve(new Key(new TypeLiteral<Comparable<String>>() {}.getType())).size());

    // All Red Comparable<String>
    assertEquals(2, store.resolve(new Key(new TypeLiteral<Comparable<String>>() {}.getType(), Set.of(RED))).size());

    // All RandomAccess Serializables
    assertEquals(0, store.resolve(new Key(Types.wildcardExtends(Serializable.class, RandomAccess.class))).size());

    // All Comparable<Integer>
    assertEquals(2, store.resolve(new Key(Types.wildcardExtends(new TypeLiteral<Comparable<Integer>>() {}.getType()))).size());

    // All CharSequence & Number (0)
    assertEquals(0, store.resolve(new Key(Types.wildcardExtends(CharSequence.class, Number.class))).size());
  }

  @Test
  public void resolveShouldFindInjectablesWhenCriteriaIsAnAnnotationClass() throws Exception {  // Tests that Annotation classes are converted to a descriptor internally
    setupStore();

    assertEquals(3, store.resolve(new Key(Object.class, Set.of(RED))).size());
  }

  @Test
  public void putShouldThrowExceptionWhenInjectableIsNull() {
    assertThatThrownBy(() -> store.put(null))
      .isExactlyInstanceOf(NullPointerException.class);
  }

  @Test
  public void removeShouldThrowExceptionWhenInjectableIsNull() {
    assertThatThrownBy(() -> store.remove(null))
      .isExactlyInstanceOf(NullPointerException.class);
  }

  @Test
  public void putShouldRejectDuplicateBeans() throws DefinitionException {
    try {
      store.put(classInjectableFactory.create(A.class));
      store.put(classInjectableFactory.create(A.class));
      fail();
    }
    catch(DuplicateDependencyException e) {
      // expected, check if store is intact:
      assertTrue(store.contains(new Key(A.class)));
      assertEquals(1, store.resolve(new Key(A.class)).size());
    }
  }

  @Test
  public void putAllShouldRejectDuplicateBeans() throws DefinitionException {
    try {
      store.putAll(List.of(classInjectableFactory.create(A.class), classInjectableFactory.create(A.class)));
      fail();
    }
    catch(DuplicateDependencyException e) {
      // expected, check if store is intact:
      assertFalse(store.contains(new Key(A.class)));
    }
  }

  @Test
  public void putAllShouldRejectDuplicateBeansWhenOnePresentAlready() throws DefinitionException {
    try {
      store.put(classInjectableFactory.create(A.class));
      store.putAll(List.of(classInjectableFactory.create(B.class), classInjectableFactory.create(A.class)));
      fail();
    }
    catch(DuplicateDependencyException e) {
      // expected, check if store is intact:
      assertTrue(store.contains(new Key(A.class)));
      assertFalse(store.contains(new Key(B.class)));
      assertEquals(1, store.resolve(new Key(A.class)).size());
    }
  }

  @Test
  public void containsShouldWork() throws Exception {
    store.put(classInjectableFactory.create(A.class));

    assertTrue(store.contains(new Key(A.class, Set.of(BIG))));
    assertTrue(store.contains(new Key(A.class, Set.of(RED))));
    assertTrue(store.contains(new Key(A.class, Set.of(BIG, RED))));
    assertFalse(store.contains(new Key(B.class, Set.of(BIG, RED))));
    assertFalse(store.contains(new Key(A.class, Set.of(Annotations.of(Small.class), RED))));

    store.put(classInjectableFactory.create(StringProvider.class));

    assertTrue(store.contains(new Key(StringProvider.class)));

    // Ensure lookup by a filtered type is not possible (store doesn't do this, injectable factory filters the types already):
    assertFalse(store.contains(new Key(Types.parameterize(Provider.class, String.class))));
    assertFalse(store.contains(new Key(new TypeLiteral<Provider<String>>() {}.getType())));
    assertFalse(store.contains(new Key(new TypeLiteral<Provider<Long>>() {}.getType())));
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

  interface StringProviderInterface extends Supplier<String> {
  }

  public static class StringProvider implements Supplier<String> {
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

  private static Annotation named(String name) {
    return Annotations.of(Named.class, Map.of("value", name));
  }
}
