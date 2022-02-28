package hs.ddif.core.store;

import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.InjectableFactories;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.util.TypeReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class QualifiedTypeStoreGenericsTest {
  private final InjectableFactories injectableFactories = new InjectableFactories();
  private final ClassInjectableFactory classInjectableFactory = injectableFactories.forClass();
  private final MethodInjectableFactory methodInjectableFactory =  injectableFactories.forMethod();
  private final InstanceInjectableFactory instanceInjectableFactory = injectableFactories.forInstance();

  private QualifiedTypeStore<Injectable> store;

  @BeforeEach
  public void beforeEach() {
    this.store = new QualifiedTypeStore<>(i -> new Key(i.getType(), i.getQualifiers()), cls -> true);
  }

  /**
   * Tests that a store containing String and Integer will resolve to the correct number of matches
   * when the store is queried for various ways of referring to these classes using generics.
   */
  @Test
  public void shouldResolveToStringWhenUsingGenerics() {
    store.put(classInjectableFactory.create(String.class));   // String extends Object implements CharSequence, Serializable, Comparable<String>
    store.put(instanceInjectableFactory.create(Integer.MAX_VALUE));  // Integer extends Number implements Serializable, Comparable<Integer>

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

  @Test
  public void shouldAcceptGenericTypesWithoutTypeVariables() {
    store.put(classInjectableFactory.create(TypeUtils.parameterize(ArrayList.class, String.class)));  // type is fully specified, so accepted
  }

  /**
   * Tests the store with some classes that have multiple generic parameters for potential resolution
   * problems.  This test only handles a few potential cases, and may need extending.
   */
  @Test
  public void shouldResolveInjectablesWithMultipleGenericParameters() {
    store.put(classInjectableFactory.create(OrangeToOrangeJuiceConverter.class));
    store.put(classInjectableFactory.create(AppleToSlicedAppleConverter.class));

    assertTrue(store.resolve(new Key(OrangeToOrangeJuiceConverter.class)).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<? extends Fruit, ? extends Juice<?>>>() {}.getType())).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<?, ?>>() {}.getType())).size() == 2);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<?, OrangeJuice>>() {}.getType())).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<?, ? extends Juice<Orange>>>() {}.getType())).size() == 1);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<? extends Fruit, ?>>() {}.getType())).size() == 2);
    assertTrue(store.resolve(new Key(new TypeReference<Converter<Apple, ?>>() {}.getType())).size() == 1);
  }

  @Test
  public void shouldBeAbleToAddAndRemoveResolvedGenericInterface() throws NoSuchMethodException, SecurityException {
    Injectable injectable = methodInjectableFactory.create(
      SuppliesGenericResolvedInterface.class.getDeclaredMethod("supply"),
      SuppliesGenericResolvedInterface.class
    );

    store.put(injectable);
    store.remove(injectable);
  }

  public static class SuppliesGenericResolvedInterface {
    public List<String> supply() {
      return null;
    }
  }

  public static class OrangeToOrangeJuiceConverter implements Converter<Orange, OrangeJuice> {
  }

  public static class AppleToSlicedAppleConverter extends FruitToSlicedFruitConverter<Apple> {
  }

  public static class FruitToSlicedFruitConverter<T extends Fruit> implements Converter<T, Sliced<T>> {
  }

  interface Converter<I, O> {
    default O convert(I input) { throw new UnsupportedOperationException(input.toString()); }
  }

  class Fruit {
  }

  class Orange extends Fruit {
  }

  class Apple extends Fruit {
  }

  class Juice<E> {
    E e;
  }

  class Sliced<E> {
    E e;
  }

  class OrangeJuice extends Juice<Orange> {
  }

  class SlicedApple extends Sliced<Apple> {
  }
}
