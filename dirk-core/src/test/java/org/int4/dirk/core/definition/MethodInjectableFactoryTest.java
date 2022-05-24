package org.int4.dirk.core.definition;

import java.util.ArrayDeque;
import java.util.Queue;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.InjectableFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

public class MethodInjectableFactoryTest {
  private final MethodInjectableFactory factory = new InjectableFactories().forMethod();

  @Test
  void createShouldRejectNullMethod() {
    assertThatThrownBy(() -> factory.create(null, A.class))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("method cannot be null")
      .hasNoCause();
  }

  @Test
  void createShouldRejectNullOwnerType() {
    assertThatThrownBy(() -> factory.create(A.class.getDeclaredMethod("a"), null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType cannot be null")
      .hasNoCause();
  }

  @Test
  void createShouldRejectIncompatibleOwnerType() {
    assertThatThrownBy(() -> factory.create(A.class.getDeclaredMethod("a"), B.class))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType must be assignable to declaring class: class org.int4.dirk.core.definition.MethodInjectableFactoryTest$B; declaring class: class org.int4.dirk.core.definition.MethodInjectableFactoryTest$A")
      .hasNoCause();
  }

  @Test
  void createShouldRejectMethodWithUnresolvableReturnType() {
    assertThatThrownBy(() -> factory.create(B.class.getDeclaredMethod("b"), B.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public java.lang.Object org.int4.dirk.core.definition.MethodInjectableFactoryTest$B.b()] has unresolvable return type")
      .hasNoCause();
  }

  @Test
  void createShouldRejectMethodWithUnresolvableTypeVariables() {
    assertThatThrownBy(() -> factory.create(B.class.getDeclaredMethod("d"), B.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public java.util.Queue org.int4.dirk.core.definition.MethodInjectableFactoryTest$B.d()] has unsuitable type")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[java.util.Queue<T>] cannot have unresolvable type variables or wild cards")
      .hasNoCause();
  }

  @Test
  void createShouldRejectVoidMethod() {
    assertThatThrownBy(() -> factory.create(A.class.getDeclaredMethod("a"), A.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [void org.int4.dirk.core.definition.MethodInjectableFactoryTest$A.a()] has unsuitable type")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[java.lang.Void] cannot be void or Void")
      .hasNoCause();
  }

  @Test
  void createShouldRejectMethodAnnotatedWithInject() {
    assertThatThrownBy(() -> factory.create(A.class.getDeclaredMethod("c"), A.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [java.lang.String org.int4.dirk.core.definition.MethodInjectableFactoryTest$A.c()] should not have an inject annotation, but found: [@jakarta.inject.Inject()]")
      .hasNoCause();
  }

  @Test
  void createShouldAcceptValidParameters() throws Exception {
    Injectable<String> injectable = factory.create(C.class.getMethod("b"), C.class);

    assertEquals(String.class, injectable.getType());
  }

  @Test
  void createShouldReturnCorrectInjectableForNonStaticMethod() throws Exception {
    Injectable<String> injectable = factory.create(C.class.getMethod("b"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Owner Type [class org.int4.dirk.core.definition.MethodInjectableFactoryTest$C]"
    );

    assertEquals("Bye", injectable.create(Bindings.resolve(injectable.getBindings(), new C())));
  }

  @Test
  void createShouldReturnCorrectInjectableForStaticMethodWithOneParameter() throws Exception {
    Injectable<String> injectable = factory.create(C.class.getMethod("e", D.class), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Parameter 0 [class org.int4.dirk.core.definition.MethodInjectableFactoryTest$D] of [public static java.lang.String org.int4.dirk.core.definition.MethodInjectableFactoryTest$C.e(org.int4.dirk.core.definition.MethodInjectableFactoryTest$D)]"
    );

    assertEquals("Hello D", injectable.create(Bindings.resolve(injectable.getBindings(), new D())));
  }

  static class A {
    void a() {
    }

    @Inject
    String c() {
      return "";
    }
  }

  static class B<T> {
    @SuppressWarnings("unchecked")
    public T b() {
      return (T)"Bye";
    }

    public Queue<T> d() {
      return new ArrayDeque<>();
    }
  }

  static class C extends B<String> {
    public static String e(D d) {
      return "Hello " + d.getClass().getSimpleName();
    }
  }

  static class D {
  }
}
