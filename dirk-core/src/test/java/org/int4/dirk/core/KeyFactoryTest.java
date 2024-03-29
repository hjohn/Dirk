package org.int4.dirk.core;

import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.util.Annotations;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KeyFactoryTest {

  @Test
  void shouldCreateValidKeys() {
    assertThat(KeyFactory.of(String.class)).isNotNull();
    assertThat(KeyFactory.of(String.class, Red.class)).isNotNull();
    assertThat(KeyFactory.of(String.class, Annotations.of(Red.class))).isNotNull();
  }

  @Test
  void shouldRejectBadInput() {
    assertThatThrownBy(() -> KeyFactory.of(String.class, Integer.class))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported qualifier, must be Class<? extends Annotation> or Annotation: class java.lang.Integer")
      .hasNoCause();

    assertThatThrownBy(() -> KeyFactory.of(String.class, 5))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported qualifier, must be Class<? extends Annotation> or Annotation: 5")
      .hasNoCause();
  }
}
