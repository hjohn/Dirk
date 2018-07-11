package hs.ddif.core;

import java.lang.reflect.AccessibleObject;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BinderTest {

  @SuppressWarnings("unused")
  private static class ClassWithGenericFields<A, B> {
    @Inject private A fieldA;
    @Inject private B fieldB;
  }

  private static class Subclass extends ClassWithGenericFields<String, Integer> {
  }

  @Test
  public void resolveShouldBindToGenericFieldInSubclass() throws NoSuchFieldException, SecurityException {
    Map<AccessibleObject, Binding[]> map = Binder.resolve(Subclass.class);

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
}
