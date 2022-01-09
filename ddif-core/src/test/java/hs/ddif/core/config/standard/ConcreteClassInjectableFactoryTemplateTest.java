package hs.ddif.core.config.standard;

import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.bind.BindingProvider;
import hs.ddif.core.inject.injectable.ClassInjectableFactoryTemplate.TypeAnalysis;
import hs.ddif.core.inject.injectable.ResolvableInjectable;
import hs.ddif.core.store.Key;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcreteClassInjectableFactoryTemplateTest {
  private BindingProvider bindingProvider = new BindingProvider(DefaultBinding::new);
  private ConcreteClassInjectableFactoryTemplate extension = new ConcreteClassInjectableFactoryTemplate(bindingProvider, ResolvableInjectable::new);

  @Test
  void shouldFailPreconditionWhenReturnTypeIsAbstract() {
    assertThat(extension.analyze(BadFactoryA.class).getUnsuitableReason(BadFactoryA.class)).isEqualTo("Type cannot be abstract: interface hs.ddif.core.config.standard.ConcreteClassInjectableFactoryTemplateTest$BadFactoryA");
  }

  @Test
  void shouldCreateInjectableForValidClass() {
    ResolvableInjectable injectable = create(B.class);

    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(new Key(String.class));
    assertThat(injectable.getScope()).isNull();
    assertThat(injectable.getQualifiers()).isEmpty();
  }

  @Test
  void shouldCreateInjectableWithQualifiersAndScope() {
    ResolvableInjectable injectable = create(C.class);

    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(
      new Key(String.class), new Key(Integer.class), new Key(Double.class, List.of(Annotations.of(Big.class)))
    );
    assertThat(injectable.getScope()).isEqualTo(Annotations.of(Singleton.class));
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(
      Annotations.of(Red.class), Annotations.of(Green.class)
    );
  }

  private ResolvableInjectable create(Type type) {
    TypeAnalysis<Type> analysis = extension.analyze(type);

    if(analysis.isNegative()) {
      throw new IllegalArgumentException(analysis.getUnsuitableReason(type));
    }

    return extension.create(analysis);
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
