package hs.ddif.core;

import hs.ddif.core.api.InstanceCreationException;
import hs.ddif.core.api.MultipleInstancesException;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.inject.consistency.UnresolvableDependencyException;
import hs.ddif.core.inject.consistency.ViolatesSingularDependencyException;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.store.DuplicateInjectableException;
import hs.ddif.core.store.NoSuchInjectableException;
import hs.ddif.core.test.injectables.AbstractBean;
import hs.ddif.core.test.injectables.BeanWithBigInjection;
import hs.ddif.core.test.injectables.BeanWithBigRedInjection;
import hs.ddif.core.test.injectables.BeanWithCollection;
import hs.ddif.core.test.injectables.BeanWithCollectionProvider;
import hs.ddif.core.test.injectables.BeanWithDirectCollectionItemDependency;
import hs.ddif.core.test.injectables.BeanWithDirectRedCollectionItemDependency;
import hs.ddif.core.test.injectables.BeanWithInjection;
import hs.ddif.core.test.injectables.BeanWithInterfaceBasedInjection;
import hs.ddif.core.test.injectables.BeanWithOptionalConstructorDependency;
import hs.ddif.core.test.injectables.BeanWithOptionalDependency;
import hs.ddif.core.test.injectables.BeanWithPostConstruct;
import hs.ddif.core.test.injectables.BeanWithProvider;
import hs.ddif.core.test.injectables.BeanWithProviderWithoutMatch;
import hs.ddif.core.test.injectables.BeanWithUnregisteredParent;
import hs.ddif.core.test.injectables.BeanWithUnresolvedDependency;
import hs.ddif.core.test.injectables.BeanWithUnresolvedProviderDependency;
import hs.ddif.core.test.injectables.BeanWithUnsupportedOptionalProviderDependency;
import hs.ddif.core.test.injectables.BigBean;
import hs.ddif.core.test.injectables.BigRedBean;
import hs.ddif.core.test.injectables.ConstructorInjectionSample;
import hs.ddif.core.test.injectables.ConstructorInjectionSampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.FieldInjectionSampleWithAnnotatedFinalField;
import hs.ddif.core.test.injectables.SampleWithAnnotatedFinalFields;
import hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SimpleBean;
import hs.ddif.core.test.injectables.SimpleChildBean;
import hs.ddif.core.test.injectables.SimpleCollectionItemImpl1;
import hs.ddif.core.test.injectables.SimpleCollectionItemImpl2;
import hs.ddif.core.test.injectables.SimpleCollectionItemImpl3;
import hs.ddif.core.test.injectables.SimpleCollectionItemInterface;
import hs.ddif.core.test.injectables.SimpleImpl;
import hs.ddif.core.test.injectables.SimpleInterface;
import hs.ddif.core.test.injectables.SubclassOfAbstractBean;
import hs.ddif.core.test.injectables.SubclassOfBeanWithInjection;
import hs.ddif.core.test.injectables.SubclassOfBeanWithInjectionWithSameNamedInjection;
import hs.ddif.core.test.injectables.UnavailableBean;
import hs.ddif.core.test.injectables.UnregisteredParentBean;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.Nullable;
import hs.ddif.core.util.Value;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InjectorTest {
  private Injector injector;

  @BeforeEach
  public void beforeEach() {
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
  public void shouldGetSimpleBean() {
    assertNotNull(injector.getInstance(SimpleBean.class));
  }

  @Test
  public void shouldThrowExceptionWhenGettingUnregisteredBean() {
    assertThrows(NoSuchInstanceException.class, () -> injector.getInstance(ArrayList.class));
  }

  @Test
  public void shouldThrowExceptionWhenBeanIsAmbiguous() {
    assertThrows(MultipleInstancesException.class, () -> injector.getInstance(SimpleCollectionItemInterface.class));
  }

  @Test
  public void shouldGetBeanWithInjection() {
    BeanWithInjection bean = injector.getInstance(BeanWithInjection.class);

    assertNotNull(bean.getInjectedValue());
    assertEquals(SimpleBean.class, bean.getInjectedValue().getClass());
  }

  @Test
  public void shouldGetBeanWithInterfaceBasedInjection() {
    BeanWithInterfaceBasedInjection bean = injector.getInstance(BeanWithInterfaceBasedInjection.class);

    assertNotNull(bean.getInjectedValue());
    assertEquals(SimpleImpl.class, bean.getInjectedValue().getClass());
  }

  @Test
  public void shouldGetBeanWithProviderInjection() {
    BeanWithProvider bean = injector.getInstance(BeanWithProvider.class);

    assertNotNull(bean.getSimpleBean());
    assertEquals(SimpleBean.class, bean.getSimpleBean().getClass());
  }

  @Test
  public void shouldGetBeanWithOptionalDependencyWhenProviderReturnsNull() {
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
  public void shouldGetBeanWithOptionalDependencyWhenNoProviderAvailable() {
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
  public void shouldLeaveDefaultFieldValuesIntactForOptionalDependencies() {
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
  }

  @Test
  public void shouldGetBeanWithOptionalConstructorDependencyWhenNoProviderAvailable() {
    injector.register(BeanWithOptionalConstructorDependency.class);

    assertNotNull(injector.getInstance(BeanWithOptionalConstructorDependency.class));
  }

  @Test  // @Nullable annotation on Provider is just ignored as it makes no sense for Providers
  public void shouldGetBeanWithOptionalProviderDependency() {
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

    // Previously the call below would throw a BeanResolutionException (because the provider was wrapped in another provider),
    // however, now the given provider is injected unaltered.  As it is injected as-is, there is no way to prevent
    // it from returning null, so it returning null is the expected behaviour now.
    assertNull(instance.getUnavailableBeanProvider().get());
  }

  @Test
  public void shouldThrowExceptionWhenGettingUnavailableBean() {
    injector.registerInstance(new Provider<UnavailableBean>() {
      @Override
      public UnavailableBean get() {
        return null;
      }
    });

    assertThrows(NoSuchInstanceException.class, () -> injector.getInstance(UnavailableBean.class));
  }

  /*
   * Injector#remove tests:
   */

  @Test
  public void shouldRemoveBean() {
    injector.remove(BeanWithInjection.class);

    assertThatThrownBy(() -> injector.getInstance(BeanWithInjection.class))
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasNoCause();
  }

  @Test
  public void shouldThrowExceptionWhenRemovingInterface() {
    assertThrows(BindingException.class, () -> injector.remove(SimpleInterface.class));
  }

  @Test
  public void shouldThrowExceptionWhenRemovingUnregisteredBean() {
    assertThrows(NoSuchInjectableException.class, () -> injector.remove(Exception.class));
  }

  @Test
  public void shouldThrowExceptionWhenRemovingBeanWouldViolateSingularDependencies() {
    assertThrows(ViolatesSingularDependencyException.class, () -> injector.remove(SimpleBean.class));
  }

  @Test
  public void shouldRepeatedlyThrowExceptionWhenRemovingBeanWouldViolateSingularDependencies() {
    assertThrows(ViolatesSingularDependencyException.class, () -> injector.remove(SimpleBean.class));
    assertThrows(ViolatesSingularDependencyException.class, () -> injector.remove(SimpleBean.class));
  }

  @Test
  public void shouldBeAbleToRemoveProviderWhichIsOnlyOptionallyDependedOn() {
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
  public void shouldRegisterStringInstances() {
    injector.registerInstance("a");
    injector.registerInstance("b");
    injector.registerInstance("c");
    injector.registerInstance("d");

    assertThat(injector.getInstances(String.class))
      .containsExactlyInAnyOrder("a", "b", "c", "d");
  }

  @Test
  public void shouldRegisterSameStringInstancesWithDifferentQualifiers() {
    injector.registerInstance("a", AnnotationDescriptor.describe(Named.class, new Value("value", "name-1")));
    injector.registerInstance("a", AnnotationDescriptor.describe(Named.class, new Value("value", "name-2")));
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringDuplicate() {
    injector.register(String.class);

    assertThatThrownBy(() -> injector.register(String.class))
      .isExactlyInstanceOf(DuplicateInjectableException.class)
      .hasNoCause();
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringInterface() {
    assertThrows(BindingException.class, () -> injector.register(List.class));
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringBeanWithUnresolvedDependencies() {
    assertThrows(UnresolvableDependencyException.class, () -> injector.register(BeanWithUnresolvedDependency.class));
  }

  @Test
  @Disabled("Providers should be able to break circular dependencies...")
  public void shouldThrowExceptionWhenRegisteringBeanWithUnresolvedProviderDependencies() {
    assertThrows(UnresolvableDependencyException.class, () -> injector.register(BeanWithUnresolvedProviderDependency.class));
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringBeanWithAmbiguousDependencies() {
    assertThrows(UnresolvableDependencyException.class, () -> injector.register(BeanWithDirectCollectionItemDependency.class));
  }

  @Test
  public void shouldRepeatedlyThrowExceptionWhenRegisteringBeanWithUnresolvedDependencies() {
    assertThrows(UnresolvableDependencyException.class, () -> injector.register(BeanWithUnresolvedDependency.class));
    assertThrows(UnresolvableDependencyException.class, () -> injector.register(BeanWithUnresolvedDependency.class));
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringBeanWouldViolateSingularDependencies() {
    assertThrows(ViolatesSingularDependencyException.class, () -> injector.register(SimpleChildBean.class));
  }

  @Test
  public void shouldRepeatedlyThrowExceptionWhenRegisteringBeanWouldViolateSingularDependencies() {
    assertThrows(ViolatesSingularDependencyException.class, () -> injector.register(SimpleChildBean.class));
    assertThrows(ViolatesSingularDependencyException.class, () -> injector.register(SimpleChildBean.class));
  }

  /*
   * Singleton tests
   */

  @Test
  public void shouldRespectSingletonAnnotation() {
    SimpleBean bean1 = injector.getInstance(SimpleBean.class);

    assertTrue(bean1 == injector.getInstance(SimpleBean.class));
    assertTrue(bean1 == injector.getInstance(BeanWithInjection.class).getInjectedValue());
  }

  /*
   * Qualifier tests
   */

  @Test
  public void shouldSupportQualifiers() {
    injector.register(BigRedBean.class);

    injector.register(BeanWithBigInjection.class);
    injector.register(BeanWithBigRedInjection.class);

    injector.remove(BeanWithBigInjection.class);
    injector.remove(BeanWithBigRedInjection.class);

    injector.remove(BigRedBean.class);
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringDependentBeanWithNoMatchForAllQualifiers() {
    injector.register(BigBean.class);

    assertThatThrownBy(() -> injector.register(BeanWithBigRedInjection.class))  // Won't match BigBean, so won't match anything
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringBeanWithMoreQualifiersWhenItWouldViolateSingularDependencies() {
    injector.register(BigBean.class);
    injector.register(BeanWithBigInjection.class);

    assertThatThrownBy(() -> injector.register(BigRedBean.class))  // Would also match injection for BeanWithBigInjection
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldThrowExceptionWhenRemovingRequiredBeanWithMoreQualifiers() {
    injector.register(BigRedBean.class);
    injector.register(BeanWithBigInjection.class);

    assertThatThrownBy(() -> injector.remove(BigRedBean.class))
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldGetBeanWithInjectionWithMultipleTypeMatchesWhenDisambiguatedWithQualifier() {
    injector.register(SimpleCollectionItemImpl3.class);  // One of several, this one qualified
    injector.register(BeanWithDirectRedCollectionItemDependency.class);  // Depends on the specific one above using qualifier

    assertNotNull(injector.getInstance(BeanWithDirectRedCollectionItemDependency.class));
  }

  /*
   * PostConstruct/PreDestroy tests
   */

  @Test
  public void shouldCallPostConstruct() {
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
  public void shouldInjectCollection() {
    BeanWithCollection bean = injector.getInstance(BeanWithCollection.class);

    assertNotNull(bean.getInjectedValues());
    assertEquals(2, bean.getInjectedValues().size());

    injector.remove(SimpleCollectionItemImpl2.class);

    BeanWithCollection bean2 = injector.getInstance(BeanWithCollection.class);

    assertEquals(2, bean.getInjectedValues().size());
    assertEquals(1, bean2.getInjectedValues().size());
  }

  @Test
  public void shouldInjectEmptyCollection() {
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
  public void shouldInjectCollectionProvider() {
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
    assertThrows(NoSuchInjectableException.class, () -> injector.remove(UnregisteredParentBean.class));
  }

  /*
   * Field injection
   */

  @Test
  public void shouldThrowExceptionWhenFinalFieldAnnotatedWithInject() throws NoSuchFieldException, SecurityException {
    assertThatThrownBy(() -> injector.register(FieldInjectionSampleWithAnnotatedFinalField.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Cannot inject final field: " + FieldInjectionSampleWithAnnotatedFinalField.class.getDeclaredField("injectedValue") + " in: " + FieldInjectionSampleWithAnnotatedFinalField.class)
      .hasNoCause();
  }

  /*
   * Constructor Injection
   */

  @Test
  public void shouldInjectConstructor() {
    injector.register(ConstructorInjectionSample.class);

    ConstructorInjectionSample instance = injector.getInstance(ConstructorInjectionSample.class);

    assertNotNull(instance);
    assertThat(instance.getInjectedValue(), isA(SimpleBean.class));
  }

  @Test
  public void shouldThrowExceptionWhenMultipleConstructorsAnnotatedWithInject() {
    assertThatThrownBy(() -> injector.register(ConstructorInjectionSampleWithMultipleAnnotatedConstructors.class))
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Multiple @Inject annotated constructors found, but only one allowed: " + ConstructorInjectionSampleWithMultipleAnnotatedConstructors.class)
      .hasNoCause();
  }

  /*
   * Provider use
   */

  @Test
  public void shouldRegisterAndUseProvider() {
    injector.registerInstance(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });

    assertEquals("a string", injector.getInstance(String.class));
  }

  @Test
  public void shouldRemoveProvider() {
    Provider<String> provider = new Provider<>() {
      @Override
      public String get() {
        return "a string";
      }
    };

    injector.registerInstance(provider);
    injector.removeInstance(provider);
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringProviderWouldViolateSingularDependencies() {
    assertThrows(ViolatesSingularDependencyException.class, () -> injector.registerInstance(new Provider<SimpleChildBean>() {
      @Override
      public SimpleChildBean get() {
        return new SimpleChildBean();
      }
    }));
  }

  @Test
  public void shouldThrowExceptionWhenRemovingSimilarButNotSameProvider() {
    injector.registerInstance(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });

    assertThrows(NoSuchInjectableException.class, () -> injector.removeInstance(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    }));
  }

  @Test
  public void shouldThrowExceptionWhenRemovingProviderByClass() {
    injector.registerInstance(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });

    assertThrows(NoSuchInjectableException.class, () -> injector.remove(String.class));
  }

  @Test
  public void shouldRegisterInstance() {
    injector.registerInstance(new String("hello there!"));
  }

  @Test
  public void shouldRegisterInstanceEvenWithBadAnnotations() {
    injector.registerInstance(new SampleWithMultipleAnnotatedConstructors());  // note, not a class, but an instantiated object!
  }

  @Test
  public void shouldRegisterInstanceEvenWithAnnotatedFinalFields() {
    injector.registerInstance(new SampleWithAnnotatedFinalFields());  // note, not a class, but an instantiated object!
  }

  @Test
  public void shouldInjectSuperClass() {
    injector.register(SubclassOfBeanWithInjection.class);

    SubclassOfBeanWithInjection bean = injector.getInstance(SubclassOfBeanWithInjection.class);

    assertEquals(injector.getInstance(SimpleBean.class), bean.getInjectedValue());
  }

  @Test
  public void shouldInjectSuperAndSubClassEvenIfFieldsAreSameName() {
    injector.register(SubclassOfBeanWithInjectionWithSameNamedInjection.class);

    SubclassOfBeanWithInjectionWithSameNamedInjection bean = injector.getInstance(SubclassOfBeanWithInjectionWithSameNamedInjection.class);
    SimpleBean simpleBean = injector.getInstance(SimpleBean.class);

    assertEquals(simpleBean, bean.getInjectedValue());
    assertEquals(simpleBean, bean.getInjectedValueInSubClass());
  }

  @Test
  public void shouldFindInstanceByAbstractSuperClass() {
    injector.register(SubclassOfAbstractBean.class);

    List<AbstractBean> beans = injector.getInstances(AbstractBean.class);

    assertEquals(1, beans.size());
  }

  @Test
  public void shouldInjectSameSingletonEachTime() {
    SimpleBean simpleBean = injector.getInstance(BeanWithInjection.class).getInjectedValue();

    assertEquals(simpleBean, injector.getInstance(BeanWithInjection.class).getInjectedValue());
  }

  @Test
  public void shouldThrowConstructionExceptionWhenPostConstructHasACircularDependency() {
    injector.register(BeanWithBadPostConstruct.class);
    injector.register(BeanDependentOnBeanWithBadPostConstruct.class);

    assertThatThrownBy(() -> injector.getInstance(BeanWithBadPostConstruct.class))
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("Exception in PostConstruct call: private void hs.ddif.core.InjectorTest$BeanWithBadPostConstruct.postConstruct()")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(InvocationTargetException.class)
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NullPointerException.class)
      .hasNoCause();
  }

  @Test
  @Disabled("This test is no longer valid; Providers are injected directly now (no indirection that could check their result) and so null instances can be part of the results if a Provider breaks its contract.")
  public void getInstancesShouldSilentlyIgnoreProvidersThatReturnNull() {
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
    @SuppressWarnings("unused")
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
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface() {
    // First register interface implementor, then interface provider
    injector.register(Bean1.class);
    injector.register(BeanThatNeedsInterface.class);

    assertThatThrownBy(() -> injector.register(Bean2.class))
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface_2() {
    // First register interface provider, then implementor
    injector.register(Bean2.class);
    injector.register(BeanThatNeedsInterface.class);

    assertThatThrownBy(() -> injector.register(Bean1.class))
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface_3() {
    // First register interface implementor, then indirect interface provider
    injector.register(Bean1.class);
    injector.register(BeanThatNeedsInterface.class);

    assertThatThrownBy(() -> injector.register(Bean3.class))
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface_4() {
    // First register indirect interface provider, then implementor
    injector.register(Bean3.class);
    injector.register(BeanThatNeedsInterface.class);

    assertThatThrownBy(() -> injector.register(Bean1.class))
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void shouldInjectCorrectProvider() {
    injector.register(Bean3.class);
    injector.register(BeanThatNeedsProviderOfSomeInterface.class);

    BeanThatNeedsProviderOfSomeInterface b = injector.getInstance(BeanThatNeedsProviderOfSomeInterface.class);

    assertTrue(SomeInterface.class.isInstance(b.callProvider()));
  }

  @Test
  public void shouldInjectCorrectProvider2() {
    injector.register(Bean3.class);
    injector.register(BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider.class);

    BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider b = injector.getInstance(BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider.class);

    assertTrue(Bean3.class.isInstance(b.callProvider()));
  }
}
