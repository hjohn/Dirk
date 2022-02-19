package hs.ddif.core.store;

import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import nl.jqno.equalsverifier.EqualsVerifier;

public class KeyTest {

  @Test
  void constructorShouldAcceptValidParameters() {
    Key key = new Key(String.class);

    assertThat(key.getType()).isEqualTo(String.class);
    assertThat(key.getQualifiers()).isEmpty();
    assertThat(key.toString()).isEqualTo("java.lang.String");

    key = new Key(TypeUtils.parameterize(Supplier.class, String.class), Set.of(Annotations.of(Red.class), Annotations.of(Green.class)));

    assertThat(key.getType()).isEqualTo(TypeUtils.parameterize(Supplier.class, String.class));
    assertThat(key.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class), Annotations.of(Green.class));
    assertThat(key.toString()).isEqualTo("@hs.ddif.core.test.qualifiers.Green() @hs.ddif.core.test.qualifiers.Red() java.util.function.Supplier<java.lang.String>");
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

    assertThatThrownBy(() -> new Key(String.class, Set.of(Annotations.of(Singleton.class))))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("qualifiers must all be annotations annotated with @Qualifier: [@javax.inject.Singleton()]")
      .hasNoCause();
  }

  @Test
  void equalsAndHashCodeShouldRespectContract() {
    EqualsVerifier
      .forClass(Key.class)
      .withNonnullFields("type", "qualifiers")
      .verify();
  }
}
