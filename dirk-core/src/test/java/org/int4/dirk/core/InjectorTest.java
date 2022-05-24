package org.int4.dirk.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.int4.dirk.annotations.Produces;
import org.int4.dirk.api.Injector;
import org.int4.dirk.api.definition.AmbiguousDependencyException;
import org.int4.dirk.api.definition.AmbiguousRequiredDependencyException;
import org.int4.dirk.api.definition.AutoDiscoveryException;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.api.definition.DependencyException;
import org.int4.dirk.api.definition.DuplicateDependencyException;
import org.int4.dirk.api.definition.MissingDependencyException;
import org.int4.dirk.api.definition.UnsatisfiedDependencyException;
import org.int4.dirk.api.definition.UnsatisfiedRequiredDependencyException;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.core.test.injectables.AbstractBean;
import org.int4.dirk.core.test.injectables.BeanWithBigInjection;
import org.int4.dirk.core.test.injectables.BeanWithBigRedInjection;
import org.int4.dirk.core.test.injectables.BeanWithCollection;
import org.int4.dirk.core.test.injectables.BeanWithCollectionProvider;
import org.int4.dirk.core.test.injectables.BeanWithDirectCollectionItemDependency;
import org.int4.dirk.core.test.injectables.BeanWithDirectRedCollectionItemDependency;
import org.int4.dirk.core.test.injectables.BeanWithInjection;
import org.int4.dirk.core.test.injectables.BeanWithInterfaceBasedInjection;
import org.int4.dirk.core.test.injectables.BeanWithOptionalConstructorDependency;
import org.int4.dirk.core.test.injectables.BeanWithOptionalDependency;
import org.int4.dirk.core.test.injectables.BeanWithPostConstruct;
import org.int4.dirk.core.test.injectables.BeanWithProvider;
import org.int4.dirk.core.test.injectables.BeanWithProviderWithoutMatch;
import org.int4.dirk.core.test.injectables.BeanWithUnregisteredParent;
import org.int4.dirk.core.test.injectables.BeanWithUnresolvedDependency;
import org.int4.dirk.core.test.injectables.BeanWithUnresolvedProviderDependency;
import org.int4.dirk.core.test.injectables.BeanWithUnsupportedOptionalProviderDependency;
import org.int4.dirk.core.test.injectables.BigBean;
import org.int4.dirk.core.test.injectables.BigRedBean;
import org.int4.dirk.core.test.injectables.ConstructorInjectionSample;
import org.int4.dirk.core.test.injectables.ConstructorInjectionSampleWithMultipleAnnotatedConstructors;
import org.int4.dirk.core.test.injectables.FieldInjectionSampleWithAnnotatedFinalField;
import org.int4.dirk.core.test.injectables.SampleWithAnnotatedFinalFields;
import org.int4.dirk.core.test.injectables.SampleWithMultipleAnnotatedConstructors;
import org.int4.dirk.core.test.injectables.SimpleBean;
import org.int4.dirk.core.test.injectables.SimpleChildBean;
import org.int4.dirk.core.test.injectables.SimpleCollectionItemImpl1;
import org.int4.dirk.core.test.injectables.SimpleCollectionItemImpl2;
import org.int4.dirk.core.test.injectables.SimpleCollectionItemImpl3;
import org.int4.dirk.core.test.injectables.SimpleCollectionItemInterface;
import org.int4.dirk.core.test.injectables.SimpleImpl;
import org.int4.dirk.core.test.injectables.SimpleInterface;
import org.int4.dirk.core.test.injectables.SubclassOfAbstractBean;
import org.int4.dirk.core.test.injectables.SubclassOfBeanWithInjection;
import org.int4.dirk.core.test.injectables.SubclassOfBeanWithInjectionWithSameNamedInjection;
import org.int4.dirk.core.test.injectables.UnavailableBean;
import org.int4.dirk.core.test.injectables.UnregisteredParentBean;
import org.int4.dirk.core.util.Nullable;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

public class InjectorTest {
  private Injector injector;

  @BeforeEach
  public void beforeEach() throws Exception {
    injector = Injectors.manual();

    injector.register(SimpleBean.class);
    injector.register(BeanWithInjection.class);
    injector.register(SimpleImpl.class);
    injector.register(BeanWithInterfaceBasedInjection.class);
    injector.register(BeanWithCollection.class);
    injector.register(BeanWithProvider.class);
    injector.register(BeanWithProviderWithoutMatch.class);
    injector.register(SimpleCollectionItemImpl1.class);
    injector.register(SimpleCollectionItemImpl2.class);
    injector.register(BeanWithUnregisteredParent.class);
  }

  /*
   * Injector#getBean tests:
   */

  @Test
  public void shouldGetSimpleBean() throws Exception {
    assertNotNull(injector.getInstance(SimpleBean.class));
  }

  @Test
  public void shouldThrowExceptionWhenGettingUnregisteredBean() {
    assertThrows(UnsatisfiedResolutionException.class, () -> injector.getInstance(ArrayList.class));
  }

  @Test
  public void shouldThrowExceptionWhenBeanIsAmbiguous() {
    assertThrows(AmbiguousResolutionException.class, () -> injector.getInstance(SimpleCollectionItemInterface.class));
  }

  @Test
  public void shouldGetBeanWithInjection() throws Exception {
    BeanWithInjection bean = injector.getInstance(BeanWithInjection.class);

    assertNotNull(bean.getInjectedValue());
    assertEquals(SimpleBean.class, bean.getInjectedValue().getClass());
  }

  @Test
  public void shouldGetBeanWithInterfaceBasedInjection() throws Exception {
    BeanWithInterfaceBasedInjection bean = injector.getInstance(BeanWithInterfaceBasedInjection.class);

    assertNotNull(bean.getInjectedValue());
    assertEquals(SimpleImpl.class, bean.getInjectedValue().getClass());
  }

  @Test
  public void shouldGetBeanWithProviderInjection() throws Exception {
    BeanWithProvider bean = injector.getInstance(BeanWithProvider.class);

    assertNotNull(bean.getSimpleBean());
    assertEquals(SimpleBean.class, bean.getSimpleBean().getClass());
  }

  @Test
  public void shouldGetBeanWithOptionalDependencyWhenProviderReturnsNull() throws Exception {
    injector.register(BeanWithOptionalDependency.class);
    injector.registerInstance(new Provider<UnavailableBean>() {
      @Override
      public UnavailableBean get() {
        return null;
      }
    });

    assertNotNull(injector.getInstance(BeanWithOptionalDependency.class));
  }

  @Test
  public void shouldGetBeanWithOptionalDependencyWhenNoProviderAvailable() throws Exception {
    injector.register(BeanWithOptionalDependency.class);

    assertNotNull(injector.getInstance(BeanWithOptionalDependency.class));
  }

  public static class OptionalDependent {
    @Inject @Nullable private String string = "default";
    @Inject @Nullable private List<String> stringList = Arrays.asList("A", "B", "C");
    @Inject @Nullable private Set<String> stringSet = new HashSet<>(Arrays.asList("A", "B", "C"));
    @Inject private List<String> nonOptionalStringList = Arrays.asList("A", "B", "C");
    @Inject private Set<String> nonOptionalStringSet = new HashSet<>(Arrays.asList("A", "B", "C"));
  }

  @Test
  public void shouldLeaveDefaultFieldValuesIntactForOptionalDependencies() throws Exception {
    injector.register(OptionalDependent.class);

    OptionalDependent instance = injector.getInstance(OptionalDependent.class);

    assertNotNull(instance);

    // Leave default values on Nullable fields:
    assertEquals("default", instance.string);
    assertEquals(Arrays.asList("A", "B", "C"), instance.stringList);
    assertEquals(new HashSet<>(Arrays.asList("A", "B", "C")), instance.stringSet);

    // Overwrite defaults on required fields:
    assertEquals(Collections.emptyList(), instance.nonOptionalStringList);
    assertEquals(Collections.emptySet(), instance.nonOptionalStringSet);

    injector.registerInstance("new");

    instance = injector.getInstance(OptionalDependent.class);  // get again (it is not a singleton, so will be constructed again)

    assertNotNull(instance);

    // Overwrite all fields as a suitable dependency is available:
    assertEquals("new", instance.string);
    assertEquals(Arrays.asList("new"), instance.stringList);
    assertEquals(new HashSet<>(Arrays.asList("new")), instance.stringSet);
    assertEquals(Arrays.asList("new"), instance.nonOptionalStringList);
    assertEquals(new HashSet<>(Arrays.asList("new")), instance.nonOptionalStringSet);

    // adding another string would mean the optional string injection point would become ambiguous
    assertThatThrownBy(() -> injector.registerInstance("another-string"))
      .isExactlyInstanceOf(AmbiguousRequiredDependencyException.class)
      .hasMessage("Registering [Instance of [java.lang.String -> another-string]] would make existing required bindings ambiguous: [Field [private java.lang.String org.int4.dirk.core.InjectorTest$OptionalDependent.string]]; already satisfied by [Instance of [java.lang.String -> new]]")
      .hasNoCause();

    injector.removeInstance("new");

    instance = injector.getInstance(OptionalDependent.class);  // get again (it is not a singleton, so will be constructed again)

    assertNotNull(instance);

    // Leave default values on Nullable fields:
    assertEquals("default", instance.string);
    assertEquals(Arrays.asList("A", "B", "C"), instance.stringList);
    assertEquals(new HashSet<>(Arrays.asList("A", "B", "C")), instance.stringSet);

    // Overwrite defaults on required fields:
    assertEquals(Collections.emptyList(), instance.nonOptionalStringList);
    assertEquals(Collections.emptySet(), instance.nonOptionalStringSet);

    injector.registerInstance("another-string");

    instance = injector.getInstance(OptionalDependent.class);  // get again (it is not a singleton, so will be constructed again)

    assertNotNull(instance);

    // Overwrite all fields as a suitable dependency is available:
    assertEquals("another-string", instance.string);
    assertEquals(Arrays.asList("another-string"), instance.stringList);
    assertEquals(new HashSet<>(Arrays.asList("another-string")), instance.stringSet);
    assertEquals(Arrays.asList("another-string"), instance.nonOptionalStringList);
    assertEquals(new HashSet<>(Arrays.asList("another-string")), instance.nonOptionalStringSet);
  }

  @Test
  public void shouldGetBeanWithOptionalConstructorDependencyWhenNoProviderAvailable() throws Exception {
    injector.register(BeanWithOptionalConstructorDependency.class);

    assertNotNull(injector.getInstance(BeanWithOptionalConstructorDependency.class));
  }

  @Test  // @Nullable annotation on Provider is just ignored as it makes no sense for Providers
  public void shouldGetBeanWithOptionalProviderDependency() throws Exception {
    injector.registerInstance(new Provider<UnavailableBean>() {
      @Override
      public UnavailableBean get() {
        return null;  // this provider breaks its contract
      }
    });
    injector.register(BeanWithUnsupportedOptionalProviderDependency.class);

    BeanWithUnsupportedOptionalProviderDependency instance = injector.getInstance(BeanWithUnsupportedOptionalProviderDependency.class);

    assertNotNull(instance);
    assertNotNull(instance.getUnavailableBeanProvider());

    /*
     * Depending on how Providers are resolved, this could inject the "bad" provider
     * directly (which can return null) or a wrapper (which can check for null).
     *
     * The implementation has changed to always wrap a provider, because resolving providers
     * is very hard as searching the store for a Provider with qualifiers on its provided
     * type is not possible.  Also see InjectorProviderTest#providersShouldRespectQualifiers
     * comments.
     *
     * Furthermore, optional providers ARE allowed to return null.
     */

    assertThat(instance.getUnavailableBeanProvider().get()).isNull();
  }

  @Test
  public void shouldThrowExceptionWhenGettingUnavailableBean() throws Exception {
    injector.registerInstance(new Provider<UnavailableBean>() {
      @Override
      public UnavailableBean get() {
        return null;
      }
    });

    assertThrows(UnsatisfiedResolutionException.class, () -> injector.getInstance(UnavailableBean.class));
  }

  /*
   * Injector#remove tests:
   */

  @Test
  public void shouldRemoveBean() throws AutoDiscoveryException, DefinitionException, DependencyException {
    injector.remove(BeanWithInjection.class);

    assertThatThrownBy(() -> injector.getInstance(BeanWithInjection.class))
      .isExactlyInstanceOf(UnsatisfiedResolutionException.class)
      .hasNoCause();
  }

  @Test
  public void shouldThrowExceptionWhenRemovingInterface() {
    assertThatThrownBy(() -> injector.remove(SimpleInterface.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[interface org.int4.dirk.core.test.injectables.SimpleInterface] cannot be abstract")
      .hasNoCause();
  }

  @Test
  public void shouldThrowExceptionWhenRemovingUnregisteredBean() {
    assertThrows(MissingDependencyException.class, () -> injector.remove(Exception.class));
  }

  @Test
  public void shouldThrowExceptionWhenRemovingBeanWouldViolateSingularDependencies() {
    assertThrows(UnsatisfiedRequiredDependencyException.class, () -> injector.remove(SimpleBean.class));
  }

  @Test
  public void shouldRepeatedlyThrowExceptionWhenRemovingBeanWouldViolateSingularDependencies() {
    assertThrows(UnsatisfiedRequiredDependencyException.class, () -> injector.remove(SimpleBean.class));
    assertThrows(UnsatisfiedRequiredDependencyException.class, () -> injector.remove(SimpleBean.class));
  }

  @Test
  public void shouldBeAbleToRemoveProviderWhichIsOnlyOptionallyDependedOn() throws Exception {
    Provider<UnavailableBean> provider = new Provider<>() {
      @Override
      public UnavailableBean get() {
        return new UnavailableBean();
      }
    };

    injector.registerInstance(provider);
    injector.register(BeanWithOptionalDependency.class);

    assertNotNull(injector.getInstance(BeanWithOptionalDependency.class));
    assertNotNull(injector.getInstance(BeanWithOptionalDependency.class).getUnavailableBean());

    injector.removeInstance(provider);

    assertNotNull(injector.getInstance(BeanWithOptionalDependency.class));
    assertNull(injector.getInstance(BeanWithOptionalDependency.class).getUnavailableBean());
  }

  /*
   * Injector#register tests
   */

  @Test
  public void shouldRegisterAndRemoveStringInstances() throws Exception {
    injector.registerInstance("a");
    injector.registerInstance("b");
    injector.registerInstance("c");
    injector.registerInstance("d");

    assertThat(injector.getInstances(String.class))
      .containsExactlyInAnyOrder("a", "b", "c", "d");

    injector.removeInstance("a");
    injector.removeInstance("b");
    injector.removeInstance("c");
    injector.removeInstance("d");

    assertThat(injector.getInstances(String.class)).isEmpty();
  }

  @Test
  public void shouldRegisterAndRemoveSameStringInstancesWithDifferentQualifiers() throws Exception {
    injector.registerInstance("a", Annotations.of(Named.class, Map.of("value", "name-1")));
    injector.registerInstance("a", Annotations.of(Named.class, Map.of("value", "name-2")));

    assertThat(injector.getInstances(String.class))
      .containsExactlyInAnyOrder("a", "a");

    injector.removeInstance("a", Annotations.of(Named.class, Map.of("value", "name-1")));

    assertThat(injector.getInstances(String.class)).containsExactlyInAnyOrder("a");

    injector.removeInstance("a", Annotations.of(Named.class, Map.of("value", "name-2")));

    assertThat(injector.getInstances(String.class)).isEmpty();
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringDuplicate() throws Exception {
    injector.register(String.class);

    assertThatThrownBy(() -> injector.register(String.class))
      .isExactlyInstanceOf(DuplicateDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringInterface() {
    assertThatThrownBy(() -> injector.register(List.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[interface java.util.List] cannot be abstract")
      .hasNoCause();
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringBeanWithUnresolvedDependencies() {
    assertThrows(UnsatisfiedDependencyException.class, () -> injector.register(BeanWithUnresolvedDependency.class));
  }

  @Test
  @Disabled("Providers should be able to break circular dependencies...")
  public void shouldThrowExceptionWhenRegisteringBeanWithUnresolvedProviderDependencies() {
    assertThrows(UnsatisfiedDependencyException.class, () -> injector.register(BeanWithUnresolvedProviderDependency.class));
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringBeanWithAmbiguousDependencies() {
    assertThrows(AmbiguousDependencyException.class, () -> injector.register(BeanWithDirectCollectionItemDependency.class));
  }

  @Test
  public void shouldRepeatedlyThrowExceptionWhenRegisteringBeanWithUnresolvedDependencies() {
    assertThrows(UnsatisfiedDependencyException.class, () -> injector.register(BeanWithUnresolvedDependency.class));
    assertThrows(UnsatisfiedDependencyException.class, () -> injector.register(BeanWithUnresolvedDependency.class));
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringBeanWouldViolateSingularDependencies() {
    assertThrows(AmbiguousRequiredDependencyException.class, () -> injector.register(SimpleChildBean.class));
  }

  @Test
  public void shouldRepeatedlyThrowExceptionWhenRegisteringBeanWouldViolateSingularDependencies() {
    assertThrows(AmbiguousRequiredDependencyException.class, () -> injector.register(SimpleChildBean.class));
    assertThrows(AmbiguousRequiredDependencyException.class, () -> injector.register(SimpleChildBean.class));
  }

  /*
   * Singleton tests
   */

  @Test
  public void shouldRespectSingletonAnnotation() throws Exception {
    SimpleBean bean1 = injector.getInstance(SimpleBean.class);

    assertTrue(bean1 == injector.getInstance(SimpleBean.class));
    assertTrue(bean1 == injector.getInstance(BeanWithInjection.class).getInjectedValue());
  }

  /*
   * Qualifier tests
   */

  @Test
  public void shouldSupportQualifiers() throws Exception {
    injector.register(BigRedBean.class);

    injector.register(BeanWithBigInjection.class);
    injector.register(BeanWithBigRedInjection.class);

    injector.remove(BeanWithBigInjection.class);
    injector.remove(BeanWithBigRedInjection.class);

    injector.remove(BigRedBean.class);
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringDependentBeanWithNoMatchForAllQualifiers() throws Exception {
    injector.register(BigBean.class);

    assertThatThrownBy(() -> injector.register(BeanWithBigRedInjection.class))  // Won't match BigBean, so won't match anything
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringBeanWithMoreQualifiersWhenItWouldViolateSingularDependencies() throws Exception {
    injector.register(BigBean.class);
    injector.register(BeanWithBigInjection.class);

    assertThatThrownBy(() -> injector.register(BigRedBean.class))  // Would also match injection for BeanWithBigInjection
      .isExactlyInstanceOf(AmbiguousRequiredDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldThrowExceptionWhenRemovingRequiredBeanWithMoreQualifiers() throws Exception {
    injector.register(BigRedBean.class);
    injector.register(BeanWithBigInjection.class);

    assertThatThrownBy(() -> injector.remove(BigRedBean.class))
      .isExactlyInstanceOf(UnsatisfiedRequiredDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldGetBeanWithInjectionWithMultipleTypeMatchesWhenDisambiguatedWithQualifier() throws Exception {
    injector.register(SimpleCollectionItemImpl3.class);  // One of several, this one qualified
    injector.register(BeanWithDirectRedCollectionItemDependency.class);  // Depends on the specific one above using qualifier

    assertNotNull(injector.getInstance(BeanWithDirectRedCollectionItemDependency.class));
  }

  /*
   * PostConstruct/PreDestroy tests
   */

  @Test
  public void shouldCallPostConstruct() throws Exception {
    injector.registerInstance("Hello World");
    injector.register(BeanWithPostConstruct.class);

    BeanWithPostConstruct instance = injector.getInstance(BeanWithPostConstruct.class);

    assertTrue(instance.isPostConstructCalled());
    assertTrue(instance.isPrivatePostConstructCalled());
    assertTrue(instance.isPostConstructOrderCorrect());
    assertTrue(instance.isPrivatePostConstructOrderCorrect());
  }

  /*
   * Collection tests
   */

  @Test
  public void shouldInjectCollection() throws Exception {
    BeanWithCollection bean = injector.getInstance(BeanWithCollection.class);

    assertNotNull(bean.getInjectedValues());
    assertEquals(2, bean.getInjectedValues().size());

    injector.remove(SimpleCollectionItemImpl2.class);

    BeanWithCollection bean2 = injector.getInstance(BeanWithCollection.class);

    assertEquals(2, bean.getInjectedValues().size());
    assertEquals(1, bean2.getInjectedValues().size());
  }

  @Test
  public void shouldInjectEmptyCollection() throws Exception {
    BeanWithCollection bean = injector.getInstance(BeanWithCollection.class);

    assertNotNull(bean.getInjectedValues());
    assertEquals(2, bean.getInjectedValues().size());

    injector.remove(SimpleCollectionItemImpl1.class);
    injector.remove(SimpleCollectionItemImpl2.class);

    BeanWithCollection bean2 = injector.getInstance(BeanWithCollection.class);

    assertEquals(2, bean.getInjectedValues().size());
    assertEquals(0, bean2.getInjectedValues().size());
  }

  @Test
  public void shouldInjectCollectionProvider() throws Exception {
    injector.register(BeanWithCollectionProvider.class);

    BeanWithCollectionProvider bean = injector.getInstance(BeanWithCollectionProvider.class);

    assertNotNull(bean.getInjectedValues());
    assertEquals(2, bean.getInjectedValues().size());

    injector.remove(SimpleCollectionItemImpl2.class);

    BeanWithCollectionProvider bean2 = injector.getInstance(BeanWithCollectionProvider.class);

    assertEquals(1, bean.getInjectedValues().size());  // provider resolves dynamically, so this is now 1 after the removal of SimpleCollectionItemImpl2.class
    assertEquals(1, bean2.getInjectedValues().size());
  }

  /*
   * Misc
   */

  @Test
  public void shouldThrowExceptionWhenRemovingUnregisteredSuperClass() {
    assertThrows(MissingDependencyException.class, () -> injector.remove(UnregisteredParentBean.class));
  }

  /*
   * Field injection
   */

  @Test
  public void shouldThrowExceptionWhenFinalFieldAnnotatedWithInject() throws SecurityException {
    assertThatThrownBy(() -> injector.register(FieldInjectionSampleWithAnnotatedFinalField.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [private final org.int4.dirk.core.test.injectables.SimpleBean org.int4.dirk.core.test.injectables.FieldInjectionSampleWithAnnotatedFinalField.injectedValue] of [class org.int4.dirk.core.test.injectables.FieldInjectionSampleWithAnnotatedFinalField] cannot be final")
      .hasNoCause();
  }

  /*
   * Setter injection
   */

  @Test
  public void shouldInjectSetters() throws Exception {
    injector.register(List.of(A.class, B.class, C.class, D.class));

    A instance = injector.getInstance(A.class);

    assertThat(instance.b).isInstanceOf(B.class);
    assertThat(instance.t).isInstanceOf(C.class);
    assertThat(instance.d).isInstanceOf(D.class);
  }

  public static class A extends G<C> {
    D d;
    String e;

    @Inject
    A fluentSetD(D d) {
      this.d = d;

      return this;
    }
  }

  public static class B {
  }

  public static class C {
  }

  public static class D {
  }

  static class G<T> {
    T t;
    B b;

    @Inject
    private void setter(B b, T t) {
      this.b = b;
      this.t = t;
    }
  }

  @Test
  public void shouldRejectSetterWithoutParameters() {
    assertThatThrownBy(() -> injector.register(BadA.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [void org.int4.dirk.core.InjectorTest$BadA.setNothing()] of [class org.int4.dirk.core.InjectorTest$BadA] must have parameters")
      .hasNoCause();
  }

  public static class BadA {
    @Inject
    void setNothing() {
    }
  }

  /*
   * Constructor Injection
   */

  @Test
  public void shouldInjectConstructor() throws Exception {
    injector.register(ConstructorInjectionSample.class);

    ConstructorInjectionSample instance = injector.getInstance(ConstructorInjectionSample.class);

    assertNotNull(instance);
    assertThat(instance.getInjectedValue()).isInstanceOf(SimpleBean.class);
  }

  @Test
  public void shouldThrowExceptionWhenMultipleConstructorsAnnotatedWithInject() {
    assertThatThrownBy(() -> injector.register(ConstructorInjectionSampleWithMultipleAnnotatedConstructors.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class org.int4.dirk.core.test.injectables.ConstructorInjectionSampleWithMultipleAnnotatedConstructors] cannot have multiple Inject annotated constructors")
      .hasNoCause();
  }

  /*
   * Instance tests
   */

  @Test
  public void shouldRegisterAndRemoveProviderInstance() throws Exception {
    Provider<String> provider = new Provider<>() {
      @Override
      public String get() {
        return "a string";
      }
    };

    injector.registerInstance(provider);

    assertEquals("a string", injector.getInstance(String.class));

    injector.removeInstance(provider);

    assertThatThrownBy(() -> injector.getInstance(String.class))
      .isExactlyInstanceOf(UnsatisfiedResolutionException.class);
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringProviderWouldViolateSingularDependencies() {
    assertThrows(AmbiguousRequiredDependencyException.class, () -> injector.registerInstance(new Provider<SimpleChildBean>() {
      @Override
      public SimpleChildBean get() {
        return new SimpleChildBean();
      }
    }));
  }

  @Test
  public void shouldThrowExceptionWhenRemovingSimilarButNotSameProvider() throws Exception {
    injector.registerInstance(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });

    assertThrows(MissingDependencyException.class, () -> injector.removeInstance(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    }));
  }

  @Test
  public void shouldThrowExceptionWhenRemovingProviderByClass() throws Exception {
    injector.registerInstance(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });

    assertThrows(MissingDependencyException.class, () -> injector.remove(String.class));
  }

  @Test
  public void shouldRegisterAndRemoveInstance() throws Exception {
    injector.registerInstance(new String("hello there!"));

    assertThat(injector.getInstances(String.class)).isNotEmpty();

    injector.removeInstance(new String("hello there!"));

    assertThat(injector.getInstances(String.class)).isEmpty();
  }

  @Test
  public void shouldRegisterInstanceEvenWithBadAnnotations() throws Exception {
    SampleWithMultipleAnnotatedConstructors sample = new SampleWithMultipleAnnotatedConstructors();

    injector.registerInstance(sample);  // note, not a class, but an instantiated object!

    assertThat(injector.getInstance(SampleWithMultipleAnnotatedConstructors.class)).isEqualTo(sample);
  }

  @Test
  public void shouldRegisterInstanceEvenWithAnnotatedFinalFields() throws Exception {
    SampleWithAnnotatedFinalFields sample = new SampleWithAnnotatedFinalFields();

    injector.registerInstance(sample);  // note, not a class, but an instantiated object!

    assertThat(injector.getInstance(SampleWithAnnotatedFinalFields.class)).isEqualTo(sample);
  }

  @Test
  public void shouldInjectSuperClass() throws Exception {
    injector.register(SubclassOfBeanWithInjection.class);

    SubclassOfBeanWithInjection bean = injector.getInstance(SubclassOfBeanWithInjection.class);

    assertEquals(injector.getInstance(SimpleBean.class), bean.getInjectedValue());
  }

  @Test
  public void shouldInjectSuperAndSubClassEvenIfFieldsAreSameName() throws Exception {
    injector.register(SubclassOfBeanWithInjectionWithSameNamedInjection.class);

    SubclassOfBeanWithInjectionWithSameNamedInjection bean = injector.getInstance(SubclassOfBeanWithInjectionWithSameNamedInjection.class);
    SimpleBean simpleBean = injector.getInstance(SimpleBean.class);

    assertEquals(simpleBean, bean.getInjectedValue());
    assertEquals(simpleBean, bean.getInjectedValueInSubClass());
  }

  @Test
  public void shouldFindInstanceByAbstractSuperClass() throws Exception {
    injector.register(SubclassOfAbstractBean.class);

    List<AbstractBean> beans = injector.getInstances(AbstractBean.class);

    assertEquals(1, beans.size());
  }

  @Test
  public void shouldInjectSameSingletonEachTime() throws Exception {
    SimpleBean simpleBean = injector.getInstance(BeanWithInjection.class).getInjectedValue();

    assertEquals(simpleBean, injector.getInstance(BeanWithInjection.class).getInjectedValue());
  }

  @Test
  public void shouldThrowConstructionExceptionWhenPostConstructHasACircularDependency() throws Exception {
    injector.register(BeanWithBadPostConstruct.class);
    injector.register(BeanDependentOnBeanWithBadPostConstruct.class);

    assertThatThrownBy(() -> injector.getInstance(BeanWithBadPostConstruct.class))
      .isExactlyInstanceOf(CreationException.class)
      .hasMessage("[class org.int4.dirk.core.InjectorTest$BeanWithBadPostConstruct] threw exception during post construction")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NullPointerException.class)
      .hasNoCause();
  }

  @Test
  @Disabled("This test is no longer valid; Providers are injected directly now (no indirection that could check their result) and so null instances can be part of the results if a Provider breaks its contract.")
  public void getInstancesShouldSilentlyIgnoreProvidersThatReturnNull() throws Exception {
    injector.registerInstance(new Provider<String>() {
      @Override
      public String get() {
        return null;
      }
    });

    assertEquals(10, injector.getInstances(Object.class).size());
  }

  public static class BeanWithBadPostConstruct {
    private Provider<BeanDependentOnBeanWithBadPostConstruct> provider;

    @PostConstruct
    private void postConstruct() {
      provider.get();
    }
  }

  public static class BeanDependentOnBeanWithBadPostConstruct {
    @Inject private BeanWithBadPostConstruct dependency;
  }

  interface SomeInterface {
  }

  interface SomeInterfaceProvider extends Provider<SomeInterface> {
  }

  static class Bean1 implements SomeInterface {
    @Inject public Bean1() {}
  }

  static class Bean2 implements Provider<SomeInterface> {
    @Inject public Bean2() {}

    @Override
    public SomeInterface get() {
      return null;
    }
  }

  public static class Bean3 implements SomeInterfaceProvider {
    @Inject public Bean3() {}

    @Override
    public SomeInterface get() {
      return new SomeInterface() {
      };
    }
  }

  static class BeanThatNeedsInterface {
    @Inject SomeInterface someInterface;

    public BeanThatNeedsInterface() {}
  }

  public static class BeanThatNeedsProviderOfSomeInterface {
    @Inject Provider<SomeInterface> someInterface;

    public BeanThatNeedsProviderOfSomeInterface() {}

    public SomeInterface callProvider() {
      return someInterface.get();
    }
  }

  public static class BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider {
    @Inject Provider<Bean3> bean3;

    public BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider() {}

    public Bean3 callProvider() {
      return bean3.get();
    }
  }

  @Test
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface() throws Exception {
    // First register interface implementor, then interface provider
    injector.register(Bean1.class);
    injector.register(BeanThatNeedsInterface.class);

    assertThatThrownBy(() -> injector.register(Bean2.class))
      .isExactlyInstanceOf(AmbiguousRequiredDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface_2() throws Exception {
    // First register interface provider, then implementor
    injector.register(Bean2.class);
    injector.register(BeanThatNeedsInterface.class);

    assertThatThrownBy(() -> injector.register(Bean1.class))
      .isExactlyInstanceOf(AmbiguousRequiredDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface_3() throws Exception {
    // First register interface implementor, then indirect interface provider
    injector.register(Bean1.class);
    injector.register(BeanThatNeedsInterface.class);

    assertThatThrownBy(() -> injector.register(Bean3.class))
      .isExactlyInstanceOf(AmbiguousRequiredDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface_4() throws Exception {
    // First register indirect interface provider, then implementor
    injector.register(Bean3.class);
    injector.register(BeanThatNeedsInterface.class);

    assertThatThrownBy(() -> injector.register(Bean1.class))
      .isExactlyInstanceOf(AmbiguousRequiredDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldInjectCorrectProvider() throws Exception {
    injector.register(Bean3.class);
    injector.register(BeanThatNeedsProviderOfSomeInterface.class);

    BeanThatNeedsProviderOfSomeInterface b = injector.getInstance(BeanThatNeedsProviderOfSomeInterface.class);

    assertTrue(SomeInterface.class.isInstance(b.callProvider()));
  }

  @Test
  public void shouldInjectCorrectProvider2() throws Exception {
    injector.register(Bean3.class);
    injector.register(BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider.class);

    BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider b = injector.getInstance(BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider.class);

    assertTrue(Bean3.class.isInstance(b.callProvider()));
  }

  public static class BadPostConstruct {
    @Inject Provider<BadPostConstruct> provider;

    @PostConstruct
    void postConstruct() {
      provider.get();  // not allowed as this class is still under construction until all post constructors have finished
    }
  }

  @Test
  public void postConstructShouldRejectReferringToObjectUnderConstruction() throws Exception {
    injector.register(BadPostConstruct.class);

    assertThatThrownBy(() -> injector.getInstance(BadPostConstruct.class))
      .isExactlyInstanceOf(CreationException.class)
      .hasMessage("[class org.int4.dirk.core.InjectorTest$BadPostConstruct] threw exception during post construction")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(CreationException.class)
      .hasMessage("[class org.int4.dirk.core.InjectorTest$BadPostConstruct] already under construction (dependency creation loop in setter, initializer or post-construct method?)")
      .hasNoCause();
  }

  @Test
  public void shouldAutoCreateCollections() throws Exception {
    injector.registerInstance("A");
    injector.registerInstance("B");
    injector.registerInstance("C");

    List<String> list = injector.getInstance(Types.parameterize(List.class, String.class));

    assertThat(list).containsExactlyInAnyOrder("A", "B", "C");

    Set<String> set = injector.getInstance(Types.parameterize(Set.class, String.class));

    assertThat(set).containsExactlyInAnyOrder("A", "B", "C");
  }

  @Test
  public void shouldSkipNullsInCollections() throws Exception {
    injector.register(NullProducers.class);

    List<String> list = injector.getInstance(Types.parameterize(List.class, String.class));

    assertThat(list).containsExactlyInAnyOrder("B", "C");

    Set<String> set = injector.getInstance(Types.parameterize(Set.class, String.class));

    assertThat(set).containsExactlyInAnyOrder("B", "C");
  }

  static class NullProducers {
    @Produces String a = null;
    @Produces String b = "B";
    @Produces String c = "C";

    public NullProducers() {}
  }

  @Test
  void shouldRegisterParentAndSubclassWithSetterMethod() throws Exception {
    injector.registerInstance("A");
    injector.registerInstance(5);
    injector.register(Parent.class);
    injector.register(Child.class);
  }

  public static class Parent {
    String x;
    Integer y;

    @Inject
    public void stuff(String x, Integer y) {
      this.x = x;
      this.y = y;
    }
  }

  public static class Child extends Parent {
  }
}
