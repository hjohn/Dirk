package hs.ddif.core.definition;

import java.util.Queue;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

public class FieldInjectableFactoryTest {
  private final FieldInjectableFactory factory = new InjectableFactories().forField();

  @Test
  void createShouldRejectNullField() {
    assertThatThrownBy(() -> factory.create(null, A.class))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("field cannot be null")
      .hasNoCause();
  }

  @Test
  void createShouldRejectNullOwnerType() {
    assertThatThrownBy(() -> factory.create(A.class.getDeclaredField("a"), null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType cannot be null")
      .hasNoCause();
  }

  @Test
  void createShouldRejectIncompatibleOwnerType() {
    assertThatThrownBy(() -> factory.create(A.class.getDeclaredField("a"), B.class))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType must be assignable to member's declaring class: class hs.ddif.core.definition.FieldInjectableFactoryTest$B; declaring class: class hs.ddif.core.definition.FieldInjectableFactoryTest$A")
      .hasNoCause();
  }

  @Test
  void createShouldRejectFieldWithUnresolvableReturnType() {
    assertThatThrownBy(() -> factory.create(B.class.getDeclaredField("b"), B.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [public java.lang.Object hs.ddif.core.definition.FieldInjectableFactoryTest$B.b] has unresolvable return type")
      .hasNoCause();
  }

  @Test
  void createShouldRejectFieldWithUnresolvableTypeVariables() {
    assertThatThrownBy(() -> factory.create(B.class.getDeclaredField("d"), B.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [public java.util.Queue hs.ddif.core.definition.FieldInjectableFactoryTest$B.d] has unsuitable type")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[java.util.Queue<T>] cannot have unresolvable type variables or wild cards")
      .hasNoCause();
  }

  @Test
  void createShouldRejectFieldAnnotatedWithInject() {
    assertThatThrownBy(() -> factory.create(A.class.getDeclaredField("c"), A.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [java.lang.String hs.ddif.core.definition.FieldInjectableFactoryTest$A.c] should not have an inject annotation, but found: [@jakarta.inject.Inject()]")
      .hasNoCause();
  }

  @Test
  void createShouldAcceptValidParameters() throws NoSuchFieldException, SecurityException {
    Injectable<String> injectable = factory.create(C.class.getField("b"), C.class);

    assertEquals(String.class, injectable.getType());
  }

  @Test
  void createShouldReturnCorrectInjectableForNonStaticField() throws Exception {
    Injectable<String> injectable = factory.create(C.class.getField("b"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).extracting(Object::toString).containsExactly(
      "Owner Type [class hs.ddif.core.definition.FieldInjectableFactoryTest$C]"
    );

    assertEquals("Bye", injectable.create(Bindings.resolve(injectable.getBindings(), new C())));
  }

  @Test
  void createShouldReturnCorrectInjectableForStaticField() throws Exception {
    Injectable<String> injectable = factory.create(C.class.getField("e"), C.class);

    assertEquals(String.class, injectable.getType());
    assertThat(injectable.getBindings()).isEmpty();

    assertEquals("Hello", injectable.create(Bindings.resolve(injectable.getBindings())));
  }

  static class A {
    String a;
    @Inject String c;
  }

  static class B<T> {
    @SuppressWarnings("unchecked")
    public T b = (T)"Bye";
    public Queue<T> d;
  }

  static class C extends B<String> {
    public static String e = "Hello";
  }
}
