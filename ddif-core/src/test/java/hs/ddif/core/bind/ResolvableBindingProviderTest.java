package hs.ddif.core.bind;

import hs.ddif.core.inject.store.ResolvableBinding;
import hs.ddif.core.inject.store.ResolvableBindingProvider;
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

public class ResolvableBindingProviderTest {

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
    Map<AccessibleObject, List<ResolvableBinding>> map = ResolvableBindingProvider.ofClass(Subclass.class);

    assertEquals(2, map.size());

    {
      List<ResolvableBinding> bindings = map.get(ClassWithGenericFields.class.getDeclaredField("fieldA"));

      assertEquals(1, bindings.size());
      assertEquals(String.class, bindings.get(0).getRequiredKey().getType());
    }

    {
      List<ResolvableBinding> bindings = map.get(ClassWithGenericFields.class.getDeclaredField("fieldB"));

      assertEquals(1, bindings.size());
      assertEquals(Integer.class, bindings.get(0).getRequiredKey().getType());
    }
  }

  @Test
  public void resolveForMethodShouldCreateCorrectBindings() throws NoSuchMethodException, SecurityException {
    List<ResolvableBinding> bindings = ResolvableBindingProvider.ofExecutable(Subclass.class.getDeclaredMethod("create", Integer.class, Double.class), Subclass.class);

    assertEquals(3, bindings.size());
    assertEquals(Integer.class, bindings.get(0).getRequiredKey().getType());
    assertEquals(Double.class, bindings.get(1).getRequiredKey().getType());
    assertEquals(Subclass.class, bindings.get(2).getRequiredKey().getType());
    assertEquals(Set.of(AnnotationDescriptor.describe(Big.class)), bindings.get(0).getRequiredKey().getQualifiers());
    assertEquals(Set.of(), bindings.get(1).getRequiredKey().getQualifiers());
    assertEquals(Set.of(), bindings.get(2).getRequiredKey().getQualifiers());
  }

  @Test
  public void resolveForMethodShouldTakeNonStaticIntoAccount() throws NoSuchMethodException, SecurityException {
    assertThat(ResolvableBindingProvider.ofExecutable(MethodHolder.class.getDeclaredMethod("create", Double.class), MethodHolder.class))
      .extracting(ResolvableBinding::getRequiredKey)
      .containsExactly(
        new Key(Double.class, Set.of()),
        new Key(MethodHolder.class, Set.of())  // dependency on the declaring class as "create" is an instance method
      );
  }

  @Test
  public void resolveForMethodShouldTakeStaticIntoAccount() throws NoSuchMethodException, SecurityException {
    assertThat(ResolvableBindingProvider.ofExecutable(MethodHolder.class.getDeclaredMethod("createStatic", Double.class), MethodHolder.class))
      .extracting(ResolvableBinding::getRequiredKey)
      .containsExactly(
        new Key(Double.class, Set.of())
      );
  }

  @Test
  public void resolveForFieldShouldTakeNonStaticIntoAccount() throws NoSuchFieldException, SecurityException {
    assertThat(ResolvableBindingProvider.ofField(FieldHolder.class.getDeclaredField("b"), FieldHolder.class))
      .extracting(ResolvableBinding::getRequiredKey)
      .containsExactly(
        new Key(FieldHolder.class, Set.of())
      );
  }

  @Test
  public void resolveForFieldShouldTakeStaticIntoAccount() throws NoSuchFieldException, SecurityException {
    assertThat(ResolvableBindingProvider.ofField(FieldHolder.class.getDeclaredField("a"), FieldHolder.class))
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
