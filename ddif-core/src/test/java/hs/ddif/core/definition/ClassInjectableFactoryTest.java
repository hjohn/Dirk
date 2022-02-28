package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.BindingException;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.test.scope.TestScope;
import hs.ddif.core.util.Annotations;
import hs.ddif.core.util.Nullable;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClassInjectableFactoryTest {
  private final InjectableFactories injectableFactories = new InjectableFactories();
  private final ClassInjectableFactory factory = injectableFactories.forClass();

  @Test
  public void createShouldAcceptValidParameters() throws Exception {
    Injectable injectable = factory.create(SimpleClass.class);

    assertEquals(SimpleClass.class, injectable.getType());
    assertEquals(Collections.emptySet(), injectable.getQualifiers());
    assertEquals(injectableFactories.getScopeResolver(Annotations.of(Singleton.class)), injectable.getScopeResolver());
    assertThat(injectable.getBindings()).hasSize(0);
    assertTrue(injectable.createInstance(Bindings.resolve(injectable.getBindings())) instanceof SimpleClass);

    injectable = factory.create(ClassWithDependencies.class);

    assertEquals(ClassWithDependencies.class, injectable.getType());
    assertEquals(Collections.singleton(ClassWithDependencies.class.getAnnotation(Red.class)), injectable.getQualifiers());
    assertEquals(injectableFactories.getScopeResolver(null), injectable.getScopeResolver());
    assertThat(injectable.getBindings()).hasSize(4);

    ClassWithDependencies instance = (ClassWithDependencies)injectable.createInstance(Bindings.resolve(injectable.getBindings(), 2, 4L, null, "a string"));

    assertEquals("a string", instance.s);
    assertEquals(2, instance.a);
    assertEquals(4L, instance.b);
    assertNull(instance.bd);

    instance = (ClassWithDependencies)injectable.createInstance(Bindings.resolve(injectable.getBindings(), 2, 4L, new BigDecimal(5), "a string"));

    assertEquals(new BigDecimal(5), instance.bd);
  }

  @Test
  public void createShouldRejectNullInjectableClass() {
    assertThatThrownBy(() -> factory.create((Type)null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("type cannot be null")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInterfaceAsInjectableClass() {
    assertThatThrownBy(() -> factory.create(SimpleInterface.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[interface hs.ddif.core.definition.ClassInjectableFactoryTest$SimpleInterface] cannot be abstract")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectAbstractClassAsInjectableClass() {
    assertThatThrownBy(() -> factory.create(SimpleAbstractClass.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.definition.ClassInjectableFactoryTest$SimpleAbstractClass] cannot be abstract")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInjectableClassWithoutConstructors() {
    assertThatThrownBy(() -> factory.create(ClassWithoutPublicConstructors.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.definition.ClassInjectableFactoryTest$ClassWithoutPublicConstructors] could not be bound")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("[class hs.ddif.core.definition.ClassInjectableFactoryTest$ClassWithoutPublicConstructors] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInjectableClassWithMultipleAnnotatedConstructors() {
    assertThatThrownBy(() -> factory.create(ClassWithTooManyAnnotatedConstructors.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.definition.ClassInjectableFactoryTest$ClassWithTooManyAnnotatedConstructors] could not be bound")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("[class hs.ddif.core.definition.ClassInjectableFactoryTest$ClassWithTooManyAnnotatedConstructors] cannot have multiple Inject annotated constructors")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInjectableClassWithAnnotatedFinalFields() {
    assertThatThrownBy(() -> factory.create(ClassWithAnnotatedFinalField.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.definition.ClassInjectableFactoryTest$ClassWithAnnotatedFinalField] could not be bound")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Field [private final java.lang.String hs.ddif.core.definition.ClassInjectableFactoryTest$ClassWithAnnotatedFinalField.a] of [class hs.ddif.core.definition.ClassInjectableFactoryTest$ClassWithAnnotatedFinalField] cannot be final")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInjectableClassWithMultipleScopes() {
    assertThatThrownBy(() -> factory.create(ClassWithMultipleScopes.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.definition.ClassInjectableFactoryTest$ClassWithMultipleScopes] cannot have multiple scope annotations, but found: [@hs.ddif.core.test.scope.TestScope(), @javax.inject.Singleton()]")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInjectableClassWithTypeParameters() {
    assertThatThrownBy(() -> factory.create(GenericClass.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.definition.ClassInjectableFactoryTest$GenericClass] cannot have unresolvable type variables: [T]")
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
