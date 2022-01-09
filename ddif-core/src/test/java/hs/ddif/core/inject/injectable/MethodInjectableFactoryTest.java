package hs.ddif.core.inject.injectable;

import hs.ddif.core.config.standard.DefaultBinding;
import hs.ddif.core.config.standard.DefaultInjectable;
import hs.ddif.core.inject.bind.BindingException;
import hs.ddif.core.inject.bind.BindingProvider;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.store.Injectable;
import hs.ddif.core.store.Key;

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
public class MethodInjectableFactoryTest {
  private final BindingProvider bindingProvider = new BindingProvider(DefaultBinding::new);
  private final MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, DefaultInjectable::new);

  @Mock private Instantiator instantiator;

  @Test
  void constructorShouldRejectNullMethod() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> methodInjectableFactory.create(null, A.class)))
      .hasMessage("method cannot be null");
  }

  @Test
  void constructorShouldRejectNullOwnerType() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> methodInjectableFactory.create(A.class.getDeclaredMethod("a"), null)))
      .hasMessage("ownerType cannot be null");
  }

  @Test
  void constructorShouldRejectIncompatibleOwnerType() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> methodInjectableFactory.create(A.class.getDeclaredMethod("a"), B.class)))
      .hasMessageStartingWith("ownerType must be assignable to method's declaring class:");
  }

  @Test
  void constructorShouldRejectMethodWithUnresolvableReturnType() {
    assertThat(assertThrows(BindingException.class, () -> methodInjectableFactory.create(B.class.getDeclaredMethod("b"), B.class)))
      .hasMessageStartingWith("Method has unresolved return type:");
  }

  @Test
  void constructorShouldRejectMethodWithUnresolvableTypeVariables() {
    assertThat(assertThrows(BindingException.class, () -> methodInjectableFactory.create(B.class.getDeclaredMethod("d"), B.class)))
      .hasMessageStartingWith("Method has unresolved type variables:");
  }

  @Test
  void constructorShouldRejectVoidMethod() {
    assertThat(assertThrows(BindingException.class, () -> methodInjectableFactory.create(A.class.getDeclaredMethod("a"), A.class)))
      .hasMessageStartingWith("Method has no return type:");
  }

  @Test
  void constructorShouldRejectMethodAnnotatedWithInject() {
    assertThat(assertThrows(BindingException.class, () -> methodInjectableFactory.create(A.class.getDeclaredMethod("c"), A.class)))
      .hasMessageStartingWith("Method cannot be annotated with Inject:");
  }

  @Test
  void constructorShouldAcceptValidParameters() throws NoSuchMethodException, SecurityException {
    Injectable injectable = methodInjectableFactory.create(C.class.getMethod("b"), C.class);

    assertEquals(String.class, injectable.getType());
  }

  @Test
  void shouldReturnCorrectInjectableForNonStaticMethod() throws Exception {
    ResolvableInjectable injectable = methodInjectableFactory.create(C.class.getMethod("b"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Owner Type [class hs.ddif.core.inject.injectable.MethodInjectableFactoryTest$C]"
    );

    when(instantiator.getInstance(new Key(C.class))).thenReturn(new C());

    assertEquals("Bye", injectable.createInstance(Bindings.resolve(instantiator, injectable.getBindings())));
  }

  @Test
  void shouldReturnCorrectInjectableForStaticMethod() throws Exception {
    ResolvableInjectable injectable = methodInjectableFactory.create(C.class.getMethod("e", D.class), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Parameter 0 of [public static java.lang.String hs.ddif.core.inject.injectable.MethodInjectableFactoryTest$C.e(hs.ddif.core.inject.injectable.MethodInjectableFactoryTest$D)]"
    );

    when(instantiator.getInstance(new Key(D.class))).thenReturn(new D());

    assertEquals("Hello D", injectable.createInstance(Bindings.resolve(instantiator, injectable.getBindings())));
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