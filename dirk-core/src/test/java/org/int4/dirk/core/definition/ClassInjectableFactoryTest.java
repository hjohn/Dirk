package org.int4.dirk.core.definition;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.InjectableFactories;
import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.core.test.scope.Dependent;
import org.int4.dirk.core.test.scope.TestScope;
import org.int4.dirk.core.util.Nullable;
import org.int4.dirk.util.Annotations;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

public class ClassInjectableFactoryTest {
  private final InjectableFactories injectableFactories = new InjectableFactories();
  private final ClassInjectableFactory factory = injectableFactories.forClass();

  @Test
  public void createShouldAcceptValidParameters() throws Exception {
    Injectable<SimpleClass> injectable = factory.create(SimpleClass.class);

    assertEquals(SimpleClass.class, injectable.getType());
    assertEquals(Collections.emptySet(), injectable.getQualifiers());
    assertEquals(Annotations.of(Singleton.class), injectable.getScopeResolver().getAnnotation());
    assertThat(injectable.getInjectionTargets()).hasSize(0);

    Injectable<ClassWithDependencies> injectable2 = factory.create(ClassWithDependencies.class);

    assertEquals(ClassWithDependencies.class, injectable2.getType());
    assertEquals(Collections.singleton(ClassWithDependencies.class.getAnnotation(Red.class)), injectable2.getQualifiers());
    assertEquals(Annotations.of(Dependent.class), injectable2.getScopeResolver().getAnnotation());
    assertThat(injectable2.getInjectionTargets()).hasSize(4);

    ClassWithDependencies instance = injectable2.create(InjectionTargets.resolve(injectable2.getInjectionTargets(), 2, 4L, null, "a string"));

    assertEquals("a string", instance.s);
    assertEquals(2, instance.a);
    assertEquals(4L, instance.b);
    assertNull(instance.bd);

    instance = injectable2.create(InjectionTargets.resolve(injectable2.getInjectionTargets(), 2, 4L, new BigDecimal(5), "a string"));

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
      .hasMessage("[interface org.int4.dirk.core.definition.ClassInjectableFactoryTest$SimpleInterface] cannot be abstract")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectAbstractClassAsInjectableClass() {
    assertThatThrownBy(() -> factory.create(SimpleAbstractClass.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class org.int4.dirk.core.definition.ClassInjectableFactoryTest$SimpleAbstractClass] cannot be abstract")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInjectableClassWithoutConstructors() {
    assertThatThrownBy(() -> factory.create(ClassWithoutPublicConstructors.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class org.int4.dirk.core.definition.ClassInjectableFactoryTest$ClassWithoutPublicConstructors] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInjectableClassWithMultipleAnnotatedConstructors() {
    assertThatThrownBy(() -> factory.create(ClassWithTooManyAnnotatedConstructors.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class org.int4.dirk.core.definition.ClassInjectableFactoryTest$ClassWithTooManyAnnotatedConstructors] cannot have multiple Inject annotated constructors")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInjectableClassWithAnnotatedFinalFields() {
    assertThatThrownBy(() -> factory.create(ClassWithAnnotatedFinalField.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [private final java.lang.String org.int4.dirk.core.definition.ClassInjectableFactoryTest$ClassWithAnnotatedFinalField.a] of [class org.int4.dirk.core.definition.ClassInjectableFactoryTest$ClassWithAnnotatedFinalField] cannot be final")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInjectableClassWithMultipleScopes() {
    assertThatThrownBy(() -> factory.create(ClassWithMultipleScopes.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class org.int4.dirk.core.definition.ClassInjectableFactoryTest$ClassWithMultipleScopes] cannot have multiple scope annotations, but found: [@jakarta.inject.Singleton(), @org.int4.dirk.core.test.scope.TestScope()]")
      .hasNoCause();
  }

  @Test
  public void createShouldRejectInjectableClassWithTypeParameters() {
    assertThatThrownBy(() -> factory.create(GenericClass.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class org.int4.dirk.core.definition.ClassInjectableFactoryTest$GenericClass] cannot have unresolvable type variables: [T]")
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
