package hs.ddif.core.config.standard;

import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.definition.BadQualifiedTypeException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.QualifiedType;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.injection.Constructable;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;
import hs.ddif.core.util.Types;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import jakarta.inject.Singleton;
import nl.jqno.equalsverifier.EqualsVerifier;

public class DefaultInjectableTest {
  private static final ScopeResolver SCOPE_RESOLVER = new SingletonScopeResolver(Annotations.of(Singleton.class));

  private final Binding binding1 = mock(Binding.class);
  private final Binding binding2 = mock(Binding.class);
  private final Binding binding3 = mock(Binding.class);

  private final Constructable<String> constructable = new Constructable<>() {
    @Override
    public void destroy(String instance) {
    }

    @Override
    public String create(List<Injection> injections) {
      return "5";
    }
  };

  @Test
  void constructorShouldAcceptValidParameters() throws InstanceCreationFailure, BadQualifiedTypeException {
    Injectable<String> injectable = new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class), List.of(), SCOPE_RESOLVER, null, constructable);

    assertThat(injectable.create(List.of())).isEqualTo("5");
    assertThat(injectable.getType()).isEqualTo(String.class);
    assertThat(injectable.getScopeResolver()).isEqualTo(SCOPE_RESOLVER);
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.getBindings()).isEmpty();
    assertThat(injectable.toString()).isEqualTo("Injectable[java.lang.String]");

    injectable = new DefaultInjectable<>(String.class, Set.of(Types.parameterize(Supplier.class, String.class)), new QualifiedType(Types.parameterize(Supplier.class, String.class), Set.of(Annotations.of(Red.class), Annotations.of(Green.class))), List.of(binding1, binding2, binding3), SCOPE_RESOLVER, null, constructable);

    assertThat(injectable.create(List.of())).isEqualTo("5");
    assertThat(injectable.getType()).isEqualTo(Types.parameterize(Supplier.class, String.class));
    assertThat(injectable.getScopeResolver()).isEqualTo(SCOPE_RESOLVER);
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class), Annotations.of(Green.class));
    assertThat(injectable.getBindings()).containsExactly(binding1, binding2, binding3);
    assertThat(injectable.toString()).isEqualTo("Injectable[@hs.ddif.core.test.qualifiers.Green() @hs.ddif.core.test.qualifiers.Red() java.util.function.Supplier<java.lang.String>]");
  }

  @Test
  void constructorShouldRejectBadParameters() {
    assertThatThrownBy(() -> new DefaultInjectable<>(null, Set.of(String.class), new QualifiedType(String.class), List.of(), SCOPE_RESOLVER, null, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, null, null, List.of(), SCOPE_RESOLVER, null, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("types cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(Integer.class), null, List.of(), SCOPE_RESOLVER, null, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("qualifiedType cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(String.class), null, List.of(), SCOPE_RESOLVER, null, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("qualifiedType cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), null, SCOPE_RESOLVER, null, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("bindings cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), List.of(), null, null, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("scopeResolver cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), List.of(), SCOPE_RESOLVER, null, null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("constructable cannot be null")
      .hasNoCause();
  }

  @Test
  void equalsAndHashCodeShouldRespectContract() throws BadQualifiedTypeException {
    EqualsVerifier
      .forClass(DefaultInjectable.class)
      .withNonnullFields("qualifiedType", "ownerType")
      .withCachedHashCode("hashCode", "calculateHash", new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), List.of(), SCOPE_RESOLVER, null, constructable))
      .withIgnoredFields("bindings", "types", "scopeResolver", "constructable")
      .verify();
  }
}
