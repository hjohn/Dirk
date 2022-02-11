package hs.ddif.core;

import hs.ddif.annotations.Argument;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.definition.DefinitionException;
import hs.ddif.core.definition.bind.BindingException;
import hs.ddif.core.inject.store.UnresolvableDependencyException;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssistedInjectionTest {
  private Injector injector = Injectors.manual();

  @Test
  public void shouldAcceptAndRemoveProducerInterface() {
    for(int i = 0; i < 2; i++) {
      injector.register(TestService.class);
      injector.register(TestAssistedSampleFactory.class);

      // The Product of the Factory should not be registered with the Injector:
      assertThatThrownBy(() -> injector.getInstance(TestAssistedSample.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasMessage("No such instance: [class hs.ddif.core.AssistedInjectionTest$TestAssistedSample]")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasMessage("No such instance: [class hs.ddif.core.AssistedInjectionTest$TestAssistedSample]")
        .hasNoCause();

      TestAssistedSampleFactory factory = injector.getInstance(TestAssistedSampleFactory.class);

      TestAssistedSample sample = factory.create(100);

      assertEquals(100, (int)sample.interval);
      assertEquals(TestService.class, sample.testService.getClass());

      injector.remove(TestAssistedSampleFactory.class);
      injector.remove(TestService.class);
    }
  }

  @Test
  public void shouldAcceptAndCreateProducerAbstractClass() {
    injector.register(ValueSupplier.class);
    injector.register(TestService.class);
    injector.register(TestAssistedAbstractSampleFactory.class);

    // The Product of the Factory should not be registered with the Injector:
    assertThatThrownBy(() -> injector.getInstance(TestAssistedAbstractSample.class))
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasMessage("No such instance: [class hs.ddif.core.AssistedInjectionTest$TestAssistedAbstractSample]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NoSuchInstance.class)
      .hasMessage("No such instance: [class hs.ddif.core.AssistedInjectionTest$TestAssistedAbstractSample]")
      .hasNoCause();

    TestAssistedAbstractSampleFactory factory = injector.getInstance(TestAssistedAbstractSampleFactory.class);

    TestAssistedAbstractSample sample = factory.create(2, 3);

    assertEquals(44 * 2 + 3, sample.interval);
    assertEquals(TestService.class, sample.testService.getClass());
  }

  @Test
  public void shouldAcceptAndCreateProducerAbstractClassWithConstructor() {
    injector.register(ValueSupplier.class);
    injector.register(TestService.class);
    injector.register(TestAssistedAbstractSampleFactoryWithConstructor.class);

    // The Product of the Factory should not be registered with the Injector:
    assertThatThrownBy(() -> injector.getInstance(TestAssistedAbstractSample.class))
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasMessage("No such instance: [class hs.ddif.core.AssistedInjectionTest$TestAssistedAbstractSample]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NoSuchInstance.class)
      .hasMessage("No such instance: [class hs.ddif.core.AssistedInjectionTest$TestAssistedAbstractSample]")
      .hasNoCause();

    TestAssistedAbstractSampleFactoryWithConstructor factory = injector.getInstance(TestAssistedAbstractSampleFactoryWithConstructor.class);

    TestAssistedAbstractSample sample = factory.create(2, 3);

    assertEquals(44 * 2 + 3, sample.interval);
    assertEquals(TestService.class, sample.testService.getClass());
  }

  @Test
  public void registerShouldRejectProducerWithMultipleAbstractMethods() {
    assertThatThrownBy(() -> injector.register(ProducerWithMultipleAbtractMethods.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessageStartingWith("Path [class hs.ddif.core.AssistedInjectionTest$ProducerWithMultipleAbtractMethods]: [class hs.ddif.core.AssistedInjectionTest$ProducerWithMultipleAbtractMethods] cannot be injected; failures:")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessageStartingWith("[class hs.ddif.core.AssistedInjectionTest$ProducerWithMultipleAbtractMethods] cannot be injected; failures:")
      .hasNoCause();
  }

  public static class TestAssistedInjectionClassWithProducerWithMultipleAbtractMethods {
  }

  public abstract static class ProducerWithMultipleAbtractMethods {
    public abstract TestAssistedInjectionClassWithProducerWithMultipleAbtractMethods create();
    public abstract TestAssistedInjectionClassWithProducerWithMultipleAbtractMethods create2();
  }

  @Test
  public void registerShouldRejectProducerWithIllegalReturnType() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectReturnType.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Path [interface hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectReturnType]: [interface hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectReturnType] cannot have unresolvable type variables: [T]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[interface hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectReturnType] cannot have unresolvable type variables: [T]")
      .hasNoCause();
  }

  public interface ProducerWithIncorrectReturnType<T> {
    public abstract T create();
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameterCount() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectParameterCount.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Path [interface hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameterCount]: Method [public abstract hs.ddif.core.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameterCount hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameterCount.create(java.lang.Double)] should have 0 argument(s) of types: {}")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.core.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameterCount hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameterCount.create(java.lang.Double)] should have 0 argument(s) of types: {}")
      .hasNoCause();
  }

  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameterCount {
    @Inject
    public TestAssistedInjectionClassWithProducerWithIncorrectParameterCount() {}
  }

  public interface ProducerWithIncorrectParameterCount {
    public abstract TestAssistedInjectionClassWithProducerWithIncorrectParameterCount create(Double notNeeded);
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameterType() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectParameterType.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Path [interface hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameterType]: Method [public abstract hs.ddif.core.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameterType hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameterType.create(java.lang.Double)] has argument [java.lang.Double text] with name 'text' that should be of type [class java.lang.String] but was: class java.lang.Double")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.core.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameterType hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameterType.create(java.lang.Double)] has argument [java.lang.Double text] with name 'text' that should be of type [class java.lang.String] but was: class java.lang.Double")
      .hasNoCause();
  }

  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameterType {
    @Inject @Argument private String text;

    @Inject
    public TestAssistedInjectionClassWithProducerWithIncorrectParameterType() {}
  }

  public interface ProducerWithIncorrectParameterType {
    public abstract TestAssistedInjectionClassWithProducerWithIncorrectParameterType create(@Argument("text") Double text);
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameter() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectParameter.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Path [interface hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameter]: Method [public abstract hs.ddif.core.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameter hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameter.create(java.lang.String)] is missing required argument with name: incorrect")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.core.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameter hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameter.create(java.lang.String)] is missing required argument with name: incorrect")
      .hasNoCause();
  }

  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameter {
    @Inject @Argument private String text;

    @Inject
    public TestAssistedInjectionClassWithProducerWithIncorrectParameter() {}
  }

  public interface ProducerWithIncorrectParameter {
    public abstract TestAssistedInjectionClassWithProducerWithIncorrectParameter create(@Argument("incorrect") String incorrect);
  }

  @Test
  @Disabled("Only works if sources are compiled without parameter information")
  public void registerShouldRejectProducerWithMissingParameterName() {
    assertThatThrownBy(() -> injector.register(TestAssistedInjectionClassWithProducerWithMissingParameterName.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessageContaining("Missing parameter name.")
      .hasNoCause();
  }

  public static class TestAssistedInjectionClassWithProducerWithMissingParameterName {
    @Inject @Argument private String text;

    @Inject
    public TestAssistedInjectionClassWithProducerWithMissingParameterName() {}
  }

  public interface ProducerWithMissingParameterName {
    public abstract TestAssistedInjectionClassWithProducerWithMissingParameterName create(String incorrect);
  }

  @Test
  @Disabled("Only works if sources are compiled without parameter information")
  public void registerShouldRejectTargetWithConstructorWithMissingParameterName() {
    assertThatThrownBy(() -> injector.register(TestAssistedInjectionTargetWithConstructorWithMissingParameterName.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessageContaining("Missing parameter name.")
      .hasNoCause();
  }

  public static class TestAssistedInjectionTargetWithConstructorWithMissingParameterName {
    String text;

    @Inject
    public TestAssistedInjectionTargetWithConstructorWithMissingParameterName(@Argument String text) {
      this.text = text;
    }
  }

  public interface ProducerForTargetWithConstructor {
    public abstract TestAssistedInjectionTargetWithConstructorWithMissingParameterName create(@Argument("text") String text);
  }

  @Test
  public void registerShouldAcceptProducerWhichNeedsNoFurtherConstruction() {
    injector.register(AutonomousProducer.class);

    AutonomousProducer producer = injector.getInstance(AutonomousProducer.class);

    TestTargetWithAutonomousProducer target = producer.create(5);

    assertEquals("abc5", target.text);
  }

  public static class TestTargetWithAutonomousProducer {
    String text;

    @Inject
    public TestTargetWithAutonomousProducer(@Argument String text) {
      this.text = text;
    }
  }

  public static class AutonomousProducer {
    public TestTargetWithAutonomousProducer create(int extension) {
      return new TestTargetWithAutonomousProducer("abc" + extension);
    }
  }

  @Test
  public void registerShouldAcceptAbstractProducerWithNoParametersWhichNeedsNoFurtherConstruction() {
    injector.register(AbstractProducerWithNoParameters.class);

    AbstractProducerWithNoParameters producer = injector.getInstance(AbstractProducerWithNoParameters.class);

    TestTargetWithAbstactProducerAndNoParameters target = producer.create();

    assertEquals("abc2", target.text);
  }

  public static class TestTargetWithAbstactProducerAndNoParameters {
    String text;

    @Inject
    public TestTargetWithAbstactProducerAndNoParameters() {
      this.text = "abc2";
    }
  }

  public static abstract class AbstractProducerWithNoParameters {
    public abstract TestTargetWithAbstactProducerAndNoParameters create();
  }

  @Test
  public void registerShouldAcceptInterfaceProducerWithNoParametersWhichNeedsNoFurtherConstruction() {
    injector.register(InterfaceProducerWithNoParameters.class);

    InterfaceProducerWithNoParameters producer = injector.getInstance(InterfaceProducerWithNoParameters.class);

    TestTargetWithInterfaceProducerAndNoParameters target = producer.create();

    assertEquals("abc2", target.text);
  }

  @Test
  public void dependenciesOfProductShouldBeCheckedToBeInStore() {
    // Registering TestAssistedSampleFactory should fail because the product TestAssistedSample
    // requires TestService which was not registered yet.
    assertThatThrownBy(() -> injector.register(TestAssistedSampleFactory.class))
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasMessageStartingWith("Missing dependency [class hs.ddif.core.AssistedInjectionTest$TestService] required for")
      .hasNoCause();
  }

  @Test
  public void shouldConstructComplicatedProduct() {
    injector.register(ComplicatedProductFactory.class);
    injector.registerInstance(3, Annotations.of(Green.class));
    injector.registerInstance(2, Annotations.of(Red.class));
    ComplicatedProduct product = injector.getInstance(ComplicatedProductFactory.class).create(11);

    assertThat(product.calculate()).isEqualTo(3 * 2 + 11);

    injector.registerInstance(4, Annotations.of(Green.class));
    injector.registerInstance(7, Annotations.of(Red.class));  // not picked up as not wrapped in provider

    assertThat(product.calculate()).isEqualTo((3 + 4) * 2 + 11);

    // In order to pickup the extra Red number, create a new Product:
    product = injector.getInstance(ComplicatedProductFactory.class).create(13);

    assertThat(product.calculate()).isEqualTo((3 + 4) * (2 * 7) + 13);
  }

  public static class TestTargetWithInterfaceProducerAndNoParameters {
    String text;

    @Inject
    public TestTargetWithInterfaceProducerAndNoParameters() {
      this.text = "abc2";
    }
  }

  public interface InterfaceProducerWithNoParameters {
    TestTargetWithInterfaceProducerAndNoParameters create();
  }

  public static class TestAssistedSample {
    @Inject public TestService testService;
    @Inject @Argument public Integer interval;

    @Inject
    public TestAssistedSample() {}
  }

  public static class TestAssistedAbstractSample {
    @Inject public TestService testService;
    @Inject @Argument public int interval;
    @Inject @Argument public Double factor;

    @Inject
    public TestAssistedAbstractSample() {}
  }

  public interface TestAssistedSampleFactory {
    TestAssistedSample create(@Argument("interval") Integer interval);
  }

  public static abstract class TestAssistedAbstractSampleFactory {
    @Inject private ValueSupplier valueSupplier;

    public TestAssistedAbstractSample create(int a, int b) {
      return create(valueSupplier.get() * a + b, 3.0);
    }

    public abstract TestAssistedAbstractSample create(@Argument("interval") int interval, @Argument("factor") Double factor);
  }

  public static abstract class TestAssistedAbstractSampleFactoryWithConstructor {
    private ValueSupplier valueSupplier;

    @Inject
    TestAssistedAbstractSampleFactoryWithConstructor(ValueSupplier valueSupplier) {
      this.valueSupplier = valueSupplier;
    }

    public TestAssistedAbstractSample create(int a, int b) {
      return create(valueSupplier.get() * a + b, 3.0);
    }

    public abstract TestAssistedAbstractSample create(@Argument("interval") Integer interval, @Argument("factor") double factor);
  }

  public static class TestService {
  }

  @Singleton
  public static class ValueSupplier {
    public int get() {
      return 44;
    }
  }

  public static class ComplicatedProduct {
    @Inject private @Green Provider<List<Integer>> numbers;
    @Inject private @Red Set<Integer> multipliers;
    @Inject @Argument private int offset;

    @Inject
    public ComplicatedProduct() {}

    public int calculate() {
      int sum = numbers.get().stream().mapToInt(Integer::intValue).sum();

      return sum * multipliers.stream().reduce((a, b) -> a * b).get() + offset;
    }
  }

  interface ComplicatedProductFactory {
    ComplicatedProduct create(int offset);
  }
}
