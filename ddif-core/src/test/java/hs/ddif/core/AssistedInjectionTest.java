package hs.ddif.core;

import hs.ddif.annotations.Argument;
import hs.ddif.annotations.Assisted;
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
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        .hasMessage("No such instance: [hs.ddif.core.AssistedInjectionTest$TestAssistedSample]")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasMessage("No such instance: [hs.ddif.core.AssistedInjectionTest$TestAssistedSample]")
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
      .hasMessage("No such instance: [hs.ddif.core.AssistedInjectionTest$TestAssistedAbstractSample]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NoSuchInstance.class)
      .hasMessage("No such instance: [hs.ddif.core.AssistedInjectionTest$TestAssistedAbstractSample]")
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
      .hasMessage("No such instance: [hs.ddif.core.AssistedInjectionTest$TestAssistedAbstractSample]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NoSuchInstance.class)
      .hasMessage("No such instance: [hs.ddif.core.AssistedInjectionTest$TestAssistedAbstractSample]")
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
      .hasMessage("[class hs.ddif.core.AssistedInjectionTest$ProducerWithMultipleAbtractMethods] must have a single abstract method to qualify for assisted injection")
      .hasNoCause();
  }

  @Assisted
  public abstract static class ProducerWithMultipleAbtractMethods {
    public abstract TestService create(String input);
    public abstract TestService create2(String input);
  }

  @Test
  public void registerShouldRejectProducerWithIllegalReturnType() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectReturnType.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract java.lang.Object hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectReturnType.create(java.lang.String)] must not have unresolvable type variables to qualify for assisted injection: [T]")
      .hasNoCause();
  }

  @Assisted
  public interface ProducerWithIncorrectReturnType<T> {
    public abstract T create(String input);
  }

  @Test
  public void registerShouldRejectProducerWithPrimitiveReturnType() {
    assertThatThrownBy(() -> injector.register(ProducerWithPrimitiveReturnType.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract int hs.ddif.core.AssistedInjectionTest$ProducerWithPrimitiveReturnType.create(java.lang.String)] must not return a primitive type to qualify for assisted injection")
      .hasNoCause();
  }

  @Assisted
  public interface ProducerWithPrimitiveReturnType {
    public abstract int create(String input);
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameterCount() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectParameterCount.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.core.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameterCount hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameterCount.create(java.lang.Double)] should have 0 argument(s) of types: {}")
      .hasNoCause();
  }

  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameterCount {
    @Inject
    public TestAssistedInjectionClassWithProducerWithIncorrectParameterCount() {}
  }

  @Assisted
  public interface ProducerWithIncorrectParameterCount {
    public abstract TestAssistedInjectionClassWithProducerWithIncorrectParameterCount create(Double notNeeded);
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameterType() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectParameterType.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.core.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameterType hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameterType.create(java.lang.Double)] has argument [java.lang.Double text] with name 'text' that should be of type [class java.lang.String] but was: class java.lang.Double")
      .hasNoCause();
  }

  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameterType {
    @Inject @Argument private String text;

    @Inject
    public TestAssistedInjectionClassWithProducerWithIncorrectParameterType() {}
  }

  @Assisted
  public interface ProducerWithIncorrectParameterType {
    public abstract TestAssistedInjectionClassWithProducerWithIncorrectParameterType create(@Argument("text") Double text);
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameter() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectParameter.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.core.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameter hs.ddif.core.AssistedInjectionTest$ProducerWithIncorrectParameter.create(java.lang.String)] is missing required argument with name: incorrect")
      .hasNoCause();
  }

  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameter {
    @Inject @Argument private String text;

    @Inject
    public TestAssistedInjectionClassWithProducerWithIncorrectParameter() {}
  }

  @Assisted
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
  public void registerShouldAcceptAbstractProducerWithNoParameters() {
    injector.register(AbstractProducerWithNoParameters.class);

    assertNotNull(injector.getInstance(AbstractProducerWithNoParameters.class).create());
  }

  public static class TestTargetWithAbstactProducerAndNoParameters {
    String text;

    @Inject
    public TestTargetWithAbstactProducerAndNoParameters() {
      this.text = "abc2";
    }
  }

  @Assisted
  public static abstract class AbstractProducerWithNoParameters {
    public abstract TestTargetWithAbstactProducerAndNoParameters create();
  }

  @Test
  public void registerShouldAcceptInterfaceProducerWithNoParameters() {
    injector.register(InterfaceProducerWithNoParameters.class);

    assertNotNull(injector.getInstance(InterfaceProducerWithNoParameters.class).create());
  }

  @Test
  public void dependenciesOfProductShouldBeCheckedToBeInStore() {
    // Registering TestAssistedSampleFactory should fail because the product TestAssistedSample
    // requires TestService which was not registered yet.
    assertThatThrownBy(() -> injector.register(TestAssistedSampleFactory.class))
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasMessageStartingWith("Missing dependency [hs.ddif.core.AssistedInjectionTest$TestService] required for")
      .hasNoCause();
  }

  @Test
  public void shouldRejectProducerWithFinalBinding() {
    injector.registerInstance(5);

    assertThatThrownBy(() -> injector.register(ProducerWithFinalBinding.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.AssistedInjectionTest$ProducerWithFinalBinding] or its product [class hs.ddif.core.AssistedInjectionTest$Product] could not be bound")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessageStartingWith("Field [final int hs.ddif.core.AssistedInjectionTest$ProducerWithFinalBinding.i] of [class hs.ddif.core.AssistedInjectionTest$ProducerWithFinalBinding$")
      .hasMessageEndingWith("] cannot be final")
      .hasNoCause();
  }

  public static class Product {
    final String input;

    @Inject
    public Product(@Argument String input) {
      this.input = input;
    }
  }

  @Assisted
  public static abstract class ProducerWithFinalBinding {
    @Inject final int i = 2;  // bad because final

    public abstract Product create(String input);
  }

  @Test
  public void shouldRejectProducerProducingProductWithFinalBinding() {
    injector.registerInstance("bla");

    assertThatThrownBy(() -> injector.register(ProducerWithBadBindings.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.AssistedInjectionTest$ProducerWithBadBindings] or its product [class hs.ddif.core.AssistedInjectionTest$BadProduct] could not be bound")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Field [final int hs.ddif.core.AssistedInjectionTest$BadProduct.x] of [class hs.ddif.core.AssistedInjectionTest$BadProduct] cannot be final")
      .hasNoCause();
  }

  public static class BadProduct {
    final String input;
    @Inject final int x = 5;

    @Inject
    public BadProduct(@Argument String input) {
      this.input = input;
    }
  }

  @Assisted
  public static abstract class ProducerWithBadBindings {
    @Inject String s;

    public abstract BadProduct create(String input);
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

  @Assisted
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

  @Assisted
  public interface TestAssistedSampleFactory {
    TestAssistedSample create(@Argument("interval") Integer interval);
  }

  @Assisted
  public static abstract class TestAssistedAbstractSampleFactory {
    @Inject private ValueSupplier valueSupplier;

    public TestAssistedAbstractSample create(int a, int b) {
      return create(valueSupplier.get() * a + b, 3.0);
    }

    public abstract TestAssistedAbstractSample create(@Argument("interval") int interval, @Argument("factor") Double factor);
  }

  @Assisted
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
    @Inject private @Green Supplier<List<Integer>> numbers;
    @Inject private @Red Set<Integer> multipliers;
    @Inject @Argument private int offset;

    @Inject
    public ComplicatedProduct() {}

    public int calculate() {
      int sum = numbers.get().stream().mapToInt(Integer::intValue).sum();

      return sum * multipliers.stream().reduce((a, b) -> a * b).get() + offset;
    }
  }

  @Assisted
  interface ComplicatedProductFactory {
    ComplicatedProduct create(int offset);
  }
}
