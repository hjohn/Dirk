package hs.ddif.core;

import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import hs.ddif.core.ClassInjectable;
import hs.ddif.core.InjectableStore;
import hs.ddif.core.Key;
import hs.ddif.core.TypeReference;

public class InjectableStoreGenericsTest {
  private InjectableStore store;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void before() {
    this.store = new InjectableStore();
  }

  /**
   * Tests that a store containing String and Integer will resolve to the correct number of matches
   * when the store is queried for various ways of referring to these classes using generics.
   */
  @Test
  public void shouldResolveToStringWhenUsingGenerics() {
    store.put(new ClassInjectable(String.class));   // String extends Object implements CharSequence, Serializable, Comparable<String>
    store.put(new InstanceInjectable(Integer.MAX_VALUE));  // Integer extends Number implements Serializable, Comparable<Integer>

    // Resolvables
    assertTrue(store.resolve(new Key(Object.class)).size() == 2);
    assertTrue(store.resolve(new Key(String.class)).size() == 1);
    assertTrue(store.resolve(new Key(Serializable.class)).size() == 2);
    assertTrue(store.resolve(new Key(CharSequence.class)).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<String>>() {}.getType())).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<?>>() {}.getType())).size() == 2);
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? extends String>>() {}.getType())).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? extends Object>>() {}.getType())).size() == 2);
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? extends Serializable>>() {}.getType())).size() == 2);
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? extends CharSequence>>() {}.getType())).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? extends Comparable<String>>>() {}.getType())).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? extends Comparable<? extends Comparable<String>>>>() {}.getType())).size() == 1);  // crazy!
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? extends Comparable<? extends Comparable<? extends String>>>>() {}.getType())).size() == 1);  // crazy!
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? extends Comparable<? extends Comparable<? super String>>>>() {}.getType())).size() == 1);  // crazy!
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? super String>>() {}.getType())).size() == 1);

    // Unresolvables
    assertTrue(store.resolve(new Key(Long.class)).isEmpty());
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<Object>>() {}.getType())).isEmpty());
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<Long>>() {}.getType())).isEmpty());
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? super Object>>() {}.getType())).isEmpty());
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? extends Comparable<? extends Comparable<Object>>>>() {}.getType())).isEmpty());
    assertTrue(store.resolve(new Key(new TypeReference<Comparable<? extends Comparable<? super Comparable<String>>>>() {}.getType())).isEmpty());
  }

  /**
   * Tests that a store will not allow adding a class that has generic parameters.
   */
  @Test
  public void shouldRejectTypesWithUnboundTypeParameters() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("class java.util.ArrayList has type parameters [E]: Injection candidates with type parameters are not supported.");

    store.put(new ClassInjectable(ArrayList.class));
  }

  /**
   * Tests the store with some classes that have multiple generic parameters for potential resolution
   * problems.  This test only handles a few potential cases, and may need extending.
   */
  @Test
  public void shouldResolveInjectablesWithMultipleGenericParameters() {
    store.put(new ClassInjectable(OrangeToOrangeJuiceConverter.class));
    store.put(new ClassInjectable(AppleToSlicedAppleConverter.class));

    assertTrue(store.resolve(new Key(OrangeToOrangeJuiceConverter.class)).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<? extends Fruit, ? extends Juice<?>>>() {}.getType())).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<?, ?>>() {}.getType())).size() == 2);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<?, OrangeJuice>>() {}.getType())).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<?, ? extends Juice<Orange>>>() {}.getType())).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<? extends Fruit, ?>>() {}.getType())).size() == 2);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<Apple, ?>>() {}.getType())).size() == 1);
  }

  public static class OrangeToOrangeJuiceConverter implements Converter<Orange, OrangeJuice> {
  }

  public static class AppleToSlicedAppleConverter extends FruitToSlicedFruitConverter<Apple> {
  }

  public static class FruitToSlicedFruitConverter<T extends Fruit> implements Converter<T, Sliced<T>> {
  }

  interface Converter<I, O> {
  }

  class Fruit {
  }

  class Orange extends Fruit {
  }

  class Apple extends Fruit {
  }

  class Juice<E> {
  }

  class Sliced<E> {
  }

  class OrangeJuice extends Juice<Orange> {
  }

  class SlicedApple extends Sliced<Apple> {
  }
}
