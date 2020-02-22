package hs.ddif.core;

import hs.ddif.core.bind.Parameter;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ConstructionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProducerAssistedInjectionTest {
  private Injector injector = new Injector();

  @Test
  public void shouldAcceptProducerInterface() throws BeanResolutionException {
    for(int i = 0; i < 2; i++) {
      injector.register(TestService.class);
      injector.register(TestAssistedSample.class);

      try {
        injector.getInstance(TestAssistedSample.class);
        fail();
      }
      catch(ConstructionException e) {
      }

      TestAssistedSampleFactory factory = injector.getInstance(TestAssistedSampleFactory.class);

      TestAssistedSample sample = factory.create(100);

      assertEquals(100, (int)sample.interval);
      assertEquals(TestService.class, sample.testService.getClass());

      injector.remove(TestAssistedSample.class);
      injector.remove(TestService.class);
    }
  }

  @Test
  public void shouldAcceptProducerAbstractClass() throws BeanResolutionException {
    injector.register(ValueSupplier.class);
    injector.register(TestService.class);
    injector.register(TestAssistedAbstractSample.class);

    try {
      injector.getInstance(TestAssistedAbstractSample.class);
      fail();
    }
    catch(ConstructionException e) {
    }

    TestAssistedAbstractSampleFactory factory = injector.getInstance(TestAssistedAbstractSampleFactory.class);

    TestAssistedAbstractSample sample = factory.create(2, 3);

    assertEquals(44 * 2 + 3, (int)sample.interval);
    assertEquals(TestService.class, sample.testService.getClass());
  }

  @Test
  public void registerShouldRejectProducerWithMultipleAbstractMethods() {
    try {
      injector.register(TestAssistedInjectionClassWithProducerWithMultipleAbtractMethods.class);
      fail("Expected BindingException");
    }
    catch(BindingException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("multiple abstract methods"));
    }
  }

  @Producer(ProducerWithMultipleAbtractMethods.class)
  public static class TestAssistedInjectionClassWithProducerWithMultipleAbtractMethods {
  }

  public abstract static class ProducerWithMultipleAbtractMethods {
    public abstract TestAssistedInjectionClassWithProducerWithMultipleAbtractMethods create();
    public abstract TestAssistedInjectionClassWithProducerWithMultipleAbtractMethods create2();
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectReturnType() {
    try {
      injector.register(TestAssistedInjectionClassWithProducerWithIncorrectReturnType.class);
      fail("Expected BindingException");
    }
    catch(BindingException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("return type that does not match"));
    }
  }

  @Producer(ProducerWithIncorrectReturnType.class)
  public static class TestAssistedInjectionClassWithProducerWithIncorrectReturnType {
  }

  public interface ProducerWithIncorrectReturnType {
    public abstract String create();
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameterCount() {
    try {
      injector.register(TestAssistedInjectionClassWithProducerWithIncorrectParameterCount.class);
      fail("Expected BindingException");
    }
    catch(BindingException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("wrong number of parameters"));
    }
  }

  @Producer(ProducerWithIncorrectParameterCount.class)
  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameterCount {
  }

  public interface ProducerWithIncorrectParameterCount {
    public abstract TestAssistedInjectionClassWithProducerWithIncorrectParameterCount create(Double notNeeded);
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameterType() {
    try {
      injector.register(TestAssistedInjectionClassWithProducerWithIncorrectParameterType.class);
      fail("Expected BindingException");
    }
    catch(BindingException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("that should be of type"));
    }
  }

  @Producer(ProducerWithIncorrectParameterType.class)
  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameterType {
    @Inject @Parameter private String text;
  }

  public interface ProducerWithIncorrectParameterType {
    public abstract TestAssistedInjectionClassWithProducerWithIncorrectParameterType create(@Parameter("text") Double text);
  }

  @Test
  public void registerShouldRejectProducerWithIncorrectParameter() {
    try {
      injector.register(TestAssistedInjectionClassWithProducerWithIncorrectParameter.class);
      fail("Expected BindingException");
    }
    catch(BindingException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Factory method is missing required parameter."));
    }
  }

  @Producer(ProducerWithIncorrectParameter.class)
  public static class TestAssistedInjectionClassWithProducerWithIncorrectParameter {
    @Inject @Parameter private String text;
  }

  public interface ProducerWithIncorrectParameter {
    public abstract TestAssistedInjectionClassWithProducerWithIncorrectParameter create(@Parameter("incorrect") String incorrect);
  }

  @Test
  public void registerShouldRejectProducerWithMissingParameterName() {
    try {
      injector.register(TestAssistedInjectionClassWithProducerWithMissingParameterName.class);
      fail("Expected BindingException");
    }
    catch(BindingException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Missing parameter name."));
    }
  }

  @Producer(ProducerWithMissingParameterName.class)
  public static class TestAssistedInjectionClassWithProducerWithMissingParameterName {
    @Inject @Parameter private String text;
  }

  public interface ProducerWithMissingParameterName {
    public abstract TestAssistedInjectionClassWithProducerWithMissingParameterName create(String incorrect);
  }

  @Test
  public void registerShouldRejectTargetWithConstructorWithMissingParameterName() {
    try {
      injector.register(TestAssistedInjectionTargetWithConstructorWithMissingParameterName.class);
      fail("Expected BindingException");
    }
    catch(BindingException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Missing parameter name."));
    }
  }

  @Producer(ProducerForTargetWithConstructor.class)
  public static class TestAssistedInjectionTargetWithConstructorWithMissingParameterName {
    String text;

    @Inject
    public TestAssistedInjectionTargetWithConstructorWithMissingParameterName(@Parameter String text) {
      this.text = text;
    }
  }

  public interface ProducerForTargetWithConstructor {
    public abstract TestAssistedInjectionTargetWithConstructorWithMissingParameterName create(@Parameter("text") String text);
  }

  @Test
  public void registerShouldAcceptProducerWhichNeedsNoFurtherConstruction() throws BeanResolutionException {
    injector.register(TestTargetWithAutonomousProducer.class);

    AutonomousProducer producer = injector.getInstance(AutonomousProducer.class);

    TestTargetWithAutonomousProducer target = producer.create(5);

    assertEquals("abc5", target.text);
  }

  @Producer(AutonomousProducer.class)
  public static class TestTargetWithAutonomousProducer {
    String text;

    @Inject
    public TestTargetWithAutonomousProducer(@Parameter String text) {
      this.text = text;
    }
  }

  public static class AutonomousProducer {
    public TestTargetWithAutonomousProducer create(int extension) {
      return new TestTargetWithAutonomousProducer("abc" + extension);
    }
  }

  @Test
  public void registerShouldAcceptAbstractProducerWithNoParametersWhichNeedsNoFurtherConstruction() throws BeanResolutionException {
    injector.register(TestTargetWithAbstactProducerAndNoParameters.class);

    AbstractProducerWithNoParameters producer = injector.getInstance(AbstractProducerWithNoParameters.class);

    TestTargetWithAbstactProducerAndNoParameters target = producer.create();

    assertEquals("abc2", target.text);
  }

  @Producer(AbstractProducerWithNoParameters.class)
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
  public void registerShouldAcceptInterfaceProducerWithNoParametersWhichNeedsNoFurtherConstruction() throws BeanResolutionException {
    injector.register(TestTargetWithInterfaceProducerAndNoParameters.class);

    InterfaceProducerWithNoParameters producer = injector.getInstance(InterfaceProducerWithNoParameters.class);

    TestTargetWithInterfaceProducerAndNoParameters target = producer.create();

    assertEquals("abc2", target.text);
  }

  @Producer(InterfaceProducerWithNoParameters.class)
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

  @Producer(TestAssistedSampleFactory.class)
  public static class TestAssistedSample {
    @Inject public TestService testService;
    @Inject @Parameter public Integer interval;
  }

  @Producer(TestAssistedAbstractSampleFactory.class)
  public static class TestAssistedAbstractSample {
    @Inject public TestService testService;
    @Inject @Parameter public Integer interval;
    @Inject @Parameter public Double factor;
  }

  public interface TestAssistedSampleFactory {
    TestAssistedSample create(@Parameter("interval") Integer interval);
  }

  public static abstract class TestAssistedAbstractSampleFactory {
    @Inject private ValueSupplier valueSupplier;

    public TestAssistedAbstractSample create(int a, int b) {
      return create(valueSupplier.get() * a + b, 3.0);
    }

    public abstract TestAssistedAbstractSample create(@Parameter("interval") Integer interval, @Parameter("factor") Double factor);
  }

  public static class TestService {
  }

  @Singleton
  public static class ValueSupplier {
    public int get() {
      return 44;
    }
  }
}
