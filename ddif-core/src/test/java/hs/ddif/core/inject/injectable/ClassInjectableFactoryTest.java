package hs.ddif.core.inject.injectable;

import hs.ddif.core.config.standard.DefaultInjectable;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.inject.instantiator.Instantiators;
import hs.ddif.core.store.QualifiedTypeStore;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.test.scope.TestScope;
import hs.ddif.core.util.Nullable;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ClassInjectableFactoryTest {
  private final ClassInjectableFactory classInjectableFactory = InjectableFactories.forClass();
  private final InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(DefaultInjectable::new);

  private QualifiedTypeStore<Injectable> store = new QualifiedTypeStore<>();
  private Instantiator instantiator = Instantiators.create(store);

  @BeforeEach
  public void beforeEach() {
    store.put(instanceInjectableFactory.create("a string"));
    store.put(instanceInjectableFactory.create(2));
    store.put(instanceInjectableFactory.create(4L));
  }

  @Test
  public void constructorShouldAcceptValidParameters() throws Exception {
    Injectable injectable = classInjectableFactory.create(SimpleClass.class);

    assertEquals(SimpleClass.class, injectable.getType());
    assertEquals(Collections.emptySet(), injectable.getQualifiers());
    assertEquals(SimpleClass.class.getAnnotation(Singleton.class), injectable.getScope());
    assertThat(injectable.getBindings()).hasSize(0);
    assertTrue(injectable.createInstance(Bindings.resolve(instantiator, injectable.getBindings())) instanceof SimpleClass);

    injectable = classInjectableFactory.create(ClassWithDependencies.class);

    assertEquals(ClassWithDependencies.class, injectable.getType());
    assertEquals(Collections.singleton(ClassWithDependencies.class.getAnnotation(Red.class)), injectable.getQualifiers());
    assertNull(injectable.getScope());
    assertThat(injectable.getBindings()).hasSize(4);

    ClassWithDependencies instance = (ClassWithDependencies)injectable.createInstance(Bindings.resolve(instantiator, injectable.getBindings()));

    assertEquals("a string", instance.s);
    assertEquals(2, instance.a);
    assertEquals(4L, instance.b);
    assertNull(instance.bd);

    store.put(instanceInjectableFactory.create(new BigDecimal(5)));

    instance = (ClassWithDependencies)injectable.createInstance(Bindings.resolve(instantiator, injectable.getBindings()));

    assertEquals(new BigDecimal(5), instance.bd);
  }

  @Test
  public void constructorShouldRejectNullInjectableClass() {
    assertThatThrownBy(() -> classInjectableFactory.create((Type)null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("type cannot be null")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInterfaceAsInjectableClass() {
    assertThatThrownBy(() -> classInjectableFactory.create(SimpleInterface.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[interface hs.ddif.core.inject.injectable.ClassInjectableFactoryTest$SimpleInterface] cannot be injected; failures:\n"
        + " - Type cannot be abstract: interface hs.ddif.core.inject.injectable.ClassInjectableFactoryTest$SimpleInterface"
      )
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectAbstractClassAsInjectableClass() {
    assertThatThrownBy(() -> classInjectableFactory.create(SimpleAbstractClass.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.inject.injectable.ClassInjectableFactoryTest$SimpleAbstractClass] cannot be injected; failures:\n"
        + " - Type cannot be abstract: class hs.ddif.core.inject.injectable.ClassInjectableFactoryTest$SimpleAbstractClass"
      )
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithoutConstructors() {
    assertThatThrownBy(() -> classInjectableFactory.create(ClassWithoutPublicConstructors.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.inject.injectable.ClassInjectableFactoryTest$ClassWithoutPublicConstructors] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithMultipleAnnotatedConstructors() {
    assertThatThrownBy(() -> classInjectableFactory.create(ClassWithTooManyAnnotatedConstructors.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.inject.injectable.ClassInjectableFactoryTest$ClassWithTooManyAnnotatedConstructors] cannot have multiple Inject annotated constructors")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithAnnotatedFinalFields() {
    assertThatThrownBy(() -> classInjectableFactory.create(ClassWithAnnotatedFinalField.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [private final java.lang.String hs.ddif.core.inject.injectable.ClassInjectableFactoryTest$ClassWithAnnotatedFinalField.a] of [class hs.ddif.core.inject.injectable.ClassInjectableFactoryTest$ClassWithAnnotatedFinalField] cannot be final")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithMultipleScopes() {
    assertThatThrownBy(() -> classInjectableFactory.create(ClassWithMultipleScopes.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.inject.injectable.ClassInjectableFactoryTest$ClassWithMultipleScopes] cannot have multiple scope annotations, but found: [@hs.ddif.core.test.scope.TestScope(), @javax.inject.Singleton()]")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithTypeParameters() {
    assertThatThrownBy(() -> classInjectableFactory.create(GenericClass.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.inject.injectable.ClassInjectableFactoryTest$GenericClass] cannot have unresolvable type variables: [T]")
      .hasNoCause();
  }

  @Singleton
  public static class SimpleClass {
  }

  @Red
  public static class ClassWithDependencies {
    @Inject String s;

    int a;
    long b;
    BigDecimal bd;

    @Inject
    public ClassWithDependencies(Integer a, Long b, @Nullable BigDecimal bd) {
      this.a = a;
      this.b = b;
      this.bd = bd;
    }
  }

  public static interface SimpleInterface {
  }

  public static abstract class SimpleAbstractClass {
  }

  private static class ClassWithoutPublicConstructors {
  }

  @SuppressWarnings("unused")
  public static class ClassWithTooManyAnnotatedConstructors {
    @Inject
    public ClassWithTooManyAnnotatedConstructors(String a) {
    }

    @Inject
    public ClassWithTooManyAnnotatedConstructors(String b, String c) {
    }
  }

  public static class ClassWithAnnotatedFinalField {
    @SuppressWarnings("unused")
    @Inject private final String a = "";
  }

  @TestScope
  @Singleton
  public static class ClassWithMultipleScopes {
  }

  public static class GenericClass<T> {
    T t;
  }
}
