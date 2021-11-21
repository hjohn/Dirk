package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.MultipleInstances;
import hs.ddif.core.inject.instantiator.NoSuchInstance;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MethodInjectableTest {
  @Mock private Instantiator instantiator;

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

  @Test
  void shouldReturnCorrectInjectableForNonStaticMethod() throws NoSuchMethodException, SecurityException, InstanceCreationFailure, InstanceCreationFailure, NoSuchInstance, MultipleInstances {
    MethodInjectable injectable = new MethodInjectable(C.class.getMethod("b"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Declaring Class of [public java.lang.Object hs.ddif.core.inject.store.MethodInjectableTest$B.b()]"
    );

    when(instantiator.getInstance((Type)C.class)).thenReturn(new C());

    assertEquals("Bye", injectable.createInstance(instantiator));
  }

  @Test
  void shouldReturnCorrectInjectableForStaticMethod() throws NoSuchMethodException, SecurityException, InstanceCreationFailure, InstanceCreationFailure, NoSuchInstance, MultipleInstances {
    MethodInjectable injectable = new MethodInjectable(C.class.getMethod("e", D.class), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Parameter 0 of [public static java.lang.String hs.ddif.core.inject.store.MethodInjectableTest$C.e(hs.ddif.core.inject.store.MethodInjectableTest$D)]"
    );

    when(instantiator.getInstance((Type)D.class)).thenReturn(new D());

    assertEquals("Hello D", injectable.createInstance(instantiator));
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

    public List<T> d() {
      return new ArrayList<>();
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
