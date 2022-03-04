package hs.ddif.core.config.standard;

import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.definition.BadQualifiedTypeException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.QualifiedType;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.injection.InjectionContext;
import hs.ddif.core.instantiation.injection.ObjectFactory;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

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
  private static final ScopeResolver SCOPE_RESOLVER = new SingletonScopeResolver(Annotations.of(Singleton.class));

  private final Binding binding1 = mock(Binding.class);
  private final Binding binding2 = mock(Binding.class);
  private final Binding binding3 = mock(Binding.class);

  private final ObjectFactory objectFactory = new ObjectFactory() {
    @Override
    public void destroyInstance(Object instance, InjectionContext injectionContext) {
    }

    @Override
    public Object createInstance(InjectionContext injectionContext) {
      return 5;
    }
  };

  @Test
  void constructorShouldAcceptValidParameters() throws InstanceCreationFailure, BadQualifiedTypeException {
    Injectable injectable = new DefaultInjectable(String.class, new QualifiedType(String.class), List.of(), SCOPE_RESOLVER, null, objectFactory);

    assertThat(injectable.createInstance(new DefaultInjectionContext(List.of()))).isEqualTo(5);
    assertThat(injectable.getType()).isEqualTo(String.class);
    assertThat(injectable.getScopeResolver()).isEqualTo(SCOPE_RESOLVER);
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.getBindings()).isEmpty();
    assertThat(injectable.toString()).isEqualTo("Injectable[java.lang.String]");

    injectable = new DefaultInjectable(String.class, new QualifiedType(TypeUtils.parameterize(Supplier.class, String.class), Set.of(Annotations.of(Red.class), Annotations.of(Green.class))), List.of(binding1, binding2, binding3), SCOPE_RESOLVER, null, objectFactory);

    assertThat(injectable.createInstance(new DefaultInjectionContext(List.of()))).isEqualTo(5);
    assertThat(injectable.getType()).isEqualTo(TypeUtils.parameterize(Supplier.class, String.class));
    assertThat(injectable.getScopeResolver()).isEqualTo(SCOPE_RESOLVER);
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class), Annotations.of(Green.class));
    assertThat(injectable.getBindings()).containsExactly(binding1, binding2, binding3);
    assertThat(injectable.toString()).isEqualTo("Injectable[@hs.ddif.core.test.qualifiers.Green() @hs.ddif.core.test.qualifiers.Red() java.util.function.Supplier<java.lang.String>]");
  }

  @Test
  void constructorShouldRejectBadParameters() {
    assertThatThrownBy(() -> new DefaultInjectable(null, new QualifiedType(String.class), List.of(), SCOPE_RESOLVER, null, objectFactory))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable(String.class, null, List.of(), SCOPE_RESOLVER, null, objectFactory))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("qualifiedType cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable(String.class, new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), null, SCOPE_RESOLVER, null, objectFactory))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("bindings cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable(String.class, new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), List.of(), null, null, objectFactory))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("scopeResolver cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectable(String.class, new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), List.of(), SCOPE_RESOLVER, null, null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("objectFactory cannot be null")
      .hasNoCause();
  }

  @Test
  void equalsAndHashCodeShouldRespectContract() throws BadQualifiedTypeException {
    EqualsVerifier
      .forClass(DefaultInjectable.class)
      .withNonnullFields("qualifiedType", "ownerType")
      .withCachedHashCode("hashCode", "calculateHash", new DefaultInjectable(String.class, new QualifiedType(String.class, Set.of(Annotations.of(Red.class))), List.of(), SCOPE_RESOLVER, null, objectFactory))
      .withIgnoredFields("bindings", "scopeResolver", "objectFactory")
      .verify();
  }
}
