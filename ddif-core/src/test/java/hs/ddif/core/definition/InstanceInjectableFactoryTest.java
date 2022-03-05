package hs.ddif.core.definition;

import hs.ddif.core.util.Annotations;

import java.util.List;

import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    assertThat(injectable.getBindings()).isEmpty();
    assertThat(injectable.getScopeResolver()).isEqualTo(injectableFactories.getScopeResolver(Annotations.of(Singleton.class)));
    assertThat(injectable.create(List.of())).isEqualTo("Hello World");
  }
}
