package hs.ddif.core.inject.injectable;

import hs.ddif.core.config.standard.DefaultBinding;
import hs.ddif.core.inject.bind.BindingException;
import hs.ddif.core.inject.bind.BindingProvider;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.store.Key;

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
public class FieldInjectableFactoryTest {
  private final BindingProvider bindingProvider = new BindingProvider(DefaultBinding::new);
  private final FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, ResolvableInjectable::new);

  @Mock private Instantiator instantiator;

  @Test
  void constructorShouldRejectNullField() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> fieldInjectableFactory.create(null, A.class)))
      .hasMessage("field cannot be null");
  }

  @Test
  void constructorShouldRejectNullOwnerType() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> fieldInjectableFactory.create(A.class.getDeclaredField("a"), null)))
      .hasMessage("ownerType cannot be null");
  }

  @Test
  void constructorShouldRejectIncompatibleOwnerType() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> fieldInjectableFactory.create(A.class.getDeclaredField("a"), B.class)))
      .hasMessageStartingWith("ownerType must be assignable to field's declaring class:");
  }

  @Test
  void constructorShouldRejectFieldWithUnresolvableReturnType() {
    assertThat(assertThrows(BindingException.class, () -> fieldInjectableFactory.create(B.class.getDeclaredField("b"), B.class)))
      .hasMessageStartingWith("Field has unresolved type:");
  }

  @Test
  void constructorShouldRejectFieldWithUnresolvableTypeVariables() {
    assertThat(assertThrows(BindingException.class, () -> fieldInjectableFactory.create(B.class.getDeclaredField("d"), B.class)))
      .hasMessageStartingWith("Field has unresolved type variables:");
  }

  @Test
  void constructorShouldRejectFieldAnnotatedWithInject() {
    assertThat(assertThrows(BindingException.class, () -> fieldInjectableFactory.create(A.class.getDeclaredField("c"), A.class)))
      .hasMessageStartingWith("Field cannot be annotated with Inject:");
  }

  @Test
  void constructorShouldAcceptValidParameters() throws NoSuchFieldException, SecurityException {
    ResolvableInjectable injectable = fieldInjectableFactory.create(C.class.getField("b"), C.class);

    assertEquals(String.class, injectable.getType());
  }

  @Test
  void shouldReturnCorrectInjectableForNonStaticField() throws Exception {
    ResolvableInjectable injectable = fieldInjectableFactory.create(C.class.getField("b"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Owner Type [class hs.ddif.core.inject.injectable.FieldInjectableFactoryTest$C]"
    );

    when(instantiator.getInstance(new Key(C.class))).thenReturn(new C());

    assertEquals("Bye", injectable.getObjectFactory().createInstance(Bindings.resolve(instantiator, injectable.getBindings())));
  }

  @Test
  void shouldReturnCorrectInjectableForStaticField() throws Exception {
    ResolvableInjectable injectable = fieldInjectableFactory.create(C.class.getField("e"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).isEmpty();

    assertEquals("Hello", injectable.getObjectFactory().createInstance(Bindings.resolve(instantiator, injectable.getBindings())));
  }

  static class A {
    String a;
    @Inject String c;
  }

  static class B<T> {
    @SuppressWarnings("unchecked")
    public T b = (T)"Bye";
    public List<T> d;
  }

  static class C extends B<String> {
    public static String e = "Hello";
  }
}
