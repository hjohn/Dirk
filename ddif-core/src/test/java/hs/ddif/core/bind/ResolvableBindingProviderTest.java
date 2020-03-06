package hs.ddif.core.bind;

import hs.ddif.core.inject.store.ResolvableBinding;
import hs.ddif.core.inject.store.ResolvableBindingProvider;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.reflect.AccessibleObject;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;

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
    Map<AccessibleObject, ResolvableBinding[]> map = ResolvableBindingProvider.ofClass(Subclass.class);

    assertEquals(2, map.size());

    {
      Binding[] bindings = map.get(ClassWithGenericFields.class.getDeclaredField("fieldA"));

      assertEquals(1, bindings.length);
      assertEquals(String.class, bindings[0].getRequiredKey().getType());
    }

    {
      Binding[] bindings = map.get(ClassWithGenericFields.class.getDeclaredField("fieldB"));

      assertEquals(1, bindings.length);
      assertEquals(Integer.class, bindings[0].getRequiredKey().getType());
    }
  }

  @Test
  public void resolveForMethodShouldCreateCorrectBindings() throws NoSuchMethodException, SecurityException {
    ResolvableBinding[] bindings = ResolvableBindingProvider.ofExecutable(Subclass.class.getDeclaredMethod("create", Integer.class, Double.class));

    assertEquals(2, bindings.length);
    assertEquals(Integer.class, bindings[0].getRequiredKey().getType());
    assertEquals(Double.class, bindings[1].getRequiredKey().getType());
    assertEquals(Set.of(AnnotationDescriptor.describe(Big.class)), bindings[0].getRequiredKey().getQualifiers());
    assertEquals(Set.of(), bindings[1].getRequiredKey().getQualifiers());
  }
}
