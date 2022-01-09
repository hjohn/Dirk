package hs.ddif.core.config.standard;

import hs.ddif.annotations.Argument;
import hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplate.Context;
import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.bind.BindingException;
import hs.ddif.core.inject.bind.BindingProvider;
import hs.ddif.core.inject.injectable.ClassInjectableFactoryTemplate.TypeAnalysis;
import hs.ddif.core.inject.injectable.InjectableFactories;
import hs.ddif.core.inject.injectable.InstanceInjectableFactory;
import hs.ddif.core.inject.injectable.ResolvableInjectable;
import hs.ddif.core.inject.instantiation.InstanceCreationFailure;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.inject.instantiation.MultipleInstances;
import hs.ddif.core.inject.instantiation.NoSuchInstance;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.Key;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

import java.lang.reflect.Type;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AssistedClassInjectableFactoryTemplateTest {
  private BindingProvider bindingProvider = new BindingProvider(DefaultBinding::new);
  private AssistedClassInjectableFactoryTemplate extension = new AssistedClassInjectableFactoryTemplate(bindingProvider, ResolvableInjectable::new);

  @Test
  void shouldConstructSimpleFactory() {
    ResolvableInjectable injectable = create(FactoryA.class);

    assertThat((Class<?>)injectable.getType()).matches(FactoryA.class::isAssignableFrom);
    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(
      new Key(String.class, List.of(Annotations.of(Red.class))), new Key(String.class, List.of(Annotations.of(Green.class)))
    );
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.getScope()).isEqualTo(Annotations.of(Singleton.class));
  }

  @Test
  void shouldConstructFactoryWithQualifiers() {
    ResolvableInjectable injectable = create(FactoryB.class);

    assertThat((Class<?>)injectable.getType()).matches(FactoryB.class::isAssignableFrom);
    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(
      new Key(String.class, List.of(Annotations.of(Red.class))), new Key(String.class, List.of(Annotations.of(Green.class)))
    );
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class));
    assertThat(injectable.getScope()).isEqualTo(Annotations.of(Singleton.class));
  }

  @Test
  void shouldConstructFactoryWithDependencies() {
    ResolvableInjectable injectable = create(FactoryC.class);

    assertThat((Class<?>)injectable.getType()).matches(FactoryC.class::isAssignableFrom);
    assertThat(injectable.getBindings()).extracting(Binding::getKey).containsExactlyInAnyOrder(
      new Key(A.class), new Key(B.class), new Key(C.class), new Key(String.class, List.of(Annotations.of(Red.class))), new Key(String.class, List.of(Annotations.of(Green.class)))
    );
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.getScope()).isEqualTo(Annotations.of(Singleton.class));
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
    assertThat(extension.analyze(BadFactoryE.class).getUnsuitableReason(BadFactoryE.class)).isEqualTo("Factory method cannot return an abstract type: public abstract hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryA hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryE.create(java.lang.Integer,java.lang.Integer) in: interface hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$BadFactoryE");
  }

  @Test
  void shouldRejectWhenFactoryAndProductHaveDifferentNumberOfArguments() {
    assertThatThrownBy(() -> create(ContradictingFactoryA.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Factory method has wrong number of arguments: [public abstract hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$Product hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$ContradictingFactoryA.create(java.lang.Integer)] should have 2 argument(s) of types: {x=class java.lang.Integer, y=class java.lang.Integer}")
      .hasNoCause();
  }

  @Test
  void shouldRejectWhenFactoryIsMissingRequiredParameters() {
    assertThatThrownBy(() -> create(ContradictingFactoryB.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Factory method is missing required argument: [public abstract hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$Product hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$ContradictingFactoryB.create(java.lang.Integer,java.lang.Integer)] is missing required argument with name: z")
      .hasNoCause();
  }

  @Test
  void shouldRejectWhenFactoryParameterIsOfWrongType() {
    assertThatThrownBy(() -> create(ContradictingFactoryC.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Factory method has argument of wrong type: [public abstract hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$Product hs.ddif.core.config.standard.AssistedClassInjectableFactoryTemplateTest$ContradictingFactoryC.create(java.lang.Integer,java.lang.String)] has argument [java.lang.String y] with name 'y' that should be of type [class java.lang.Integer] but was: class java.lang.String")
      .hasNoCause();
  }

  @Test
  void shouldInstantiateTypeViaFactory() throws NoSuchInstance, MultipleInstances, InstanceCreationFailure, OutOfScopeException {
    InjectableStore<ResolvableInjectable> store = new InjectableStore<>();
    Instantiator instantiator = new DefaultInstantiator(store, new AutoDiscoveringGatherer(false, List.of(), InjectableFactories.forClass()));

    store.put(new InstanceInjectableFactory(ResolvableInjectable::new).create("Red", Annotations.of(Red.class)));
    store.put(new InstanceInjectableFactory(ResolvableInjectable::new).create("Green", Annotations.of(Green.class)));
    store.put(create(FactoryA.class));

    FactoryA instance = (FactoryA)instantiator.getInstance(new Key(FactoryA.class));

    Product product1 = instance.create(5, 6);
    Product product2 = instance.create(5, 6);
    Product product3 = instance.create(6, 7);

    assertThat(product1.result).isEqualTo("Red Green x=5 y=6");
    assertThat(product2.result).isEqualTo("Red Green x=5 y=6");
    assertThat(product3.result).isEqualTo("Red Green x=6 y=7");
    assertThat(product1).isNotEqualTo(product2);
    assertThat(product1).isNotEqualTo(product3);
  }

  private ResolvableInjectable create(Type type) {
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

    @Inject @Green Provider<String> dependencyGreen;
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

  @Red
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
