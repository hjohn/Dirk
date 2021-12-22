package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.Key;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.AccessibleObject;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class BindingProviderTest {

  @SuppressWarnings("unused")
  private static class ClassWithGenericFields<A, B> {
    @Inject private A fieldA;
    @Inject private B fieldB;
  }

  private static class Subclass extends ClassWithGenericFields<String, Integer> {
    @SuppressWarnings("unused")
    public String create(@Big Integer dep1, Double dep2) {
      return null;
    }
  }

  @Test
  public void resolveForClassShouldBindToGenericFieldInSubclass() throws NoSuchFieldException, SecurityException {
    Map<AccessibleObject, List<Binding>> map = BindingProvider.ofClass(Subclass.class);

    assertEquals(2, map.size());

    {
      List<Binding> bindings = map.get(ClassWithGenericFields.class.getDeclaredField("fieldA"));

      assertEquals(1, bindings.size());
      assertEquals(String.class, bindings.get(0).getKey().getType());
    }

    {
      List<Binding> bindings = map.get(ClassWithGenericFields.class.getDeclaredField("fieldB"));

      assertEquals(1, bindings.size());
      assertEquals(Integer.class, bindings.get(0).getKey().getType());
    }
  }

  @Test
  public void resolveForMethodShouldCreateCorrectBindings() throws NoSuchMethodException, SecurityException {
    List<Binding> bindings = BindingProvider.ofExecutable(Subclass.class.getDeclaredMethod("create", Integer.class, Double.class), Subclass.class);

    assertEquals(3, bindings.size());
    assertEquals(Integer.class, bindings.get(0).getKey().getType());
    assertEquals(Double.class, bindings.get(1).getKey().getType());
    assertEquals(Subclass.class, bindings.get(2).getKey().getType());
    assertEquals(Set.of(AnnotationDescriptor.describe(Big.class)), bindings.get(0).getKey().getQualifiers());
    assertEquals(Set.of(), bindings.get(1).getKey().getQualifiers());
    assertEquals(Set.of(), bindings.get(2).getKey().getQualifiers());
  }

  @Test
  public void resolveForMethodShouldTakeNonStaticIntoAccount() throws NoSuchMethodException, SecurityException {
    assertThat(BindingProvider.ofExecutable(MethodHolder.class.getDeclaredMethod("create", Double.class), MethodHolder.class))
      .extracting(Binding::getKey)
      .containsExactly(
        new Key(Double.class, Set.of()),
        new Key(MethodHolder.class, Set.of())  // dependency on the declaring class as "create" is an instance method
      );
  }

  @Test
  public void resolveForMethodShouldTakeStaticIntoAccount() throws NoSuchMethodException, SecurityException {
    assertThat(BindingProvider.ofExecutable(MethodHolder.class.getDeclaredMethod("createStatic", Double.class), MethodHolder.class))
      .extracting(Binding::getKey)
      .containsExactly(
        new Key(Double.class, Set.of())
      );
  }

  @Test
  public void resolveForFieldShouldTakeNonStaticIntoAccount() throws NoSuchFieldException, SecurityException {
    assertThat(BindingProvider.ofField(FieldHolder.class.getDeclaredField("b"), FieldHolder.class))
      .extracting(Binding::getKey)
      .containsExactly(
        new Key(FieldHolder.class, Set.of())
      );
  }

  @Test
  public void resolveForFieldShouldTakeStaticIntoAccount() throws NoSuchFieldException, SecurityException {
    assertThat(BindingProvider.ofField(FieldHolder.class.getDeclaredField("a"), FieldHolder.class))
      .isEmpty();
  }

  public static class MethodHolder {
    public static String createStatic(Double x) {
      return "" + x;
    }

    public String create(Double x) {
      return "" + x;
    }
  }

  public static class FieldHolder {
    public static String a;

    public String b;
  }
}
