package hs.ddif.core.config.standard;

import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.UninjectableTypeException;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;
import hs.ddif.core.util.Types;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import nl.jqno.equalsverifier.EqualsVerifier;

public class DefaultInjectableTest {
  private final Binding binding1 = mock(Binding.class);
  private final Binding binding2 = mock(Binding.class);
  private final Binding binding3 = mock(Binding.class);

  @Test
  void constructorShouldAcceptValidParameters() throws UninjectableTypeException, InstanceCreationFailure {
    Injectable injectable = new DefaultInjectable(String.class, Set.of(), List.of(), Annotations.of(Singleton.class), null, injections -> 5);

    assertThat(injectable.createInstance(List.of())).isEqualTo(5);
    assertThat(injectable.getType()).isEqualTo(String.class);
    assertThat(injectable.getScope()).isEqualTo(Annotations.of(Singleton.class));
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.getBindings()).isEmpty();
    assertThat(injectable.toString()).isEqualTo("Injectable[java.lang.String]");

    injectable = new DefaultInjectable(TypeUtils.parameterize(Supplier.class, String.class), Set.of(Annotations.of(Red.class), Annotations.of(Green.class)), List.of(binding1, binding2, binding3), null, null, injections -> 6);

    assertThat(injectable.createInstance(List.of())).isEqualTo(6);
    assertThat(injectable.getType()).isEqualTo(TypeUtils.parameterize(Supplier.class, String.class));
    assertThat(injectable.getScope()).isNull();
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class), Annotations.of(Green.class));
    assertThat(injectable.getBindings()).containsExactly(binding1, binding2, binding3);
    assertThat(injectable.toString()).isEqualTo("Injectable[@hs.ddif.core.test.qualifiers.Green() @hs.ddif.core.test.qualifiers.Red() java.util.function.Supplier<java.lang.String>]");
  }

  @Test
  void constructorShouldRejectBadParameters() {
    assertThatThrownBy(() -> new DefaultInjectable(null, Set.of(Annotations.of(Red.class)), List.of(), Annotations.of(Singleton.class), null, injections -> 5))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("type cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable(String.class, null, List.of(), Annotations.of(Singleton.class), null, injections -> 5))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("qualifiers cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable(String.class, Set.of(Annotations.of(Red.class)), null, Annotations.of(Singleton.class), null, injections -> 5))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("bindings cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable(String.class, Set.of(Annotations.of(Red.class)), List.of(), Annotations.of(Singleton.class), null, null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("objectFactory cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable(void.class, Set.of(Annotations.of(Red.class)), List.of(), Annotations.of(Singleton.class), null, injections -> 5))
      .isExactlyInstanceOf(UninjectableTypeException.class)
      .hasMessage("[void] is not an injectable type")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable(List.class, Set.of(Annotations.of(Red.class)), List.of(), Annotations.of(Singleton.class), null, injections -> 5))
      .isExactlyInstanceOf(UninjectableTypeException.class)
      .hasMessage("[interface java.util.List] has unresolvable type variables")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable(Types.wildcardExtends(String.class), Set.of(Annotations.of(Red.class)), List.of(), Annotations.of(Singleton.class), null, injections -> 5))
      .isExactlyInstanceOf(UninjectableTypeException.class)
      .hasMessage("[? extends java.lang.String] has unresolvable type variables")
      .hasNoCause();
  }

  @Test
  void equalsAndHashCodeShouldRespectContract() throws UninjectableTypeException {
    EqualsVerifier
      .forClass(DefaultInjectable.class)
      .withNonnullFields("type", "qualifiers")
      .withCachedHashCode("hashCode", "calculateHash", new DefaultInjectable(String.class, Set.of(Annotations.of(Red.class)), List.of(), Annotations.of(Singleton.class), null, injections -> 5))
      .withIgnoredFields("bindings", "scope", "objectFactory")
      .verify();
  }
}
