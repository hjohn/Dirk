package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.store.Key;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ClassExtensionTest {
  private ClassExtension extension = new ClassExtension(ResolvableInjectable::new);

  @Test
  void preconditionTextShouldDescribeRequirements() {
    assertThat(extension.getPreconditionText()).isEqualTo("a concrete class");
  }

  @Test
  void shouldFailPreconditionWhenReturnTypeIsAbstract() {
    assertNull(extension.create(BadFactoryA.class));
  }

  @Test
  void shouldCreateInjectableForValidClass() {
    ResolvableInjectable injectable = extension.create(B.class);

    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(new Key(String.class));
    assertThat(injectable.getScope()).isNull();
    assertThat(injectable.getQualifiers()).isEmpty();
  }

  @Test
  void shouldCreateInjectableWithQualifiersAndScope() {
    ResolvableInjectable injectable = extension.create(C.class);

    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(
      new Key(String.class), new Key(Integer.class), new Key(Double.class, List.of(Annotations.of(Big.class)))
    );
    assertThat(injectable.getScope()).isEqualTo(Annotations.of(Singleton.class));
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(
      Annotations.of(Red.class), Annotations.of(Green.class)
    );
  }

  interface A {}

  interface BadFactoryA {
    A create(Integer x, Integer y);
  }

  static class B {
    @Inject String x;

    public B() {
    }
  }

  @Singleton
  @Red @Green
  static class C {
    @Inject String x;

    @Inject
    public C(@SuppressWarnings("unused") Integer a, @SuppressWarnings("unused") @Big Double b) {
    }
  }
}
