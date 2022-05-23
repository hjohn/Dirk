package hs.ddif.core.definition;

import hs.ddif.annotations.Opt;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.core.InjectionTargetExtensionStores;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.library.ConfigurableAnnotationStrategy;
import hs.ddif.util.Annotations;
import hs.ddif.util.Types;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

public class BindingProviderTest {
  private static final Annotation RED = Annotations.of(Red.class);
  private static final Annotation GREEN = Annotations.of(Green.class);

  private BindingProvider bindingProvider = new BindingProvider(new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, null), InjectionTargetExtensionStores.create());

  @Test
  public void ofMembersShouldBindToGenericFieldInSubclass() throws Exception {
    List<Binding> bindings = bindingProvider.ofMembers(Subclass.class);

    assertThat(bindings).extracting(Binding::getAccessibleObject, Binding::getType).containsExactlyInAnyOrder(
      Tuple.tuple(ClassWithGenericFields.class.getDeclaredField("fieldA"), String.class),
      Tuple.tuple(ClassWithGenericFields.class.getDeclaredMethod("setterB", Object.class), Integer.class)
    );
  }

  @Test
  public void ofMethodShouldCreateCorrectBindings() throws Exception {
    List<Binding> bindings = bindingProvider.ofMethod(Subclass.class.getDeclaredMethod("create", Integer.class, Double.class), Subclass.class);

    assertEquals(3, bindings.size());
    assertEquals(Integer.class, bindings.get(0).getType());
    assertEquals(Double.class, bindings.get(1).getType());
    assertEquals(Subclass.class, bindings.get(2).getType());
    assertEquals(Set.of(Annotations.of(Big.class)), bindings.get(0).getQualifiers());
    assertEquals(Set.of(), bindings.get(1).getQualifiers());
    assertEquals(Set.of(), bindings.get(2).getQualifiers());
  }

  @Test
  public void ofMethodShouldTakeNonStaticIntoAccount() throws Exception {
    assertThat(bindingProvider.ofMethod(MethodHolder.class.getDeclaredMethod("create", Double.class), MethodHolder.class))
      .extracting(Binding::getType)
      .containsExactly(
        Double.class,
        MethodHolder.class  // dependency on the declaring class as "create" is an instance method
      );
  }

  @Test
  public void ofMethodShouldTakeStaticIntoAccount() throws Exception {
    assertThat(bindingProvider.ofMethod(MethodHolder.class.getDeclaredMethod("createStatic", Double.class), MethodHolder.class))
      .extracting(Binding::getType)
      .containsExactly(
        Double.class
      );
  }

  @Test
  public void ofFieldShouldTakeNonStaticIntoAccount() throws Exception {
    assertThat(bindingProvider.ofField(FieldHolder.class.getDeclaredField("b"), FieldHolder.class))
      .extracting(Binding::getType)
      .containsExactly(
        FieldHolder.class
      );
  }

  @Test
  public void ofFieldShouldTakeStaticIntoAccount() throws Exception {
    assertThat(bindingProvider.ofField(FieldHolder.class.getDeclaredField("a"), FieldHolder.class))
      .isEmpty();
  }

  @Test
  public void getConstructorShouldFindInjectAnnotatedConstructor() throws Exception {
    assertThat(bindingProvider.getConstructor(ClassA.class))
      .isEqualTo(ClassA.class.getDeclaredConstructor(ClassB.class));
  }

  @Test
  public void getConstructorShouldFindDefaultConstructor() throws Exception {
    assertThat(bindingProvider.getConstructor(ClassB.class))
      .isEqualTo(ClassB.class.getConstructor());
  }

  @Test
  public void getConstructorShouldRejectClassWithoutPublicDefaultConstructor() {
    assertThatThrownBy(() -> bindingProvider.getConstructor(ClassC.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.definition.BindingProviderTest$ClassC] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
      .hasNoCause();
  }

  @Test
  public void getConstructorShouldRejectClassWithMultipleAnnotatedConstructors() {
    assertThatThrownBy(() -> bindingProvider.getConstructor(ClassD.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.definition.BindingProviderTest$ClassD] cannot have multiple Inject annotated constructors")
      .hasNoCause();
  }

  @Test
  public void ofMembersShouldRejectFinalField() {
    assertThatThrownBy(() -> bindingProvider.ofMembers(ClassF.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [final java.lang.String hs.ddif.core.definition.BindingProviderTest$ClassF.x] of [class hs.ddif.core.definition.BindingProviderTest$ClassF] cannot be final")
      .hasNoCause();
  }

  @Test
  public void ofConstructorAndMembersShouldFindAllBindings() throws Exception {
    List<Binding> bindings = bindingProvider.ofConstructorAndMembers(ClassA.class.getDeclaredConstructor(ClassB.class), ClassA.class);

    assertThat(bindings)
      .extracting(Binding::getType, Binding::getQualifiers)
      .containsExactly(
        Tuple.tuple(ClassB.class, Set.of()),
        Tuple.tuple(String.class, Set.of()),
        Tuple.tuple(Types.parameterize(Provider.class, Integer.class), Set.of()),
        Tuple.tuple(Types.parameterize(List.class, Double.class), Set.of()),
        Tuple.tuple(Types.parameterize(Set.class, String.class), Set.of()),
        Tuple.tuple(Types.parameterize(List.class, Double.class), Set.of(RED)),
        Tuple.tuple(Types.parameterize(Set.class, String.class), Set.of(RED)),
        Tuple.tuple(Types.parameterize(List.class, Double.class), Set.of(GREEN)),
        Tuple.tuple(Types.parameterize(Set.class, String.class), Set.of(GREEN)),
        Tuple.tuple(Long.class, Set.of()),
        Tuple.tuple(Types.parameterize(Provider.class, Short.class), Set.of()),
        Tuple.tuple(Types.parameterize(Provider.class, Short.class), Set.of())
      );
  }

  @Test
  @Disabled
  public void shouldNotCreateBindingForOverridenParentMethod() throws Exception {
    List<Binding> bindings = bindingProvider.ofMembers(SpecializedChild.class);

    assertThat(bindings).hasSize(2);
  }

  @Test
  public void bindingsOfChildShouldNotMatchBindingsOfParent() throws Exception {
    List<Binding> parentBindings = bindingProvider.ofMembers(Parent.class);
    List<Binding> childBindings = bindingProvider.ofMembers(Child.class);

    assertThat(childBindings).doesNotContainAnyElementsOf(parentBindings);
  }

  @SuppressWarnings("unused")
  private static class ClassWithGenericFields<A, B> {
    @Inject private A fieldA;
    private B fieldB;

    @Inject
    void setterB(B b) {
      fieldB = b;
    }
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

  public static class Parent {
    @Inject String y;
    String x;
    String d;

    @Inject
    public void setText(String x) {
      this.x = x;
    }

    @Inject
    public void setDescription(String d) {
      this.d = d;
    }
  }

  public static class Child extends Parent {
  }

  public static class SpecializedChild extends Parent {
    @Override
    @Inject
    public void setDescription(String x) {
      super.setDescription(x);
    }
  }

}
