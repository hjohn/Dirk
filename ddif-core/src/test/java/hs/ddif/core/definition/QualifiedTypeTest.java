package hs.ddif.core.definition;

import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;
import hs.ddif.core.util.Types;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import nl.jqno.equalsverifier.EqualsVerifier;

public class QualifiedTypeTest {

  @Test
  void constructorShouldAcceptValidParameters() throws BadQualifiedTypeException {
    QualifiedType key = new QualifiedType(String.class);

    assertThat(key.getType()).isEqualTo(String.class);
    assertThat(key.getQualifiers()).isEmpty();
    assertThat(key.toString()).isEqualTo("java.lang.String");

    key = new QualifiedType(TypeUtils.parameterize(Supplier.class, String.class), Set.of(Annotations.of(Red.class), Annotations.of(Green.class)));

    assertThat(key.getType()).isEqualTo(TypeUtils.parameterize(Supplier.class, String.class));
    assertThat(key.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class), Annotations.of(Green.class));
    assertThat(key.toString()).isEqualTo("@hs.ddif.core.test.qualifiers.Green() @hs.ddif.core.test.qualifiers.Red() java.util.function.Supplier<java.lang.String>");
  }

  @Test
  void constructorShouldRejectBadParameters() {
    assertThatThrownBy(() -> new QualifiedType(null, Set.of(Annotations.of(Red.class))))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("type cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new QualifiedType(String.class, null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("qualifiers cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new QualifiedType(void.class, Set.of(Annotations.of(Singleton.class))))
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[@javax.inject.Singleton() java.lang.Void] cannot be void or Void")
      .hasNoCause();

    assertThatThrownBy(() -> new QualifiedType(List.class))
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[java.util.List] cannot have unresolvable type variables or wild cards")
      .hasNoCause();

    assertThatThrownBy(() -> new QualifiedType(Types.wildcardExtends(String.class)))
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[? extends java.lang.String] cannot have unresolvable type variables or wild cards")
      .hasNoCause();
  }

  @Test
  void equalsAndHashCodeShouldRespectContract() {
    EqualsVerifier
      .forClass(QualifiedType.class)
      .withNonnullFields("type", "qualifiers")
      .verify();
  }
}
