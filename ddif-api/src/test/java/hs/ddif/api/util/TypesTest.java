package hs.ddif.api.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TypesTest {

  @Test
  void getSuperTypesShouldReturnCorrectResult() {
    assertThat(Types.getSuperTypes(BookShop.class)).containsExactlyInAnyOrder(
      Object.class,
      BookShop.class,
      Business.class,
      Shop.class,
      Franchise.class
    );

    assertThat(Types.getSuperTypes(Properties.class)).containsExactlyInAnyOrder(
      Object.class,
      Serializable.class,
      Properties.class,
      Cloneable.class,
      Hashtable.class,
      Map.class,
      Dictionary.class
    );

    assertThat(Types.getSuperTypes(List.class)).containsExactlyInAnyOrder(
      List.class,
      Collection.class,
      Iterable.class
    );
  }

  @Test
  void getGenericSuperTypesShouldReturnCorrectResult() {
    assertThat(Types.getGenericSuperTypes(BookShop.class)).containsExactlyInAnyOrder(
      Object.class,
      BookShop.class,
      Business.class,
      Types.parameterize(Shop.class, Book.class),
      Types.parameterize(Franchise.class, ReadOrDie.class)
    );

    assertThat(Types.getGenericSuperTypes(Properties.class)).containsExactlyInAnyOrder(
      Object.class,
      Serializable.class,
      Properties.class,
      Cloneable.class,
      Types.parameterize(Hashtable.class, Object.class, Object.class),
      Types.parameterize(Map.class, Object.class, Object.class),
      Types.parameterize(Dictionary.class, Object.class, Object.class)
    );

    assertThat(Types.getGenericSuperTypes(Types.parameterize(List.class, String.class))).containsExactlyInAnyOrder(
      Types.parameterize(List.class, String.class),
      Types.parameterize(Collection.class, String.class),
      Types.parameterize(Iterable.class, String.class)
    );
  }

  interface Shop<T> {
    default T shopStuff(T t) { return t; }
  }

  interface Franchise<T> {
    default T franchiseStuff(T t) { return t; }
  }

  static class Book {
  }

  static class ReadOrDie {
  }

  static class Business implements Franchise<ReadOrDie> {
  }

  static class BookShop extends Business implements Shop<Book> {
  }
}
