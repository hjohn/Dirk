package hs.ddif.core.inject.store;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MethodInjectableTest {

  @Test
  void constructorShouldRejectNullMethod() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> new MethodInjectable(null, A.class)))
      .hasMessage("method cannot be null");
  }

  @Test
  void constructorShouldRejectNullOwnerType() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> new MethodInjectable(A.class.getDeclaredMethod("a"), null)))
      .hasMessage("ownerType cannot be null");
  }

  @Test
  void constructorShouldRejectIncompatibleOwnerType() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> new MethodInjectable(A.class.getDeclaredMethod("a"), B.class)))
      .hasMessageStartingWith("ownerType must be assignable to method's declaring class:");
  }

  @Test
  void constructorShouldRejectMethodWithUnresolvableReturnType() {
    assertThat(assertThrows(BindingException.class, () -> new MethodInjectable(B.class.getDeclaredMethod("b"), B.class)))
      .hasMessageStartingWith("Method has unresolved return type:");
  }

  @Test
  void constructorShouldRejectMethodWithUnresolvableTypeVariables() {
    assertThat(assertThrows(BindingException.class, () -> new MethodInjectable(B.class.getDeclaredMethod("d"), B.class)))
      .hasMessageStartingWith("Method has unresolved type variables:");
  }

  @Test
  void constructorShouldRejectVoidMethod() {
    assertThat(assertThrows(BindingException.class, () -> new MethodInjectable(A.class.getDeclaredMethod("a"), A.class)))
      .hasMessageStartingWith("Method has no return type:");
  }

  @Test
  void constructorShouldRejectMethodAnnotatedWithInject() {
    assertThat(assertThrows(BindingException.class, () -> new MethodInjectable(A.class.getDeclaredMethod("c"), A.class)))
      .hasMessageStartingWith("Method cannot be annotated with Inject:");
  }

  @Test
  void constructorShouldAcceptValidParameters() throws NoSuchMethodException, SecurityException {
    MethodInjectable injectable = new MethodInjectable(C.class.getMethod("b"), C.class);

    assertEquals(String.class, injectable.getType());
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
    public T b() {
      return null;
    }

    public List<T> d() {
      return null;
    }
  }

  static class C extends B<String> {
  }
}
