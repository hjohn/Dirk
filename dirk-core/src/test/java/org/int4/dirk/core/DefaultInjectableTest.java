package org.int4.dirk.core;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.core.definition.BadQualifiedTypeException;
import org.int4.dirk.core.definition.ExtendedScopeResolver;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.definition.InjectionTarget;
import org.int4.dirk.core.definition.QualifiedType;
import org.int4.dirk.core.definition.injection.Constructable;
import org.int4.dirk.core.definition.injection.Injection;
import org.int4.dirk.core.test.qualifiers.Green;
import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import nl.jqno.equalsverifier.EqualsVerifier;

public class DefaultInjectableTest {
  private static final ExtendedScopeResolver SCOPE_RESOLVER = mock(ExtendedScopeResolver.class);

  private final InjectionTarget it1 = mock(InjectionTarget.class);
  private final InjectionTarget it2 = mock(InjectionTarget.class);
  private final InjectionTarget it3 = mock(InjectionTarget.class);

  private final Constructable<String> constructable = new Constructable<>() {
    @Override
    public void destroy(String instance) {
    }

    @Override
    public String create(List<Injection> injections) {
      return "5";
    }

    @Override
    public boolean needsDestroy() {
      return false;
    }
  };

  @Test
  void constructorShouldAcceptValidParameters() throws CreationException, BadQualifiedTypeException {
    Injectable<String> injectable = new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class), List.of(), SCOPE_RESOLVER, String.class, constructable);

    assertThat(injectable.create(List.of())).isEqualTo("5");
    assertThat(injectable.getType()).isEqualTo(String.class);
    assertThat(injectable.getScopeResolver()).isEqualTo(SCOPE_RESOLVER);
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.getInjectionTargets()).isEmpty();
    assertThat(injectable.toString()).isEqualTo("Class [java.lang.String]");

    injectable = new DefaultInjectable<>(String.class, Set.of(Types.parameterize(Supplier.class, String.class)), new QualifiedType(Types.parameterize(Supplier.class, String.class), Set.of(Annotations.of(Red.class), Annotations.of(Green.class))), List.of(it1, it2, it3), SCOPE_RESOLVER, String.class, constructable);

    assertThat(injectable.create(List.of())).isEqualTo("5");
    assertThat(injectable.getType()).isEqualTo(Types.parameterize(Supplier.class, String.class));
    assertThat(injectable.getScopeResolver()).isEqualTo(SCOPE_RESOLVER);
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class), Annotations.of(Green.class));
    assertThat(injectable.getInjectionTargets()).containsExactly(it1, it2, it3);
    assertThat(injectable.toString()).isEqualTo("Class [@org.int4.dirk.core.test.qualifiers.Green(), @org.int4.dirk.core.test.qualifiers.Red() java.lang.String]");
  }

  @Test
  void constructorShouldRejectBadParameters() {
    assertThatThrownBy(() -> new DefaultInjectable<>(null, Set.of(String.class), new QualifiedType(String.class), List.of(), SCOPE_RESOLVER, String.class, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, null, null, List.of(), SCOPE_RESOLVER, String.class, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("types")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(Integer.class), null, List.of(), SCOPE_RESOLVER, String.class, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("qualifiedType")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(String.class), null, List.of(), SCOPE_RESOLVER, String.class, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("qualifiedType")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), null, SCOPE_RESOLVER, String.class, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("injectionTargets")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), List.of(), null, String.class, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("scopeResolver")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), List.of(), SCOPE_RESOLVER, null, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("discriminator")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), List.of(), SCOPE_RESOLVER, String.class, null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("constructable")
      .hasNoCause();
  }

  @Test
  void equalsAndHashCodeShouldRespectContract() throws BadQualifiedTypeException {
    EqualsVerifier
      .forClass(DefaultInjectable.class)
      .withNonnullFields("qualifiedType", "ownerType", "discriminator")
      .withCachedHashCode("hashCode", "calculateHash", new DefaultInjectable<>(String.class, Set.of(String.class), new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), List.of(), SCOPE_RESOLVER, String.class, constructable))
      .withIgnoredFields("injectionTargets", "types", "scopeResolver", "constructable")
      .verify();
  }
}
