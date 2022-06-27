package org.int4.dirk.core.util;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.function.Supplier;

import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Qualifier;
import nl.jqno.equalsverifier.EqualsVerifier;

public class KeyTest {

  @Test
  void constructorShouldAcceptValidParameters() {
    Key key = new Key(String.class);

    assertThat(key.getType()).isEqualTo(String.class);
    assertThat(key.getQualifiers()).isEmpty();
    assertThat(key.toString()).isEqualTo("java.lang.String");

    key = new Key(Types.parameterize(Supplier.class, String.class), Set.of(Annotations.of(Red.class), Annotations.of(Green.class)));

    assertThat(key.getType()).isEqualTo(Types.parameterize(Supplier.class, String.class));
    assertThat(key.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class), Annotations.of(Green.class));
    assertThat(key.toString()).isEqualTo("@org.int4.dirk.core.util.KeyTest$Green() @org.int4.dirk.core.util.KeyTest$Red() java.util.function.Supplier<java.lang.String>");
  }

  @Test
  void constructorShouldRejectBadParameters() {
    assertThatThrownBy(() -> new Key(null, Set.of(Annotations.of(Red.class))))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("type cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new Key(String.class, null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("qualifiers cannot be null")
      .hasNoCause();
  }

  @Test
  void equalsAndHashCodeShouldRespectContract() {
    EqualsVerifier
      .forClass(Key.class)
      .withNonnullFields("type", "qualifiers")
      .verify();
  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public @interface Red {
  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public @interface Green {
  }
}
