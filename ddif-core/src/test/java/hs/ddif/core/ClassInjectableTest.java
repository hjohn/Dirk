package hs.ddif.core;

import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.Nullable;

import java.math.BigDecimal;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ClassInjectableTest {
  private Injector injector = new Injector();
  private Instantiator instantiator = injector.getInstantiator();

  @Before
  public void before() {
    injector.registerInstance("a string");
    injector.registerInstance(2);
    injector.registerInstance(4L);
  }

  @Test
  public void constructorShouldAcceptValidParameters() {
    ClassInjectable injectable = new ClassInjectable(SimpleClass.class);

    assertEquals(SimpleClass.class, injectable.getInjectableClass());
    assertEquals(Collections.emptySet(), injectable.getQualifiers());
    assertEquals(SimpleClass.class.getAnnotation(Singleton.class), injectable.getScope());
    assertEquals(1, injectable.getBindings().size());
    assertTrue(injectable.getInstance(instantiator) instanceof SimpleClass);

    injectable = new ClassInjectable(ClassWithDependencies.class);

    assertEquals(ClassWithDependencies.class, injectable.getInjectableClass());
    assertEquals(Collections.singleton(new AnnotationDescriptor(ClassWithDependencies.class.getAnnotation(Red.class))), injectable.getQualifiers());
    assertNull(injectable.getScope());
    assertEquals(2, injectable.getBindings().size());

    ClassWithDependencies instance = (ClassWithDependencies)injectable.getInstance(instantiator);

    assertEquals("a string", instance.s);
    assertEquals(2, instance.a);
    assertEquals(4L, instance.b);
    assertNull(instance.bd);

    injector.registerInstance(new BigDecimal(5));

    instance = (ClassWithDependencies)injectable.getInstance(instantiator);

    assertEquals(new BigDecimal(5), instance.bd);
  }

  @Test(expected = IllegalArgumentException.class) @SuppressWarnings("unused")
  public void constructorShouldRejectNullInjectableClass() {
    new ClassInjectable(null);
  }

  @Test(expected = IllegalArgumentException.class) @SuppressWarnings("unused")
  public void constructorShouldRejectInterfaceAsInjectableClass() {
    new ClassInjectable(SimpleInterface.class);
  }

  @Test(expected = IllegalArgumentException.class) @SuppressWarnings("unused")
  public void constructorShouldRejectAbstractClassAsInjectableClass() {
    new ClassInjectable(SimpleAbstractClass.class);
  }

  @Test(expected = BindingException.class) @SuppressWarnings("unused")
  public void constructorShouldRejectInjectableClassWithoutConstructors() {
    new ClassInjectable(ClassWithoutPublicConstructors.class);
  }

  @Test(expected = BindingException.class) @SuppressWarnings("unused")
  public void constructorShouldRejectInjectableClassWithMultipleAnnotatedConstructors() {
    new ClassInjectable(ClassWithTooManyAnnotatedConstructors.class);
  }

  @Test(expected = BindingException.class) @SuppressWarnings("unused")
  public void constructorShouldRejectInjectableClassWithAnnotatedFinalFields() {
    new ClassInjectable(ClassWithAnnotatedFinalField.class);
  }

  @Test(expected = BindingException.class) @SuppressWarnings("unused")
  public void constructorShouldRejectInjectableClassWithMultipleScopes() {
    new ClassInjectable(ClassWithMultipleScopes.class);
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
}
