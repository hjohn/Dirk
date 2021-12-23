package hs.ddif.core.inject.instantiator;

import hs.ddif.core.TestScope;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.inject.store.InstanceInjectableFactory;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.test.qualifiers.Red;
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
  private final ClassInjectableFactory classInjectableFactory = new ClassInjectableFactory(ResolvableInjectable::new);
  private final InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(ResolvableInjectable::new);

  private InjectableStore<ResolvableInjectable> store = new InjectableStore<>();
  private Instantiator instantiator = Instantiators.create(store);

  @BeforeEach
  public void beforeEach() {
    store.put(instanceInjectableFactory.create("a string"));
    store.put(instanceInjectableFactory.create(2));
    store.put(instanceInjectableFactory.create(4L));
  }

  @Test
  public void constructorShouldAcceptValidParameters() throws InstanceCreationFailure {
    ResolvableInjectable injectable = classInjectableFactory.create(SimpleClass.class);

    assertEquals(SimpleClass.class, injectable.getType());
    assertEquals(Collections.emptySet(), injectable.getQualifiers());
    assertEquals(SimpleClass.class.getAnnotation(Singleton.class), injectable.getScope());
    assertThat(injectable.getBindings()).hasSize(0);
    assertTrue(injectable.getObjectFactory().createInstance(instantiator) instanceof SimpleClass);

    injectable = classInjectableFactory.create(ClassWithDependencies.class);

    assertEquals(ClassWithDependencies.class, injectable.getType());
    assertEquals(Collections.singleton(ClassWithDependencies.class.getAnnotation(Red.class)), injectable.getQualifiers());
    assertNull(injectable.getScope());
    assertThat(injectable.getBindings()).hasSize(4);

    ClassWithDependencies instance = (ClassWithDependencies)injectable.getObjectFactory().createInstance(instantiator);

    assertEquals("a string", instance.s);
    assertEquals(2, instance.a);
    assertEquals(4L, instance.b);
    assertNull(instance.bd);

    store.put(instanceInjectableFactory.create(new BigDecimal(5)));

    instance = (ClassWithDependencies)injectable.getObjectFactory().createInstance(instantiator);

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
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Type cannot be abstract: interface hs.ddif.core.inject.instantiator.ClassInjectableFactoryTest$SimpleInterface")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectAbstractClassAsInjectableClass() {
    assertThatThrownBy(() -> classInjectableFactory.create(SimpleAbstractClass.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Type cannot be abstract: class hs.ddif.core.inject.instantiator.ClassInjectableFactoryTest$SimpleAbstractClass")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithoutConstructors() {
    assertThatThrownBy(() -> classInjectableFactory.create(ClassWithoutPublicConstructors.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.inject.instantiator.ClassInjectableFactoryTest$ClassWithoutPublicConstructors")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithMultipleAnnotatedConstructors() {
    assertThatThrownBy(() -> classInjectableFactory.create(ClassWithTooManyAnnotatedConstructors.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Multiple @Inject annotated constructors found, but only one allowed: class hs.ddif.core.inject.instantiator.ClassInjectableFactoryTest$ClassWithTooManyAnnotatedConstructors")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithAnnotatedFinalFields() {
    assertThatThrownBy(() -> classInjectableFactory.create(ClassWithAnnotatedFinalField.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Cannot inject final field: private final java.lang.String hs.ddif.core.inject.instantiator.ClassInjectableFactoryTest$ClassWithAnnotatedFinalField.a in: class hs.ddif.core.inject.instantiator.ClassInjectableFactoryTest$ClassWithAnnotatedFinalField")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithMultipleScopes() {
    assertThatThrownBy(() -> classInjectableFactory.create(ClassWithMultipleScopes.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Multiple scope annotations found, but only one allowed: class hs.ddif.core.inject.instantiator.ClassInjectableFactoryTest$ClassWithMultipleScopes, found: [@hs.ddif.core.TestScope(), @javax.inject.Singleton()]")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithTypeParameters() {
    assertThatThrownBy(() -> classInjectableFactory.create(GenericClass.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Unresolved type variables in class hs.ddif.core.inject.instantiator.ClassInjectableFactoryTest$GenericClass are not allowed: [T]")
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
