package hs.ddif.core.inject.bind;

import hs.ddif.annotations.Opt;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.config.standard.DefaultBinding;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.inject.instantiation.NoSuchInstance;
import hs.ddif.core.store.Key;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BindingProviderTest {
  private static final Annotation RED = Annotations.of(Red.class);
  private static final Annotation GREEN = Annotations.of(Green.class);

  private BindingProvider bindingProvider = new BindingProvider(DefaultBinding::new);

  @Mock private Instantiator instantiator;

  @Test
  public void ofMembersShouldBindToGenericFieldInSubclass() throws Exception {
    List<Binding> bindings = bindingProvider.ofMembers(Subclass.class);

    assertThat(bindings).extracting(Binding::getAccessibleObject, Binding::getKey).containsExactlyInAnyOrder(
      Tuple.tuple(ClassWithGenericFields.class.getDeclaredField("fieldA"), new Key(String.class)),
      Tuple.tuple(ClassWithGenericFields.class.getDeclaredField("fieldB"), new Key(Integer.class))
    );
  }

  @Test
  public void ofMethodShouldCreateCorrectBindings() throws Exception {
    List<Binding> bindings = bindingProvider.ofMethod(Subclass.class.getDeclaredMethod("create", Integer.class, Double.class), Subclass.class);

    assertEquals(3, bindings.size());
    assertEquals(Integer.class, bindings.get(0).getKey().getType());
    assertEquals(Double.class, bindings.get(1).getKey().getType());
    assertEquals(Subclass.class, bindings.get(2).getKey().getType());
    assertEquals(Set.of(Annotations.of(Big.class)), bindings.get(0).getKey().getQualifiers());
    assertEquals(Set.of(), bindings.get(1).getKey().getQualifiers());
    assertEquals(Set.of(), bindings.get(2).getKey().getQualifiers());
  }

  @Test
  public void ofMethodShouldTakeNonStaticIntoAccount() throws Exception {
    assertThat(bindingProvider.ofMethod(MethodHolder.class.getDeclaredMethod("create", Double.class), MethodHolder.class))
      .extracting(Binding::getKey)
      .containsExactly(
        new Key(Double.class),
        new Key(MethodHolder.class)  // dependency on the declaring class as "create" is an instance method
      );
  }

  @Test
  public void ofMethodShouldTakeStaticIntoAccount() throws Exception {
    assertThat(bindingProvider.ofMethod(MethodHolder.class.getDeclaredMethod("createStatic", Double.class), MethodHolder.class))
      .extracting(Binding::getKey)
      .containsExactly(
        new Key(Double.class)
      );
  }

  @Test
  public void ofFieldShouldTakeNonStaticIntoAccount() throws Exception {
    assertThat(bindingProvider.ofField(FieldHolder.class.getDeclaredField("b"), FieldHolder.class))
      .extracting(Binding::getKey)
      .containsExactly(
        new Key(FieldHolder.class)
      );
  }

  @Test
  public void ofFieldShouldTakeStaticIntoAccount() throws NoSuchFieldException, SecurityException {
    assertThat(bindingProvider.ofField(FieldHolder.class.getDeclaredField("a"), FieldHolder.class))
      .isEmpty();
  }

  @Test
  public void getConstructorShouldFindInjectAnnotatedConstructor() throws Exception {
    assertThat(BindingProvider.getConstructor(ClassA.class))
      .isEqualTo(ClassA.class.getDeclaredConstructor(ClassB.class));
  }

  @Test
  public void getConstructorShouldFindDefaultConstructor() throws Exception {
    assertThat(BindingProvider.getConstructor(ClassB.class))
      .isEqualTo(ClassB.class.getConstructor());
  }

  @Test
  public void getConstructorShouldRejectClassWithoutPublicDefaultConstructor() {
    assertThatThrownBy(() -> BindingProvider.getConstructor(ClassC.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("[class hs.ddif.core.inject.bind.BindingProviderTest$ClassC] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
      .hasNoCause();
  }

  @Test
  public void getConstructorShouldRejectClassWithMultipleAnnotatedConstructors() {
    assertThatThrownBy(() -> BindingProvider.getConstructor(ClassD.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("[class hs.ddif.core.inject.bind.BindingProviderTest$ClassD] cannot have multiple Inject annotated constructors")
      .hasNoCause();
  }

  @Test
  public void ofConstructorShouldRejectNestedProvider() {
    assertThatThrownBy(() -> bindingProvider.ofConstructor(ClassE.class.getDeclaredConstructors()[0]))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Constructor [hs.ddif.core.inject.bind.BindingProviderTest$ClassE(javax.inject.Provider)] of [class hs.ddif.core.inject.bind.BindingProviderTest$ClassE] could not bind parameter 0 [javax.inject.Provider<javax.inject.Provider<java.lang.String>> provider]: Nested Provider not allowed: javax.inject.Provider<java.lang.String>")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("Nested Provider not allowed: javax.inject.Provider<java.lang.String>")
      .hasNoCause();
  }

  @Test
  public void ofMembersShouldRejectNestedProvider() {
    assertThatThrownBy(() -> bindingProvider.ofMembers(ClassE.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Field [javax.inject.Provider hs.ddif.core.inject.bind.BindingProviderTest$ClassE.x] of [class hs.ddif.core.inject.bind.BindingProviderTest$ClassE] could not be bound: Nested Provider not allowed: javax.inject.Provider<java.lang.String>")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("Nested Provider not allowed: javax.inject.Provider<java.lang.String>")
      .hasNoCause();
  }

  @Test
  public void ofMembersShouldRejectFinalField() {
    assertThatThrownBy(() -> bindingProvider.ofMembers(ClassF.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Field [final java.lang.String hs.ddif.core.inject.bind.BindingProviderTest$ClassF.x] of [class hs.ddif.core.inject.bind.BindingProviderTest$ClassF] cannot be final")
      .hasNoCause();
  }

  @Test
  public void ofConstructorAndMembersShouldFindAllBindings() throws Exception {
    List<Binding> bindings = bindingProvider.ofConstructorAndMembers(ClassA.class.getDeclaredConstructor(ClassB.class), ClassA.class);

    assertThat(bindings)
      .extracting(Binding::getKey, Binding::isOptional)
      .containsExactly(
        tuple(new Key(ClassB.class), false),
        tuple(new Key(String.class), false),
        tuple(new Key(Integer.class), false),
        tuple(new Key(TypeUtils.parameterize(List.class, Double.class)), true),
        tuple(new Key(TypeUtils.parameterize(Set.class, String.class)), true),
        tuple(new Key(TypeUtils.parameterize(List.class, Double.class), List.of(RED)), true),
        tuple(new Key(TypeUtils.parameterize(Set.class, String.class), List.of(RED)), true),
        tuple(new Key(TypeUtils.parameterize(List.class, Double.class), List.of(GREEN)), true),
        tuple(new Key(TypeUtils.parameterize(Set.class, String.class), List.of(GREEN)), true),
        tuple(new Key(Long.class), true),
        tuple(new Key(Short.class), false),
        tuple(new Key(Short.class), true)
      );

    ClassB classB = new ClassB();

    when(instantiator.getInstance(new Key(ClassB.class))).thenReturn(classB);
    when(instantiator.getInstance(new Key(String.class))).thenReturn("Hello");
    when(instantiator.getInstance(new Key(Integer.class))).thenReturn(5);
    when(instantiator.getInstances(new Key(Double.class))).thenReturn(List.of(1.2, 3.4));
    when(instantiator.getInstances(new Key(String.class))).thenReturn(List.of("a", "b"));
    when(instantiator.getInstances(new Key(Double.class, List.of(RED)))).thenReturn(List.of());
    when(instantiator.getInstances(new Key(String.class, List.of(RED)))).thenReturn(List.of());
    when(instantiator.getInstances(new Key(Double.class, List.of(GREEN)))).thenReturn(List.of());
    when(instantiator.getInstances(new Key(String.class, List.of(GREEN)))).thenReturn(List.of());
    when(instantiator.findInstance(new Key(Long.class))).thenReturn(null);

    assertThat(bindings.get(0).getValue(instantiator)).isEqualTo(classB);
    assertThat(bindings.get(1).getValue(instantiator)).isEqualTo("Hello");
    assertThat(bindings.get(2).getValue(instantiator)).isInstanceOfSatisfying(Provider.class, p -> {
      assertThat(p.get()).isEqualTo(5);
    });
    assertThat(bindings.get(3).getValue(instantiator)).isEqualTo(List.of(1.2, 3.4));
    assertThat(bindings.get(4).getValue(instantiator)).isEqualTo(Set.of("a", "b"));
    assertThat(bindings.get(5).getValue(instantiator)).isNull();
    assertThat(bindings.get(6).getValue(instantiator)).isNull();
    assertThat(bindings.get(7).getValue(instantiator)).isEqualTo(List.of());
    assertThat(bindings.get(8).getValue(instantiator)).isEqualTo(Set.of());
    assertThat(bindings.get(9).getValue(instantiator)).isNull();

    /*
     * Test optional provider functionality:
     */

    when(instantiator.findInstance(new Key(Short.class))).thenReturn(null);
    when(instantiator.getInstance(new Key(Short.class))).thenThrow(new NoSuchInstance(new Key(Short.class), List.of()));

    assertThat(bindings.get(10).getValue(instantiator)).isInstanceOfSatisfying(Provider.class, p -> {
      assertThatThrownBy(() -> p.get())
        .isExactlyInstanceOf(NoSuchInstanceException.class);
    });
    assertThat(bindings.get(11).getValue(instantiator)).isInstanceOfSatisfying(Provider.class, p -> {
      assertThat(p.get()).isNull();
    });

    reset(instantiator);
    when(instantiator.findInstance(new Key(Short.class))).thenReturn((short)2);
    when(instantiator.getInstance(new Key(Short.class))).thenReturn((short)3);

    assertThat(bindings.get(10).getValue(instantiator)).isInstanceOfSatisfying(Provider.class, p -> {
      assertThat(p.get()).isEqualTo((short)3);
    });
    assertThat(bindings.get(11).getValue(instantiator)).isInstanceOfSatisfying(Provider.class, p -> {
      assertThat(p.get()).isEqualTo((short)2);
    });
  }

  @SuppressWarnings("unused")
  private static class ClassWithGenericFields<A, B> {
    @Inject private A fieldA;
    @Inject private B fieldB;
  }

  private static class Subclass extends ClassWithGenericFields<String, Integer> {
    @SuppressWarnings("unused")
    public String create(@Big Integer dep1, Double dep2) {
      return null;
    }
  }

  public static class MethodHolder {
    public static String createStatic(Double x) {
      return "" + x;
    }

    public String create(Double x) {
      return "" + x;
    }
  }

  public static class FieldHolder {
    public static String a;

    public String b;
  }

  public static class ClassA {
    ClassB classB;

    @Inject String x;
    @Inject Provider<Integer> y;
    @Inject List<Double> doubles;
    @Inject Set<String> strings;
    @Inject @Opt @Red List<Double> optionalRedDoubles;
    @Inject @Opt @Red Set<String> optoinalRedStrings;
    @Inject @Green List<Double> emptyGreenDoubles;
    @Inject @Green Set<String> emptyGreenStrings;
    @Inject @Opt long z = 15;
    @Inject Provider<Short> p;
    @Inject @Opt Provider<Short> q;

    @Inject
    ClassA(ClassB classB) {
      this.classB = classB;
    }

    @SuppressWarnings("unused")
    ClassA(int a, int b, int c) {
    }
  }

  public static class ClassB {
  }

  public static class ClassC {
    ClassC() {}
  }

  public static class ClassD {
    @Inject
    ClassD() {}

    @Inject @SuppressWarnings("unused")
    ClassD(int a, int b, int c) {}
  }

  public static class ClassE {
    @Inject
    Provider<Provider<String>> x;

    @Inject
    ClassE(@SuppressWarnings("unused") Provider<Provider<String>> provider) {}
  }

  public static class ClassF {
    @Inject final String x = "";
  }
}
