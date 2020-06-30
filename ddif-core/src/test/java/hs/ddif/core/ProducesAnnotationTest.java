package hs.ddif.core;

import hs.ddif.annotations.Produces;
import hs.ddif.core.inject.consistency.CyclicDependencyException;
import hs.ddif.core.inject.consistency.UnresolvableDependencyException;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.store.DuplicateBeanException;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.test.qualifiers.Small;
import hs.ddif.core.util.ReplaceCamelCaseDisplayNameGenerator;
import hs.ddif.core.util.TypeReference;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.function.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.leangen.geantyref.TypeFactory;

@DisplayNameGeneration(ReplaceCamelCaseDisplayNameGenerator.class)
@TestInstance(Lifecycle.PER_CLASS)
public class ProducesAnnotationTest {
  private Injector injector = new Injector();

  @Test
  void registerShouldRejectFactoryWithUnresolvableProducerDependencies() {
    UnresolvableDependencyException e = assertThrows(UnresolvableDependencyException.class, () -> injector.register(SimpleFactory1.class));

    assertThat(e).hasMessageStartingWith("Missing dependency of type [class java.lang.Integer] required for Parameter 0 of [");
  }

  @Test
  void registerShouldRejectFactoryWithUnresolvableDependencies() {
    UnresolvableDependencyException e = assertThrows(UnresolvableDependencyException.class, () -> injector.register(SimpleFactory2.class));

    assertThat(e).hasMessageStartingWith("Missing dependency of type [class java.lang.Integer] required for Field [");
  }

  @Test
  void registerShouldRejectCyclicalFactories() {
    CyclicDependencyException e;

    e = assertThrows(CyclicDependencyException.class, () -> injector.register(CyclicalFactory1.class));

    assertEquals(2, e.getCycle().size());

    e = assertThrows(CyclicDependencyException.class, () -> injector.register(CyclicalFactory2.class));

    assertEquals(3, e.getCycle().size());
  }

  @Test
  void registerShouldRejectBadlyAnnotatedProducesField() {
    BindingException e = assertThrows(BindingException.class, () -> injector.register(BadFactory1.class));

    assertThat(e).hasMessageStartingWith("Field cannot be annotated with Inject:");
  }

  @Test
  void registerShouldRejectBadlyAnnotatedProducesMethod() {
    BindingException e = assertThrows(BindingException.class, () -> injector.register(BadFactory2.class));

    assertThat(e).hasMessageStartingWith("Method cannot be annotated with Inject:");
  }

  @Test
  void registerShouldRejectVoidProducesMethod() {
    BindingException e = assertThrows(BindingException.class, () -> injector.register(BadFactory3.class));

    assertThat(e).hasMessageStartingWith("Method has no return type:");
  }

  @Test
  void registerShouldRejectProducesMethodWithUnresolvedTypeVariables() {
    BindingException e = assertThrows(BindingException.class, () -> injector.register(GenericFactory1.class));  // GenericFactory1 has a type variable T which is not provided

    assertThat(e).hasMessageStartingWith("Method has unresolved type variables:");
  }

  @Test
  void shouldSupportGenericProducerMethod() throws BeanResolutionException {
    injector.register(StringMethodFactory.class);
    injector.register(TypeFactory.parameterizedClass(GenericFactory1.class, Long.class));

    assertThrows(DuplicateBeanException.class, () -> injector.register(TypeFactory.parameterizedClass(GenericFactory1.class, Long.class)));

    List<String> x1 = injector.getInstance(new TypeReference<List<String>>() {}.getType());
    List<Long> y1 = injector.getInstance(new TypeReference<List<Long>>() {}.getType());
    List<String> x2 = injector.getInstance(new TypeReference<List<String>>() {}.getType());
    List<Long> y2 = injector.getInstance(new TypeReference<List<Long>>() {}.getType());

    assertTrue(x1 == x2);
    assertTrue(y1 == y2);
    assertFalse((Object)x1 == y1);
    assertFalse((Object)x1 == y2);

    injector.remove(StringMethodFactory.class);
    injector.remove(TypeFactory.parameterizedClass(GenericFactory1.class, Long.class));
  }

  @Test
  void shouldSupportGenericProducerField() throws BeanResolutionException {
    StringFactory stringFactory = new StringFactory("hi");
    IntegerFactory integerFactory = new IntegerFactory(123);

    injector.registerInstance(stringFactory);
    injector.registerInstance(integerFactory);

    assertEquals("hi", injector.getInstance(String.class));
    assertEquals(123, injector.getInstance(Integer.class));

    assertThrows(BeanResolutionException.class, () -> injector.getInstance(new TypeReference<GenericFactory2<Long>>() {}.getType()));

    GenericFactory2<String> x1 = injector.getInstance(new TypeReference<GenericFactory2<String>>() {}.getType());
    GenericFactory2<String> x2 = injector.getInstance(StringFactory.class);

    assertTrue(x1 == x2);

    injector.removeInstance(stringFactory);
    injector.removeInstance(integerFactory);
  }

  @Nested
  class WithFactoriesRegistered {
    private Integer intValue = 15;
    private String prefix = "pre";

    @BeforeEach
    void beforeEach() {
      injector.registerInstance(intValue);
      injector.registerInstance(prefix);
      injector.register(SingletonFactory.class);
      injector.register(UnscopedFactory.class);
    }

    @AfterEach
    void afterEach() {
      injector.remove(UnscopedFactory.class);
      injector.remove(SingletonFactory.class);
      injector.removeInstance(prefix);
      injector.removeInstance(intValue);
    }

    @Test
    void registeringAFactoryTwiceShouldThrowException() {
      assertThrows(DuplicateBeanException.class, new Executable() {
        @Override
        public void execute() throws Throwable {
          injector.register(UnscopedFactory.class);
        }
      });
    }

    @Test
    void factoryScopeShouldBeRespected() throws BeanResolutionException {
      SingletonFactory factory1 = injector.getInstance(SingletonFactory.class);
      SingletonFactory factory2 = injector.getInstance(SingletonFactory.class);

      assertTrue(factory1 == factory2);

      UnscopedFactory unscopedFactory1 = injector.getInstance(UnscopedFactory.class);
      UnscopedFactory unscopedFactory2 = injector.getInstance(UnscopedFactory.class);

      assertFalse(unscopedFactory1 == unscopedFactory2);
    }

    @Test
    void factoryShouldHaveDependenciesInjected() throws BeanResolutionException {
      UnscopedFactory unscopedFactory = injector.getInstance(UnscopedFactory.class);

      assertEquals("pre", unscopedFactory.prefix);
    }

    @Test
    void productsShouldBeAvailableDirectly() throws BeanResolutionException {
      Car car = injector.getInstance(Car.class);

      assertEquals("pre-toyota-15", car.name);

      assertThrows(BeanResolutionException.class, new Executable() {
        @Override
        public void execute() throws Throwable {
          injector.getInstance(Phone.class);
        }
      });

      assertEquals(Phone.class, injector.getInstance(Phone.class, Red.class).getClass());
    }

    @Test
    void phoneScopeShouldBeRespected() throws BeanResolutionException {
      Phone redPhone1 = injector.getInstance(Phone.class, Red.class);
      Phone redPhone2 = injector.getInstance(Phone.class, Red.class);

      assertFalse(redPhone1 == redPhone2);

      Phone greenPhone1 = injector.getInstance(Phone.class, Green.class);
      Phone greenPhone2 = injector.getInstance(Phone.class, Green.class);

      assertTrue(greenPhone1 == greenPhone2);
    }

    @Test
    void allPhonesShouldBeReturnedWhenGettingInstancesOfPhone() throws BeanResolutionException {
      assertEquals(4, injector.getInstances(Phone.class).size());
    }

    @Test
    void injectorShouldReturnGenericTypes() throws BeanResolutionException {
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
    void providerCreatedViaFactoryShouldSupplyObjects() throws BeanResolutionException {
      assertEquals(Bus.class, injector.getInstance(Bus.class).getClass());
    }

    @Test
    void recursiveFactoryShouldWork() throws BeanResolutionException {
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

  public static class GenericFactory1<T> {
    @Produces
    @Singleton
    public ArrayList<T> create() {
      return new ArrayList<>();
    }
  }

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

  public static class BusProvider implements Provider<Bus> {

    @Override
    public Bus get() {
      return new Bus();
    }
  }
}
