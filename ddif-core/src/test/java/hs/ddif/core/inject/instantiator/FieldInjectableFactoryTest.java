package hs.ddif.core.inject.instantiator;

import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.FieldInjectableFactory;
import hs.ddif.core.scope.OutOfScopeException;
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
  private final FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(ResolvableInjectable::new);

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
  void shouldReturnCorrectInjectableForNonStaticField() throws NoSuchFieldException, SecurityException, InstanceCreationFailure, InstanceCreationFailure, NoSuchInstance, MultipleInstances, OutOfScopeException {
    ResolvableInjectable injectable = fieldInjectableFactory.create(C.class.getField("b"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Declaring Class of [public java.lang.Object hs.ddif.core.inject.instantiator.FieldInjectableFactoryTest$B.b]"
    );

    when(instantiator.getInstance(new Key(C.class))).thenReturn(new C());

    assertEquals("Bye", injectable.getObjectFactory().createInstance(instantiator));
  }

  @Test
  void shouldReturnCorrectInjectableForStaticField() throws NoSuchFieldException, SecurityException, InstanceCreationFailure {
    ResolvableInjectable injectable = fieldInjectableFactory.create(C.class.getField("e"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).isEmpty();

    assertEquals("Hello", injectable.getObjectFactory().createInstance(instantiator));
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
