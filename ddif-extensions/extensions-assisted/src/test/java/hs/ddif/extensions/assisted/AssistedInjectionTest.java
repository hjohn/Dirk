package hs.ddif.extensions.assisted;

import hs.ddif.annotations.Argument;
import hs.ddif.annotations.Assisted;
import hs.ddif.api.Injector;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.instantiation.NoSuchInstanceException;
import hs.ddif.api.util.Annotations;
import hs.ddif.api.util.Types;
import hs.ddif.core.InjectorBuilder;
import hs.ddif.core.definition.BindingException;
import hs.ddif.core.inject.store.UnresolvableDependencyException;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

public class AssistedInjectionTest {
  private static final Inject INJECT = Annotations.of(Inject.class);
  private static final AssistedAnnotationStrategy<?> ASSISTED_ANNOTATION_STRATEGY = new ConfigurableAssistedAnnotationStrategy<>(Assisted.class, Argument.class, AssistedInjectionTest::extractArgumentName, INJECT, Provider.class, Provider::get);

  private Injector injector = InjectorBuilder.builder()
    .manual()
    .discoveryExtensions(context -> List.of(new AssistedDiscoveryExtension(context.bindingProvider, context.lifeCycleCallbacksFactory, ASSISTED_ANNOTATION_STRATEGY)))
    .build();

  private static String extractArgumentName(AnnotatedElement element) {
    Argument annotation = element.getAnnotation(Argument.class);

    return annotation == null ? null : annotation.value();
  }

  @Test
  public void shouldAcceptAndRemoveProducerInterface() throws Exception {
    for(int i = 0; i < 2; i++) {
      injector.register(TestService.class);
      injector.register(TestAssistedSampleFactory.class);

      // The Product of the Factory should not be registered with the Injector:
      assertThatThrownBy(() -> injector.getInstance(TestAssistedSample.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasMessage("No such instance: [hs.ddif.extensions.assisted.AssistedInjectionTest$TestAssistedSample]")
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
  public void shouldAcceptAndCreateProducerAbstractClass() throws Exception {
    injector.register(ValueSupplier.class);
    injector.register(TestService.class);
    injector.register(TestAssistedAbstractSampleFactory.class);

    // The Product of the Factory should not be registered with the Injector:
    assertThatThrownBy(() -> injector.getInstance(TestAssistedAbstractSample.class))
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasMessage("No such instance: [hs.ddif.extensions.assisted.AssistedInjectionTest$TestAssistedAbstractSample]")
      .hasNoCause();

    TestAssistedAbstractSampleFactory factory = injector.getInstance(TestAssistedAbstractSampleFactory.class);

    TestAssistedAbstractSample sample = factory.create(2, 3);

    assertEquals(44 * 2 + 3, sample.interval);
    assertEquals(TestService.class, sample.testService.getClass());
  }

  @Test
  public void shouldAcceptAndCreateProducerAbstractClassWithConstructor() throws Exception {
    injector.register(ValueSupplier.class);
    injector.register(TestService.class);
    injector.register(TestAssistedAbstractSampleFactoryWithConstructor.class);

    // The Product of the Factory should not be registered with the Injector:
    assertThatThrownBy(() -> injector.getInstance(TestAssistedAbstractSample.class))
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasMessage("No such instance: [hs.ddif.extensions.assisted.AssistedInjectionTest$TestAssistedAbstractSample]")
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
      .hasMessage("[class hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithMultipleAbtractMethods] cannot be abstract")
      .hasNoCause();
  }

  public abstract static class ProducerWithMultipleAbtractMethods {
    public abstract TestService create(String input);
    public abstract TestService create2(String input);
  }

  @Test
  public void registerShouldRejectProducerWithIllegalReturnType() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectReturnType.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.extensions.assisted.AssistedInjectionTest$Stuff hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithIncorrectReturnType.create(java.lang.String)] must not have unresolvable type variables to qualify for assisted injection")
      .hasNoCause();

    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectReturnType2.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.extensions.assisted.AssistedInjectionTest$Stuff hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithIncorrectReturnType2.create(java.lang.String)] must not have unresolvable type variables to qualify for assisted injection")
      .hasNoCause();
  }

  @Assisted
  public static class Stuff<T> {
    T field;
  }

  interface ProducerWithIncorrectReturnType<T> {
    Stuff<T> create(String input);
  }

  interface ProducerWithIncorrectReturnType2 {
    <T> Stuff<T> create(String input);
  }

  @Test
  public void registerShouldRejectProducerWithAbstractReturnType() {
    assertThatThrownBy(() -> injector.register(ProducerWithAbstractReturnType.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.extensions.assisted.AssistedInjectionTest$AbstractProduct hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithAbstractReturnType.create(java.lang.String)] must return a concrete type to qualify for assisted injection")
      .hasNoCause();
  }

  @Assisted
  public static abstract class AbstractProduct {
  }

  public interface ProducerWithAbstractReturnType {
    AbstractProduct create(String input);
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameterCount() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectParameterCount.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.extensions.assisted.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameterCount hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithIncorrectParameterCount.create(java.lang.Double)] should have 0 argument(s) of types: {}")
      .hasNoCause();
  }

  @Assisted
  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameterCount {
    @Inject
    public TestAssistedInjectionClassWithProducerWithIncorrectParameterCount() {}
  }

  public interface ProducerWithIncorrectParameterCount {
    TestAssistedInjectionClassWithProducerWithIncorrectParameterCount create(Double notNeeded);
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameterType() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectParameterType.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.extensions.assisted.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameterType hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithIncorrectParameterType.create(java.lang.Double)] has argument [java.lang.Double text] with name 'text' that should be of type [class java.lang.String] but was: class java.lang.Double")
      .hasNoCause();
  }

  @Assisted
  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameterType {
    @Inject @Argument private String text;

    @Inject
    public TestAssistedInjectionClassWithProducerWithIncorrectParameterType() {}
  }

  public interface ProducerWithIncorrectParameterType {
    TestAssistedInjectionClassWithProducerWithIncorrectParameterType create(@Argument("text") Double text);
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameter() {
    assertThatThrownBy(() -> injector.register(ProducerWithIncorrectParameter.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.extensions.assisted.AssistedInjectionTest$TestAssistedInjectionClassWithProducerWithIncorrectParameter hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithIncorrectParameter.create(java.lang.String)] is missing required argument with name: incorrect")
      .hasNoCause();
  }

  @Assisted
  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameter {
    @Inject @Argument private String text;

    @Inject
    public TestAssistedInjectionClassWithProducerWithIncorrectParameter() {}
  }

  public interface ProducerWithIncorrectParameter {
    TestAssistedInjectionClassWithProducerWithIncorrectParameter create(@Argument("incorrect") String incorrect);
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
    TestAssistedInjectionClassWithProducerWithMissingParameterName create(String incorrect);
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
    TestAssistedInjectionTargetWithConstructorWithMissingParameterName create(@Argument("text") String text);
  }

  @Test
  public void registerShouldMatchArgumentsByTypeIfProducerNotAnnotated() throws Exception {
    injector.register(ProducerForProductWithMismatchingArgumentNames.class);
  }

  @Assisted
  public static class ProductWithMismatchingArgumentNames {
    @Inject @Argument String s;
    @Inject @Argument double x;
  }

  interface ProducerForProductWithMismatchingArgumentNames {
    ProductWithMismatchingArgumentNames create(String t, double y);
  }

  @Test
  public void registerShouldAcceptProducerWhichNeedsNoFurtherConstruction() throws Exception {
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
  public void registerShouldAcceptAbstractProducerWithNoParameters() throws Exception {
    injector.register(AbstractProducerWithNoParameters.class);

    assertNotNull(injector.getInstance(AbstractProducerWithNoParameters.class).create());
  }

  @Assisted
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
  public void registerShouldAcceptInterfaceProducerWithNoParameters() throws Exception {
    injector.register(InterfaceProducerWithNoParameters.class);

    assertNotNull(injector.getInstance(InterfaceProducerWithNoParameters.class).create());
  }

  @Test
  public void dependenciesOfProductShouldBeCheckedToBeInStore() {
    // Registering TestAssistedSampleFactory should fail because the product TestAssistedSample
    // requires TestService which was not registered yet.
    assertThatThrownBy(() -> injector.register(TestAssistedSampleFactory.class))
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasMessageStartingWith("Missing dependency [hs.ddif.extensions.assisted.AssistedInjectionTest$TestService] required for")
      .hasNoCause();
  }

  @Test
  public void shouldRejectProducerWithFinalBinding() throws Exception {
    injector.registerInstance(5);

    assertThatThrownBy(() -> injector.register(ProducerWithFinalBinding.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessageStartingWith("[class hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithFinalBinding")
      .hasMessageEndingWith("] could not be bound")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessageStartingWith("Field [final int hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithFinalBinding.i] of [class hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithFinalBinding$")
      .hasMessageEndingWith("] cannot be final")
      .hasNoCause();
  }

  @Assisted
  public static class Product {
    final String input;

    @Inject
    public Product(@Argument String input) {
      this.input = input;
    }
  }

  public static abstract class ProducerWithFinalBinding {
    @Inject final int i = 2;  // bad because final

    public abstract Product create(String input);
  }

  @Test
  public void shouldRejectProducerProducingProductWithFinalBinding() throws Exception {
    injector.registerInstance("bla");

    assertThatThrownBy(() -> injector.register(ProducerWithBadBindings.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.extensions.assisted.AssistedInjectionTest$BadProduct] could not be bound")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Field [final int hs.ddif.extensions.assisted.AssistedInjectionTest$BadProduct.x] of [class hs.ddif.extensions.assisted.AssistedInjectionTest$BadProduct] cannot be final")
      .hasNoCause();
  }

  @Assisted
  public static class BadProduct {
    final String input;
    @Inject final int x = 5;

    @Inject
    public BadProduct(@Argument String input) {
      this.input = input;
    }
  }

  public static abstract class ProducerWithBadBindings {
    @Inject String s;

    public abstract BadProduct create(String input);
  }

  @Test
  public void shouldRejectDuplicateArgumentNamesInProducer() {
    assertThatThrownBy(() -> injector.register(ProducerWithDuplicateArgumentNames.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.extensions.assisted.AssistedInjectionTest$ProductWithTwoArguments hs.ddif.extensions.assisted.AssistedInjectionTest$ProducerWithDuplicateArgumentNames.create(java.lang.String,java.lang.String)] has a duplicate argument name: a")
      .hasNoCause();
  }

  @Assisted
  public static class ProductWithTwoArguments {
    @Inject @Argument String a;
    @Inject @Argument String b;
  }

  interface ProducerWithDuplicateArgumentNames {
    ProductWithTwoArguments create(@Argument("a") String a, @Argument("a") String b);
  }

  @Test
  public void shouldRejectDuplicateArgumentNamesInProduct() {
    assertThatThrownBy(() -> injector.register(ProducerForProductWithDuplicateNames.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [java.lang.String hs.ddif.extensions.assisted.AssistedInjectionTest$ProductWithDuplicateNames.b] has a duplicate argument name: a")
      .hasNoCause();
  }

  @Assisted
  public static class ProductWithDuplicateNames {
    @Inject @Argument String a;
    @Inject @Argument("a") String b;
  }

  interface ProducerForProductWithDuplicateNames {
    ProductWithDuplicateNames create(String a, String b);
  }

  @Test
  public void shouldBePossibleToUseBiFunction() throws Exception {
    injector.register(TestService.class);

    ParameterizedType factoryType = Types.parameterize(BiFunction.class, Integer.class, Double.class, TestAssistedAbstractSample.class);

    injector.register(factoryType);

    BiFunction<Integer, Double, TestAssistedAbstractSample> instance = injector.getInstance(factoryType);

    TestAssistedAbstractSample sample = instance.apply(5, 4.5);

    assertThat(sample.interval).isEqualTo(5);
    assertThat(sample.factor).isEqualTo(4.5);
  }

  @Test
  public void shouldConstructComplicatedProduct() throws Exception {
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

  @Assisted
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

  @Assisted
  public static class TestAssistedSample {
    @Inject public TestService testService;
    @Inject @Argument public Integer interval;

    @Inject
    public TestAssistedSample() {}
  }

  @Assisted
  public static class TestAssistedAbstractSample {
    public TestService testService;
    @Inject @Argument public int interval;
    @Inject @Argument public Double factor;

    @Inject
    public TestAssistedAbstractSample(TestService testService) {
      this.testService = testService;
    }
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

  @Assisted
  public static class TestService {
  }

  @Singleton
  public static class ValueSupplier {
    public int get() {
      return 44;
    }
  }

  @Assisted
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
