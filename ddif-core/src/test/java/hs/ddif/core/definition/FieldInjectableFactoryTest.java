package hs.ddif.core.definition;

import hs.ddif.core.config.standard.DefaultInjectable;
import hs.ddif.core.definition.bind.BindingProvider;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldInjectableFactoryTest {
  private final BindingProvider bindingProvider = new BindingProvider();
  private final FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, DefaultInjectable::new);

  @Test
  void constructorShouldRejectNullField() {
    assertThatThrownBy(() -> fieldInjectableFactory.create(null, A.class))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("field cannot be null")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectNullOwnerType() {
    assertThatThrownBy(() -> fieldInjectableFactory.create(A.class.getDeclaredField("a"), null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType cannot be null")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectIncompatibleOwnerType() {
    assertThatThrownBy(() -> fieldInjectableFactory.create(A.class.getDeclaredField("a"), B.class))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType must be assignable to field's declaring class: class hs.ddif.core.definition.FieldInjectableFactoryTest$B; declaring class: class hs.ddif.core.definition.FieldInjectableFactoryTest$A")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectFieldWithUnresolvableReturnType() {
    assertThatThrownBy(() -> fieldInjectableFactory.create(B.class.getDeclaredField("b"), B.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [public java.lang.Object hs.ddif.core.definition.FieldInjectableFactoryTest$B.b] is of unresolvable type")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectFieldWithUnresolvableTypeVariables() {
    assertThatThrownBy(() -> fieldInjectableFactory.create(B.class.getDeclaredField("d"), B.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [public java.util.List hs.ddif.core.definition.FieldInjectableFactoryTest$B.d] has unresolvable type variables")
      .hasNoCause();
  }

  @Test
  void constructorShouldRejectFieldAnnotatedWithInject() {
    assertThatThrownBy(() -> fieldInjectableFactory.create(A.class.getDeclaredField("c"), A.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [java.lang.String hs.ddif.core.definition.FieldInjectableFactoryTest$A.c] cannot be annotated with Inject")
      .hasNoCause();
  }

  @Test
  void constructorShouldAcceptValidParameters() throws NoSuchFieldException, SecurityException {
    Injectable injectable = fieldInjectableFactory.create(C.class.getField("b"), C.class);

    assertEquals(String.class, injectable.getType());
  }

  @Test
  void shouldReturnCorrectInjectableForNonStaticField() throws Exception {
    Injectable injectable = fieldInjectableFactory.create(C.class.getField("b"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Owner Type [class hs.ddif.core.definition.FieldInjectableFactoryTest$C]"
    );

    assertEquals("Bye", injectable.createInstance(Bindings.resolve(injectable.getBindings(), new C())));
  }

  @Test
  void shouldReturnCorrectInjectableForStaticField() throws Exception {
    Injectable injectable = fieldInjectableFactory.create(C.class.getField("e"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).isEmpty();

    assertEquals("Hello", injectable.createInstance(Bindings.resolve(injectable.getBindings())));
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
