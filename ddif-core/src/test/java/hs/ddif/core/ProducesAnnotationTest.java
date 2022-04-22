package hs.ddif.core;

import hs.ddif.annotations.Produces;
import hs.ddif.api.Injector;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.instantiation.InstanceCreationException;
import hs.ddif.api.instantiation.MultipleInstancesException;
import hs.ddif.api.instantiation.NoSuchInstanceException;
import hs.ddif.api.util.TypeReference;
import hs.ddif.api.util.Types;
import hs.ddif.core.definition.BadQualifiedTypeException;
import hs.ddif.core.inject.store.CyclicDependencyException;
import hs.ddif.core.inject.store.UnresolvableDependencyException;
import hs.ddif.core.store.DuplicateKeyException;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.test.qualifiers.Small;
import hs.ddif.test.util.ReplaceCamelCaseDisplayNameGenerator;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@DisplayNameGeneration(ReplaceCamelCaseDisplayNameGenerator.class)
public class ProducesAnnotationTest {
  private Injector injector = Injectors.manual();
  private Injector autoDiscoveryInjector = Injectors.autoDiscovering();

  @Test
  void registerShouldRejectFactoryWithUnresolvableProducerDependencies() {
    UnresolvableDependencyException e = assertThrows(UnresolvableDependencyException.class, () -> injector.register(SimpleFactory1.class));

    assertThat(e).hasMessageStartingWith("Missing dependency [java.lang.Integer] required for Parameter 0 [class java.lang.Integer] of [");
    assertFalse(injector.contains(Object.class));
  }

  @Test
  void registerShouldRejectFactoryWithUnresolvableDependencies() {
    UnresolvableDependencyException e = assertThrows(UnresolvableDependencyException.class, () -> injector.register(SimpleFactory2.class));

    assertThat(e).hasMessageStartingWith("Missing dependency [java.lang.Integer] required for Field [");
    assertFalse(injector.contains(Object.class));
  }

  @Test
  void registerShouldRejectCyclicalFactories() {
    CyclicDependencyException e;

    e = assertThrows(CyclicDependencyException.class, () -> injector.register(CyclicalFactory1.class));

    assertEquals(2, e.getCycle().size());

    e = assertThrows(CyclicDependencyException.class, () -> injector.register(CyclicalFactory2.class));

    assertEquals(3, e.getCycle().size());
    assertFalse(injector.contains(Object.class));
  }

  @Test
  void registerShouldRejectBadlyAnnotatedProducesField() {
    assertThatThrownBy(() -> injector.register(BadFactory1.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [java.lang.Integer hs.ddif.core.ProducesAnnotationTest$BadFactory1.size] should not have an inject annotation, but found: [@jakarta.inject.Inject()]")
      .hasNoCause();

    assertFalse(injector.contains(Object.class));
  }

  @Test
  void registerShouldRejectBadlyAnnotatedProducesMethod() {
    assertThatThrownBy(() -> injector.register(BadFactory2.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [java.lang.Integer hs.ddif.core.ProducesAnnotationTest$BadFactory2.create(java.lang.Double)] should not have an inject annotation, but found: [@jakarta.inject.Inject()]")
      .hasNoCause();

    assertFalse(injector.contains(Object.class));
  }

  @Test
  void registerShouldRejectVoidProducesMethod() {
    assertThatThrownBy(() -> injector.register(BadFactory3.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [void hs.ddif.core.ProducesAnnotationTest$BadFactory3.create()] has unsuitable type")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[java.lang.Void] cannot be void or Void")
      .hasNoCause();

    assertFalse(injector.contains(Object.class));
  }

  @Test
  void registerShouldRejectProducesMethodWithUnresolvedTypeVariables() {
    assertThatThrownBy(() -> injector.register(GenericProduces.class))  // GenericProduces has a Produces method with a type variable T which is not provided
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public java.util.ArrayList hs.ddif.core.ProducesAnnotationTest$GenericProduces.create()] has unsuitable type")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[java.util.ArrayList<T>] cannot have unresolvable type variables or wild cards")
      .hasNoCause();

    assertFalse(injector.contains(Object.class));
  }

  @Test
  void registerShouldRejectClassWithUnresolvedTypeVariables() {
    assertThatThrownBy(() -> injector.register(GenericFactory1.class))  // GenericFactory1 has a type variable T which is not provided
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public java.util.ArrayList hs.ddif.core.ProducesAnnotationTest$GenericFactory1.create()] has unsuitable type")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[java.util.ArrayList<T>] cannot have unresolvable type variables or wild cards")
      .hasNoCause();

    assertFalse(injector.contains(Object.class));
  }

  @Test
  void shouldSupportGenericProducerMethod() throws Exception {
    injector.register(StringMethodFactory.class);
    injector.register(Types.parameterize(GenericFactory1.class, Long.class));

    assertThrows(DuplicateKeyException.class, () -> injector.register(Types.parameterize(GenericFactory1.class, Long.class)));

    List<String> x1 = injector.getInstance(new TypeReference<ArrayList<String>>() {}.getType());
    List<Long> y1 = injector.getInstance(new TypeReference<ArrayList<Long>>() {}.getType());
    List<String> x2 = injector.getInstance(new TypeReference<ArrayList<String>>() {}.getType());
    List<Long> y2 = injector.getInstance(new TypeReference<ArrayList<Long>>() {}.getType());

    assertTrue(x1 == x2);
    assertTrue(y1 == y2);
    assertFalse((Object)x1 == y1);
    assertFalse((Object)x1 == y2);
    assertTrue(injector.contains(Object.class));

    injector.remove(StringMethodFactory.class);
    injector.remove(Types.parameterize(GenericFactory1.class, Long.class));

    assertFalse(injector.contains(Object.class));
  }

  @Test
  void shouldSupportGenericProducerField() throws Exception {
    StringFactory stringFactory = new StringFactory("hi");
    IntegerFactory integerFactory = new IntegerFactory(123);

    injector.registerInstance(stringFactory);
    injector.registerInstance(integerFactory);

    assertEquals("hi", injector.getInstance(String.class));
    assertEquals(123, injector.getInstance(Integer.class));

    assertThrows(NoSuchInstanceException.class, () -> injector.getInstance(new TypeReference<GenericFactory2<Long>>() {}.getType()));

    GenericFactory2<String> x1 = injector.getInstance(new TypeReference<GenericFactory2<String>>() {}.getType());
    GenericFactory2<String> x2 = injector.getInstance(StringFactory.class);

    assertTrue(x1 == x2);
    assertTrue(injector.contains(Object.class));

    injector.removeInstance(stringFactory);
    injector.removeInstance(integerFactory);

    assertFalse(injector.contains(Object.class));
  }

  @Test
  public void shouldRegisterSelfDependentFactory() throws Exception {
    injector.register(SelfDependentFactory.class);

    assertNotNull(injector.getInstance(Phone.class));

    injector.remove(SelfDependentFactory.class);

    assertFalse(injector.contains(Object.class));
  }

  @Test
  public void shouldNotRegisterAutoDiscoveryDependentFactoryWithoutUsingAutoDiscovery() {
    assertThrows(UnresolvableDependencyException.class, () -> injector.register(AutoDiscoveryDependentFactory.class));
    assertFalse(injector.contains(Object.class));
  }

  @Test
  public void shouldRegisterAutoDiscoveryDependentFactory() throws Exception {
    autoDiscoveryInjector.register(AutoDiscoveryDependentFactory.class);

    assertNotNull(autoDiscoveryInjector.getInstance(Phone.class));

    autoDiscoveryInjector.remove(AutoDiscoveryDependentFactory.class);

    // TODO should an auto discovered class (Bus.class) be auto-removed as well?
    // Currently it isn't, hence why the assert below will fail:
    // assertFalse(autoDiscoveryInjector.contains(Object.class));

    assertFalse(autoDiscoveryInjector.contains(AutoDiscoveryDependentFactory.class));
  }

  @Test
  public void shouldRegisterFactoryWithProducesWhichRequiresProvidedClassInSameFactory() throws Exception {
    injector.register(CrossDependentFactory.class);

    assertNotNull(injector.getInstance(Phone.class));

    injector.remove(CrossDependentFactory.class);

    assertFalse(injector.contains(Object.class));
  }

  @Test
  public void shouldAutoDiscoverProducesAnnotations() throws Exception {
    assertNotNull(autoDiscoveryInjector.getInstance(AnotherFactory.class));
    assertNotNull(autoDiscoveryInjector.getInstance(Truck.class));
  }

  @Test
  public void shouldAutoDiscoverNestedProducesAnnotations() throws Exception {
    Thing instance = autoDiscoveryInjector.getInstance(Thing.class);

    assertNotNull(instance.anotherFactory);
    assertNotNull(instance.truck);
    assertEquals(2000, instance.truck.size);
  }

  @Test
  public void shouldAutoDiscoverComplicatedNestedProducesAnnotations() throws Exception {
    ComplicatedThing instance = autoDiscoveryInjector.getInstance(ComplicatedThing.class);

    assertNotNull(instance.part2.part3);
    assertNotNull(instance.part1.truck);
    assertEquals(3001, instance.part1.truck.size);
  }

  @Test
  public void shouldNotRegisterClassWhichDependsOnUnregisteredClass() {
    assertThatThrownBy(() -> injector.register(StaticFieldBasedPhoneProducer.class))
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasMessage("Missing dependency [hs.ddif.core.ProducesAnnotationTest$Thing3] required for Field [hs.ddif.core.ProducesAnnotationTest$Thing3 hs.ddif.core.ProducesAnnotationTest$StaticFieldBasedPhoneProducer.thing]")
      .hasNoCause();
    assertFalse(injector.contains(Object.class));
  }

  @Test
  public void shouldRegisterClassWhichProducesInstances() throws Exception {
    injector.register(Thing4.class);

    Phone phone = injector.getInstance(Phone.class);

    assertNotNull(phone);
    assertEquals("Hi", phone.type);

    injector.remove(Thing4.class);

    assertFalse(injector.contains(Object.class));
  }

  @Test
  public void requiredDependencySuppliedIndirectlyByStaticFieldShouldNotCauseCycle() throws Exception {
    autoDiscoveryInjector.getInstance(StaticFieldBasedPhoneProducer.class);
  }

  @Test
  public void requiredDependencySuppliedIndirectlyByStaticMethodShouldNotCauseCycle() throws Exception {
    autoDiscoveryInjector.getInstance(StaticMethodBasedPhoneProducer.class);
  }

  @Test
  public void shouldDiscoverClassWhichProducesInstances() throws Exception {
    assertNotNull(autoDiscoveryInjector.getInstance(Thing4.class));

    Phone phone = autoDiscoveryInjector.getInstance(Phone.class);

    assertNotNull(phone);
    assertEquals("Hi", phone.type);
  }

  @Nested
  class WithFactoriesRegistered {
    private Integer intValue = 15;
    private String prefix = "pre";

    @BeforeEach
    void beforeEach() throws Exception {
      injector.registerInstance(intValue);
      injector.registerInstance(prefix);
      injector.register(SingletonFactory.class);
      injector.register(UnscopedFactory.class);
    }

    @AfterEach
    void afterEach() throws Exception {
      injector.remove(UnscopedFactory.class);
      injector.remove(SingletonFactory.class);
      injector.removeInstance(prefix);
      injector.removeInstance(intValue);

      assertFalse(injector.contains(Object.class));
    }

    @Test
    void registeringAFactoryTwiceShouldThrowException() {
      assertThrows(DuplicateKeyException.class, new Executable() {
        @Override
        public void execute() throws Exception {
          injector.register(UnscopedFactory.class);
        }
      });
    }

    @Test
    void factoryScopeShouldBeRespected() throws Exception {
      SingletonFactory factory1 = injector.getInstance(SingletonFactory.class);
      SingletonFactory factory2 = injector.getInstance(SingletonFactory.class);

      assertTrue(factory1 == factory2);

      UnscopedFactory unscopedFactory1 = injector.getInstance(UnscopedFactory.class);
      UnscopedFactory unscopedFactory2 = injector.getInstance(UnscopedFactory.class);

      assertFalse(unscopedFactory1 == unscopedFactory2);
    }

    @Test
    void factoryShouldHaveDependenciesInjected() throws Exception {
      UnscopedFactory unscopedFactory = injector.getInstance(UnscopedFactory.class);

      assertEquals("pre", unscopedFactory.prefix);
    }

    @Test
    void productsShouldBeAvailableDirectly() throws Exception {
      Car car = injector.getInstance(Car.class);

      assertEquals("pre-toyota-15", car.name);

      assertThrows(MultipleInstancesException.class, new Executable() {
        @Override
        public void execute() throws Exception {
          injector.getInstance(Phone.class);
        }
      });

      assertEquals(Phone.class, injector.getInstance(Phone.class, Red.class).getClass());
    }

    @Test
    void phoneScopeShouldBeRespected() throws Exception {
      Phone redPhone1 = injector.getInstance(Phone.class, Red.class);
      Phone redPhone2 = injector.getInstance(Phone.class, Red.class);

      assertFalse(redPhone1 == redPhone2);

      Phone greenPhone1 = injector.getInstance(Phone.class, Green.class);
      Phone greenPhone2 = injector.getInstance(Phone.class, Green.class);

      assertTrue(greenPhone1 == greenPhone2);
    }

    @Test
    void allPhonesShouldBeReturnedWhenGettingInstancesOfPhone() throws InstanceCreationException {
      assertEquals(4, injector.getInstances(Phone.class).size());
    }

    @Test
    void injectorShouldReturnGenericTypes() throws Exception {
      injector.getInstance(new TypeReference<Garage<Bus>>() {}.getType());

      Garage<Bus> busGarage1 = injector.getInstance(new TypeReference<Garage<Bus>>() {}.getType());
      Garage<Bus> busGarage2 = injector.getInstance(new TypeReference<Garage<Bus>>() {}.getType());
      Garage<Car> carGarage1 = injector.getInstance(new TypeReference<Garage<Car>>() {}.getType());
      Garage<Car> carGarage2 = injector.getInstance(new TypeReference<Garage<Car>>() {}.getType());
      Garage<Truck> truckGarage1 = injector.getInstance(new TypeReference<Garage<Truck>>() {}.getType());
      Garage<Truck> truckGarage2 = injector.getInstance(new TypeReference<Garage<Truck>>() {}.getType());

      assertEquals(carGarage1, carGarage2);  // singleton
      assertNotEquals(busGarage1, busGarage2);  // not singleton
      assertNotEquals(truckGarage1, truckGarage2);  // not singleton
      assertNotEquals(busGarage1, carGarage1);  // different generic types
    }

    @Test
    void providerCreatedViaFactoryShouldSupplyObjects() throws Exception {
      assertEquals(Bus.class, injector.getInstance(Bus.class).getClass());
    }

    @Test
    void recursiveFactoryShouldWork() throws Exception {
      assertEquals(Truck.class, injector.getInstance(Truck.class).getClass());
    }
  }

  public abstract static class PhoneFactory {
    @Red public abstract Phone createRedPhone();

    @Singleton
    @Green
    @Produces
    public Phone createGreenPhone() {
      return new Phone("motorola");
    }

    @Produces
    private static AnotherFactory createAnotherFactory() {
      return new AnotherFactory();
    }
  }

  public static class AnotherFactory {
    @Produces
    public Truck createTruck() {
      return new Truck(2000);
    }
  }

  public static class SelfDependentFactory {
    @Produces
    public Phone createPhone(Truck truck) {  // requires a Truck, which is produced in the same class
      return new Phone("truck-phone: " + truck);
    }

    @Produces
    public Truck createTruck() {
      return new Truck(2500);
    }
  }

  public static class CrossDependentFactory {
    @Produces
    public Phone createPhone(Truck truck) {  // requires a Truck, which is produced in the same class
      return new Phone("truck-phone: " + truck);
    }

    @Produces
    public Truck get() {
      return new Truck(2525);
    }
  }

  public static class AutoDiscoveryDependentFactory {
    @Produces
    public Phone createPhone(Bus bus) {  // requires a Bus, a class that can be auto discovered
      return new Phone("truck-phone: " + bus);
    }
  }

  @Singleton
  public static class SingletonFactory extends PhoneFactory {
    @Inject private String prefix;

    @Produces @Small private Phone miniPhone = new Phone("mini");

    @Singleton
    @Produces
    private Car createCar(int size) {
      return new Car(prefix + "-toyota-" + size);
    }

    @Singleton
    @Produces
    public Garage<Car> createCarGarage(Car car) {
      return new Garage<>(car);
    }

    @Produces
    public Garage<Bus> createBusGarage(Bus bus) {
      return new Garage<>(bus);
    }

    @Produces
    public Garage<Truck> createTruckGarage(Truck truck) {
      return new Garage<>(truck);
    }

    @Produces
    public BusProvider createBusProvider() {
      return new BusProvider();
    }

    @Override
    @Produces
    @Red
    public Phone createRedPhone() {
      return new Phone("emergency");
    }
  }

  public static class UnscopedFactory {
    @Inject String prefix;

    @Produces @Big
    public Phone createBigPhone() {
      return new Phone("nokia");
    }
  }

  public static class CyclicalFactory1 {
    @Produces
    public Truck create(Car car) {
      return new Truck(car.name.length());
    }

    @Produces
    public Car create(Truck truck) {
      return new Car("bmw-of-size-" + truck.size);
    }
  }

  public static class CyclicalFactory2 {
    @Produces
    public Truck create(Vehicle vehicle) {
      return new Truck(vehicle.toString().length());
    }

    @Produces
    public Car create(Bus bus) {
      return new Car("bmw-of-size-" + bus.hashCode());
    }

    @Produces
    public Bus create(@SuppressWarnings("unused") Truck truck) {
      return new Bus();
    }

    @Produces
    public Float create() {
      return 2.0f;
    }
  }

  public static class SimpleFactory1 {
    @Produces
    public static Truck create(Integer size) {
      return new Truck(size);
    }
  }

  public static class SimpleFactory2 {
    @Inject Integer size;

    @Produces
    public Truck create() {
      return new Truck(size);
    }
  }

  public static class BadFactory1 {
    @Inject @Produces Integer size = 5;
  }

  public static class BadFactory2 {
    @Inject @Produces
    Integer create(Double size) {
      return size.intValue();
    }
  }

  public static class BadFactory3 {
    @Produces
    void create() {
    }
  }

  @Singleton
  public static class GenericFactory1<T> {
    @Produces
    @Singleton
    public ArrayList<T> create() {
      return new ArrayList<>();
    }
  }

  public static class GenericProduces {
    @Produces
    public <T> ArrayList<T> create() {
      return new ArrayList<>();
    }
  }

  @Singleton
  public static class StringMethodFactory extends GenericFactory1<String> {
  }

  public static class StringFactory extends GenericFactory2<String> {
    public StringFactory(String obj) {
      super(obj);
    }
  }

  public static class IntegerFactory extends GenericFactory2<Integer> {
    public IntegerFactory(Integer obj) {
      super(obj);
    }
  }

  public static class GenericFactory2<T> {
    @Produces private T obj;

    public GenericFactory2(T obj) {
      this.obj = obj;
    }
  }

  interface Vehicle {
  }

  public static class Car implements Vehicle {
    String name;

    public Car(String name) {
      this.name = name;
    }
  }

  public static class Bus {
  }

  public static class Phone {
    String type;

    public Phone(String type) {
      this.type = type;
    }
  }

  public static class Truck {
    int size;

    public Truck(int size) {
      this.size = size;
    }
  }

  public static class Garage<T> {
    T content;

    public Garage(T content) {
      this.content = content;
    }
  }

  public static class BusProvider {

    @Produces
    public Bus get() {
      return new Bus();
    }
  }

  public static class Thing {
    @Inject private AnotherFactory anotherFactory;  // produces Truck
    @Inject private Truck truck;  // as AnotherFactory produces Truck, this should be okay
  }

  public static class ComplicatedThing {
    public static class Part1 {
      @Inject Truck truck;
      @Produces Part5 part5 = new Part5(5);
    }

    public static class Part2 {
      @Inject private Part3 part3;  // produces Truck
    }

    public static class Part3 {
      @Produces
      Truck createTruck(@SuppressWarnings("unused") Part4 part4) {
        return new Truck(3001);
      }
    }

    public static class Part4 {
    }

    public static class Part5 {
      int part;

      Part5(int part) {
        this.part = part;
      }
    }

    @Inject Part1 part1;  // needs truck at some level
    @Inject Part2 part2;  // produces truck at some level
    @Inject Part5 part5;  // produced by part1
  }

  public static class Thing3 {
    @Inject Phone text;
  }

  public static class StaticFieldBasedPhoneProducer {
    @Produces static Phone phone = new Phone("Hi");

    @Inject Thing3 thing;
  }

  public static class StaticMethodBasedPhoneProducer {
    @Produces
    static Phone createPhone() {
      return new Phone("Hi");
    }

    @Inject Thing3 thing;
  }

  public static class Thing4 {
    @Produces String text = "Hi";
    @Produces Phone createPhone(String text) {
      return new Phone(text);
    }
  }

  @Test
  public void shouldRejectRegisteringClassWithProducerProducingExactTypeProvidedByATypeExtension() {
    assertThatThrownBy(() -> injector.register(P.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasNoCause();
  }

  @Test
  public void shouldAllowRegisteringClassWithProducerProducingSubtypeProvidedByATypeExtension() throws Exception {
    injector.register(R.class);

    assertNotNull(injector.getInstance(R.class));
    assertNotNull(injector.getInstance(S.class));
    assertNotNull(injector.getInstance(Q.class));
  }

  public static class P {
    @Produces
    Provider<Q> create() {
      return () -> new Q();
    }
  }

  static class Q {
  }

  public static class R {
    @Produces
    S create() {
      return new S();
    }
  }

  static class S implements Provider<Q> {
    @Override
    public Q get() {
      return new Q();
    }
  }
}
