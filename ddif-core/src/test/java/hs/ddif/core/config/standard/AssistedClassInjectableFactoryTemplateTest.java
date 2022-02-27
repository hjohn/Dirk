package hs.ddif.core.config.standard;

import hs.ddif.annotations.Argument;
import hs.ddif.core.config.ConfigurableAnnotationStrategy;
import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplate.Context;
import hs.ddif.core.definition.ClassInjectableFactoryTemplate.TypeAnalysis;
import hs.ddif.core.definition.DefinitionException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.InjectableFactories;
import hs.ddif.core.definition.bind.AnnotationStrategy;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.definition.bind.BindingException;
import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.inject.store.InjectableStore;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.ScopeResolverManager;
import hs.ddif.core.store.Key;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AssistedClassInjectableFactoryTemplateTest {
  private static final ScopeResolver SINGLETON_SCOPE_RESOLVER = new SingletonScopeResolver(Annotations.of(Singleton.class));
  private static final ScopeResolverManager SCOPE_RESOLVER_MANAGER = new ScopeResolverManager(SINGLETON_SCOPE_RESOLVER);
  private static final AnnotationStrategy ANNOTATION_STRATEGY = new ConfigurableAnnotationStrategy(Annotations.of(Inject.class), Annotations.of(Qualifier.class), Annotations.of(Scope.class));

  private BindingProvider bindingProvider = new BindingProvider(ANNOTATION_STRATEGY);
  private AssistedClassInjectableFactoryTemplate extension = new AssistedClassInjectableFactoryTemplate(bindingProvider, new DefaultInjectableFactory(SCOPE_RESOLVER_MANAGER, ANNOTATION_STRATEGY), Annotations.of(Inject.class), Supplier.class, Supplier::get);

  @Test
  void shouldConstructSimpleFactory() throws BindingException {
    Injectable injectable = create(FactoryA.class);

    assertThat((Class<?>)injectable.getType()).matches(FactoryA.class::isAssignableFrom);
    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(
      new Key(TypeUtils.parameterize(Supplier.class, String.class), List.of(Annotations.of(Red.class))),
      new Key(TypeUtils.parameterize(Supplier.class, TypeUtils.parameterize(Supplier.class, String.class)), List.of(Annotations.of(Green.class)))
    );
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.getScopeResolver()).isEqualTo(SCOPE_RESOLVER_MANAGER.getScopeResolver(null));
  }

  @Test
  void shouldConstructFactoryWithQualifiers() throws BindingException {
    Injectable injectable = create(FactoryB.class);

    assertThat((Class<?>)injectable.getType()).matches(FactoryB.class::isAssignableFrom);
    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(
      new Key(TypeUtils.parameterize(Supplier.class, String.class), List.of(Annotations.of(Red.class))),
      new Key(TypeUtils.parameterize(Supplier.class, TypeUtils.parameterize(Supplier.class, String.class)), List.of(Annotations.of(Green.class)))
    );
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class));
    assertThat(injectable.getScopeResolver()).isEqualTo(SINGLETON_SCOPE_RESOLVER);
  }

  @Test
  void shouldConstructFactoryWithDependencies() throws BindingException {
    Injectable injectable = create(FactoryC.class);

    assertThat((Class<?>)injectable.getType()).matches(FactoryC.class::isAssignableFrom);
    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(
      new Key(A.class),
      new Key(B.class),
      new Key(C.class),
      new Key(TypeUtils.parameterize(Supplier.class, String.class), List.of(Annotations.of(Red.class))),
      new Key(TypeUtils.parameterize(Supplier.class, TypeUtils.parameterize(Supplier.class, String.class)), List.of(Annotations.of(Green.class)))
    );
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.getScopeResolver()).isEqualTo(SCOPE_RESOLVER_MANAGER.getScopeResolver(null));
  }

  @Test
  void shouldConstructFactoryWithGenericMethodAndGenericResult() throws BindingException, InstanceCreationFailure {
    Injectable injectable = create(FactoryD.class);

    assertThat((Class<?>)injectable.getType()).matches(FactoryD.class::isAssignableFrom);
    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(
      new Key(TypeUtils.parameterize(Supplier.class, Integer.class))
    );
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.getScopeResolver()).isEqualTo(SINGLETON_SCOPE_RESOLVER);

    /*
     * Test if factory actually works
     */

    Supplier<Integer> integerProvider = () -> 15;
    FactoryD factory = (FactoryD)injectable.createInstance(List.of(new Injection(injectable.getBindings().get(0).getAccessibleObject(), integerProvider)));

    GenericProduct<Car> product = factory.create(Car.class, "Meryota");
    Car car = product.getProduct();

    assertThat(car.getBrand()).isEqualTo("Meryota");
    assertThat(car.getWheels()).isEqualTo(15);
  }

  @Test
  void shouldFailPreconditionWhenNoSingleAbstractMethodIsFound() {
    assertThat(extension.analyze(BadFactoryA.class).getUnsuitableReason(BadFactoryA.class)).isEqualTo("Type must have a single abstract method to qualify for assisted injection: interface hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryA");
    assertThat(extension.analyze(BadFactoryB.class).getUnsuitableReason(BadFactoryB.class)).isEqualTo("Type must have a single abstract method to qualify for assisted injection: interface hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryB");
  }

  @Test
  void shouldFailPreconditionWhenReturnTypeIsPrimitive() {
    assertThat(extension.analyze(BadFactoryC.class).getUnsuitableReason(BadFactoryC.class)).isEqualTo("Factory method cannot return a primitive type: public abstract double hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryC.create(java.lang.Integer,java.lang.Integer) in: interface hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryC");
    assertThat(extension.analyze(BadFactoryD.class).getUnsuitableReason(BadFactoryD.class)).isEqualTo("Factory method cannot return a primitive type: public abstract void hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryD.create(java.lang.Integer,java.lang.Integer) in: interface hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryD");
  }

  @Test
  void shouldFailPreconditionWhenReturnTypeIsAbstract() {
    ParameterizedType connectionProvider = TypeUtils.parameterize(Function.class, String.class, Connection.class);

    assertThat(extension.analyze(connectionProvider).getUnsuitableReason(connectionProvider)).isEqualTo("Factory method cannot return an abstract type: public abstract java.lang.Object java.util.function.Function.apply(java.lang.Object) in: java.util.function.Function<java.lang.String, java.sql.Connection>");
    assertThat(extension.analyze(BadFactoryE.class).getUnsuitableReason(BadFactoryE.class)).isEqualTo("Factory method cannot return an abstract type: public abstract hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryA hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryE.create(java.lang.Integer,java.lang.Integer) in: interface hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryE");
  }

  @Test
  void shouldFailPreconditionWhenFactoryMethodHasNoParameters() {
    ParameterizedType connectionProvider = TypeUtils.parameterize(Supplier.class, Connection.class);

    assertThat(extension.analyze(connectionProvider).getUnsuitableReason(connectionProvider)).isEqualTo("Factory method must have at least one parameter to qualify for assisted injection: java.util.function.Supplier<java.sql.Connection>");
    assertThat(extension.analyze(BadFactoryE.class).getUnsuitableReason(BadFactoryE.class)).isEqualTo("Factory method cannot return an abstract type: public abstract hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryA hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryE.create(java.lang.Integer,java.lang.Integer) in: interface hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryE");
  }

  @Test
  void shouldRejectWhenFactoryAndProductHaveDifferentNumberOfArguments() {
    assertThatThrownBy(() -> create(ContradictingFactoryA.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$Product hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$ContradictingFactoryA.create(java.lang.Integer)] should have 2 argument(s) of types: {x=class java.lang.Integer, y=class java.lang.Integer}")
      .hasNoCause();
  }

  @Test
  void shouldRejectWhenFactoryIsMissingRequiredParameters() {
    assertThatThrownBy(() -> create(ContradictingFactoryB.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$Product hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$ContradictingFactoryB.create(java.lang.Integer,java.lang.Integer)] is missing required argument with name: z")
      .hasNoCause();
  }

  @Test
  void shouldRejectWhenFactoryParameterIsOfWrongType() {
    assertThatThrownBy(() -> create(ContradictingFactoryC.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [public abstract hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$Product hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$ContradictingFactoryC.create(java.lang.Integer,java.lang.String)] has argument [java.lang.String y] with name 'y' that should be of type [class java.lang.Integer] but was: class java.lang.String")
      .hasNoCause();
  }

  @Test
  void shouldInstantiateTypeViaFactory() throws BindingException {
    DefaultInstanceResolver instanceResolver = InstanceResolvers.create();
    InjectableStore store = instanceResolver.getStore();
    InjectableFactories injectableFactories = new InjectableFactories();

    store.putAll(List.of(
      injectableFactories.forInstance().create("Red", Annotations.of(Red.class)),
      injectableFactories.forInstance().create("Green", Annotations.of(Green.class)),
      create(FactoryA.class)
    ));

    FactoryA instance = instanceResolver.getInstance(FactoryA.class);

    Product product1 = instance.create(5, 6);
    Product product2 = instance.create(5, 6);
    Product product3 = instance.create(6, 7);

    assertThat(product1.result).isEqualTo("Red Green x=5 y=6");
    assertThat(product2.result).isEqualTo("Red Green x=5 y=6");
    assertThat(product3.result).isEqualTo("Red Green x=6 y=7");
    assertThat(product1).isNotEqualTo(product2);
    assertThat(product1).isNotEqualTo(product3);
  }

  private Injectable create(Type type) throws BindingException {
    TypeAnalysis<Context> analysis = extension.analyze(type);

    if(analysis.isNegative()) {
      throw new IllegalArgumentException(analysis.getUnsuitableReason(type));
    }

    return extension.create(analysis);
  }

  public interface FactoryA {
    Product create(Integer x, Integer y);
  }

  public static class Product {
    private final String dependencyRed;
    private final Integer x;

    @Inject @Green Supplier<String> dependencyGreen;
    @Inject @Argument private Integer y;

    String result;

    @Inject
    Product(@Red String dependency, @Argument Integer x) {
      this.dependencyRed = dependency;
      this.x = x;
    }

    @PostConstruct
    private void postConstruct() {
      this.result = dependencyRed + " " + dependencyGreen.get() + " x=" + x + " y=" + y;
    }
  }

  @Red @Singleton
  public interface FactoryB {
    Product create(@Argument("x") Integer a, @Argument("y") Integer b);

    default Product create(Integer x) {
      return create(x, 20);
    }
  }

  public static abstract class FactoryC {
    @Inject A a;

    @Inject
    public FactoryC(@SuppressWarnings("unused") B b, @SuppressWarnings("unused") C c) {
    }

    public abstract Product create(Integer x, Integer y);
  }

  @Singleton
  public interface FactoryD {
    <X> GenericProduct<X> create(Class<X> cls, String brand);
  }

  static class Car {
    private String brand;
    private int wheels;

    public Car(String brand, int wheels) {
      this.brand = brand;
      this.wheels = wheels;
    }

    public String getBrand() {
      return brand;
    }

    public int getWheels() {
      return wheels;
    }
  }

  static class GenericProduct<T> {
    @Inject int x;

    Class<T> cls;
    String brand;

    @Inject
    GenericProduct(@Argument Class<T> cls, @Argument String brand) {
      this.cls = cls;
      this.brand = brand;
    }

    T getProduct() {
      try {
        return cls.getDeclaredConstructor(String.class, int.class).newInstance(brand, x);
      }
      catch(Exception e) {
        throw new IllegalStateException(e);
      }
    }
  }

  static class A {}
  static class B {}
  static class C {}

  interface BadFactoryA {}

  interface BadFactoryB {
    A createA(Integer x, Integer y);
    B createB(Integer x, Integer y);
  }

  interface BadFactoryC {
    double create(Integer x, Integer y);
  }

  interface BadFactoryD {
    void create(Integer x, Integer y);
  }

  interface BadFactoryE {
    BadFactoryA create(Integer x, Integer y);
  }

  interface ContradictingFactoryA {
    Product create(Integer x);
  }

  interface ContradictingFactoryB {
    Product create(Integer x, @Argument("z") Integer d);
  }

  interface ContradictingFactoryC {
    Product create(Integer x, String y);
  }
}
