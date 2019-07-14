package hs.ddif.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TypeUtilsTest {

  private abstract static class Test1 implements List<String> {
  }

  private static class Test3<T> extends ArrayList<T> {
  }

  private abstract static class Test5 implements List<String>, Consumer<Integer> {
  }

  ArrayList<String> test2;
  Test3<String> test4;
  Test3<? extends String> test6;

  @Test
  public void determineClassFromTypeShouldReturnClass() throws NoSuchFieldException, SecurityException {
    assertEquals(Test1.class, TypeUtils.determineClassFromType(new TypeReference<Test1>() {}.getType()));
    assertEquals(ArrayList.class, TypeUtils.determineClassFromType(TypeUtilsTest.class.getDeclaredField("test2").getGenericType()));
    assertEquals(Test3.class, TypeUtils.determineClassFromType(TypeUtilsTest.class.getDeclaredField("test4").getGenericType()));
    assertEquals(Test5.class, TypeUtils.determineClassFromType(new TypeReference<Test5>() {}.getType()));
  }

  @Test
  public void shouldReturnGenericType() throws NoSuchFieldException, SecurityException {
    assertEquals(String.class, TypeUtils.determineTypeOfImplementedType(new TypeReference<Test1>() {}.getType(), List.class));
    assertEquals(String.class, TypeUtils.determineTypeOfImplementedType(TypeUtilsTest.class.getDeclaredField("test2").getGenericType(), List.class));
    assertEquals(String.class, TypeUtils.determineTypeOfImplementedType(TypeUtilsTest.class.getDeclaredField("test2").getGenericType(), ArrayList.class));
    assertEquals(String.class, TypeUtils.determineTypeOfImplementedType(TypeUtilsTest.class.getDeclaredField("test4").getGenericType(), List.class));
    assertEquals(String.class, TypeUtils.determineTypeOfImplementedType(new TypeReference<Test5>() {}.getType(), List.class));
    assertEquals(Integer.class, TypeUtils.determineTypeOfImplementedType(new TypeReference<Test5>() {}.getType(), Consumer.class));
  }

  @Test
  public void shouldReturnNullForNotImplementedTypes() throws NoSuchFieldException, SecurityException {
    assertNull(TypeUtils.determineTypeOfImplementedType(new TypeReference<Test1>() {}.getType(), Set.class));
    assertNull(TypeUtils.determineTypeOfImplementedType(TypeUtilsTest.class.getDeclaredField("test4").getGenericType(), Set.class));
  }

  @Test
  public void shouldReturnNullForNonConcreteTypes() throws NoSuchFieldException, SecurityException {
    assertNull(TypeUtils.determineTypeOfImplementedType(TypeUtilsTest.class.getDeclaredField("test6").getGenericType(), List.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenImplementedTypeIsNotGeneric() {
    TypeUtils.determineTypeOfImplementedType(new TypeReference<Test1>() {}.getType(), Object.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenImplementedTypeHasMoreThanOneTypeParameter() {
    TypeUtils.determineTypeOfImplementedType(new TypeReference<Test1>() {}.getType(), Map.class);
  }
}
