package hs.ddif.core;

import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.Instantiators;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.inject.store.InstanceInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.AnnotationDescriptor;
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

public class ClassInjectableTest {
  private InjectableStore<ResolvableInjectable> store = new InjectableStore<>();
  private Instantiator instantiator = Instantiators.create(store);

  @BeforeEach
  public void beforeEach() {
    store.put(new InstanceInjectable("a string"));
    store.put(new InstanceInjectable(2));
    store.put(new InstanceInjectable(4L));
  }

  @Test
  public void constructorShouldAcceptValidParameters() throws InstanceCreationFailure {
    ClassInjectable injectable = new ClassInjectable(SimpleClass.class);

    assertEquals(SimpleClass.class, injectable.getType());
    assertEquals(Collections.emptySet(), injectable.getQualifiers());
    assertEquals(SimpleClass.class.getAnnotation(Singleton.class), injectable.getScope());
    assertThat(injectable.getBindings()).hasSize(0);
    assertTrue(injectable.createInstance(instantiator) instanceof SimpleClass);

    injectable = new ClassInjectable(ClassWithDependencies.class);

    assertEquals(ClassWithDependencies.class, injectable.getType());
    assertEquals(Collections.singleton(new AnnotationDescriptor(ClassWithDependencies.class.getAnnotation(Red.class))), injectable.getQualifiers());
    assertNull(injectable.getScope());
    assertThat(injectable.getBindings()).hasSize(4);

    ClassWithDependencies instance = (ClassWithDependencies)injectable.createInstance(instantiator);

    assertEquals("a string", instance.s);
    assertEquals(2, instance.a);
    assertEquals(4L, instance.b);
    assertNull(instance.bd);

    store.put(new InstanceInjectable(new BigDecimal(5)));

    instance = (ClassWithDependencies)injectable.createInstance(instantiator);

    assertEquals(new BigDecimal(5), instance.bd);
  }

  @Test
  public void constructorShouldRejectNullInjectableClass() {
    assertThatThrownBy(() -> new ClassInjectable((Type)null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("injectableType cannot be null")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInterfaceAsInjectableClass() {
    assertThatThrownBy(() -> new ClassInjectable(SimpleInterface.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Type cannot be abstract: interface hs.ddif.core.ClassInjectableTest$SimpleInterface")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectAbstractClassAsInjectableClass() {
    assertThatThrownBy(() -> new ClassInjectable(SimpleAbstractClass.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Type cannot be abstract: class hs.ddif.core.ClassInjectableTest$SimpleAbstractClass")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithoutConstructors() {
    assertThatThrownBy(() -> new ClassInjectable(ClassWithoutPublicConstructors.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.ClassInjectableTest$ClassWithoutPublicConstructors")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithMultipleAnnotatedConstructors() {
    assertThatThrownBy(() -> new ClassInjectable(ClassWithTooManyAnnotatedConstructors.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Multiple @Inject annotated constructors found, but only one allowed: class hs.ddif.core.ClassInjectableTest$ClassWithTooManyAnnotatedConstructors")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithAnnotatedFinalFields() {
    assertThatThrownBy(() -> new ClassInjectable(ClassWithAnnotatedFinalField.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Cannot inject final field: private final java.lang.String hs.ddif.core.ClassInjectableTest$ClassWithAnnotatedFinalField.a in: class hs.ddif.core.ClassInjectableTest$ClassWithAnnotatedFinalField")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithMultipleScopes() {
    assertThatThrownBy(() -> new ClassInjectable(ClassWithMultipleScopes.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Multiple scope annotations found, but only one allowed: class hs.ddif.core.ClassInjectableTest$ClassWithMultipleScopes, found: [@hs.ddif.core.TestScope(), @javax.inject.Singleton()]")
      .hasNoCause();
  }

  @Test
  public void constructorShouldRejectInjectableClassWithTypeParameters() {
    assertThatThrownBy(() -> new ClassInjectable(GenericClass.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Unresolved type variables in class hs.ddif.core.ClassInjectableTest$GenericClass are not allowed: [T]")
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
  }
}
