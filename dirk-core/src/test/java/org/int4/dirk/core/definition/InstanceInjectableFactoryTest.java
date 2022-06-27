package org.int4.dirk.core.definition;

import java.util.List;

import org.int4.dirk.core.InjectableFactories;
import org.int4.dirk.util.Annotations;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Singleton;

public class InstanceInjectableFactoryTest {
  private final InjectableFactories injectableFactories = new InjectableFactories();
  private final InstanceInjectableFactory factory = injectableFactories.forInstance();

  @Test
  void createShouldRejectNullField() {
    assertThatThrownBy(() -> factory.create(null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("instance cannot be null")
      .hasNoCause();
  }

  @Test
  void createShouldReturnInjectable() throws Exception {
    Injectable<String> injectable = factory.create("Hello World");

    assertThat(injectable.getType()).isEqualTo(String.class);
    assertThat(injectable.getInjectionTargets()).isEmpty();
    assertThat(injectable.getScopeResolver().getAnnotation()).isEqualTo(Annotations.of(Singleton.class));
    assertThat(injectable.create(List.of())).isEqualTo("Hello World");
  }
}
