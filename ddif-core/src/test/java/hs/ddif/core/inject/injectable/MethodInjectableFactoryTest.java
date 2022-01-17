package hs.ddif.core.inject.injectable;

import hs.ddif.core.config.standard.DefaultBinding;
import hs.ddif.core.config.standard.DefaultInjectable;
import hs.ddif.core.inject.bind.BindingProvider;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.store.Key;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MethodInjectableFactoryTest {
  private final BindingProvider bindingProvider = new BindingProvider(DefaultBinding::new);
  private final MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, DefaultInjectable::new);

  @Mock private Instantiator instantiator;

  @Test
  void constructorShouldRejectNullMethod() {
    assertThatThrownBy(() -> methodInjectableFactory.create(null, A.class))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("method cannot be null")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectNullOwnerType() {
    assertThatThrownBy(() -> methodInjectableFactory.create(A.class.getDeclaredMethod("a"), null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType cannot be null")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectIncompatibleOwnerType() {
    assertThatThrownBy(() -> methodInjectableFactory.create(A.class.getDeclaredMethod("a"), B.class))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType must be assignable to method's declaring class: class hs.ddif.core.inject.injectable.MethodInjectableFactoryTest$B; declaring class: class hs.ddif.core.inject.injectable.MethodInjectableFactoryTest$A")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectMethodWithUnresolvableReturnType() {
    assertThatThrownBy(() -> methodInjectableFactory.create(B.class.getDeclaredMethod("b"), B.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public java.lang.Object hs.ddif.core.inject.injectable.MethodInjectableFactoryTest$B.b()] has unresolvable return type")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectMethodWithUnresolvableTypeVariables() {
    assertThatThrownBy(() -> methodInjectableFactory.create(B.class.getDeclaredMethod("d"), B.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public java.util.List hs.ddif.core.inject.injectable.MethodInjectableFactoryTest$B.d()] has unresolvable type variables")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectVoidMethod() {
    assertThatThrownBy(() -> methodInjectableFactory.create(A.class.getDeclaredMethod("a"), A.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [void hs.ddif.core.inject.injectable.MethodInjectableFactoryTest$A.a()] has no return type")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectMethodAnnotatedWithInject() {
    assertThatThrownBy(() -> methodInjectableFactory.create(A.class.getDeclaredMethod("c"), A.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [java.lang.String hs.ddif.core.inject.injectable.MethodInjectableFactoryTest$A.c()] cannot be annotated with Inject")
      .hasNoCause();
  }

  @Test
  void constructorShouldAcceptValidParameters() throws NoSuchMethodException, SecurityException {
    Injectable injectable = methodInjectableFactory.create(C.class.getMethod("b"), C.class);

    assertEquals(String.class, injectable.getType());
  }

  @Test
  void shouldReturnCorrectInjectableForNonStaticMethod() throws Exception {
    Injectable injectable = methodInjectableFactory.create(C.class.getMethod("b"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Owner Type [class hs.ddif.core.inject.injectable.MethodInjectableFactoryTest$C]"
    );

    when(instantiator.getInstance(new Key(C.class))).thenReturn(new C());

    assertEquals("Bye", injectable.createInstance(Bindings.resolve(instantiator, injectable.getBindings())));
  }

  @Test
  void shouldReturnCorrectInjectableForStaticMethod() throws Exception {
    Injectable injectable = methodInjectableFactory.create(C.class.getMethod("e", D.class), C.class);

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
