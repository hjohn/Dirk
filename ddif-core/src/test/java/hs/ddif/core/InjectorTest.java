package hs.ddif.core;

import hs.ddif.core.inject.consistency.UnresolvableDependencyException;
import hs.ddif.core.inject.consistency.ViolatesSingularDependencyException;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ConstructionException;
import hs.ddif.core.store.DuplicateBeanException;
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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InjectorTest {
  private Injector injector;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void before() {
    injector = new Injector();

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
  public void shouldGetSimpleBean() throws BeanResolutionException {
    assertNotNull(injector.getInstance(SimpleBean.class));
  }

  @Test(expected = BeanResolutionException.class)
  public void shouldThrowExceptionWhenGettingUnregisteredBean() throws BeanResolutionException {
    injector.getInstance(ArrayList.class);
  }

  @Test(expected = BeanResolutionException.class)
  public void shouldThrowExceptionWhenBeanIsAmbigious() throws BeanResolutionException {
    injector.getInstance(SimpleCollectionItemInterface.class);
  }

  @Test
  public void shouldGetBeanWithInjection() throws BeanResolutionException {
    BeanWithInjection bean = injector.getInstance(BeanWithInjection.class);

    assertNotNull(bean.getInjectedValue());
    assertEquals(SimpleBean.class, bean.getInjectedValue().getClass());
  }

  @Test
  public void shouldGetBeanWithInterfaceBasedInjection() throws BeanResolutionException {
    BeanWithInterfaceBasedInjection bean = injector.getInstance(BeanWithInterfaceBasedInjection.class);

    assertNotNull(bean.getInjectedValue());
    assertEquals(SimpleImpl.class, bean.getInjectedValue().getClass());
  }

  @Test
  public void shouldGetBeanWithProviderInjection() throws BeanResolutionException {
    BeanWithProvider bean = injector.getInstance(BeanWithProvider.class);

    assertNotNull(bean.getSimpleBean());
    assertEquals(SimpleBean.class, bean.getSimpleBean().getClass());
  }

  @Test
  public void shouldGetBeanWithOptionalDependencyWhenProviderReturnsNull() throws BeanResolutionException {
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
  public void shouldGetBeanWithOptionalDependencyWhenNoProviderAvailable() throws BeanResolutionException {
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
  public void shouldLeaveDefaultFieldValuesIntactForOptionalDependencies() throws BeanResolutionException {
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
  public void shouldGetBeanWithOptionalConstructorDependencyWhenNoProviderAvailable() throws BeanResolutionException {
    injector.register(BeanWithOptionalConstructorDependency.class);

    assertNotNull(injector.getInstance(BeanWithOptionalConstructorDependency.class));
  }

  @Test  // @Nullable annotation on Provider is just ignored as it makes no sense for Providers
  public void shouldGetBeanWithOptionalProviderDependency() throws BeanResolutionException {
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

  @Test  // Providers cannot be made optional with @Nullable as a provider can always be created; it's only the result of the Provider that could be optional but that requires a different approach (new annotation of sub-interface)
  public void shouldThrowUnresolvedDependencyExceptionWhenRegisteringBeanWithNullableProviderDependencyWhenNoProviderAvailableBecauseProvidersCannotBeMadeOptional() {
    thrown.expect(UnresolvableDependencyException.class);

    injector.register(BeanWithUnsupportedOptionalProviderDependency.class);
  }

  @Test(expected = BeanResolutionException.class)
  public void shouldThrowExceptionWhenGettingUnavailableBean() throws BeanResolutionException {
    injector.registerInstance(new Provider<UnavailableBean>() {
      @Override
      public UnavailableBean get() {
        return null;
      }
    });

    injector.getInstance(UnavailableBean.class);
  }

  /*
   * Injector#remove tests:
   */

  @Test
  public void shouldRemoveBean() {
    injector.remove(BeanWithInjection.class);

    try {
      injector.getInstance(BeanWithInjection.class);
      fail();
    }
    catch(BeanResolutionException e) {
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenRemovingInterface() {
    injector.remove(SimpleInterface.class);
  }

  @Test(expected = NoSuchInjectableException.class)
  public void shouldThrowExceptionWhenRemovingUnregisteredBean() {
    injector.remove(ArrayList.class);
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldThrowExceptionWhenRemovingBeanWouldViolateSingularDependencies() {
    injector.remove(SimpleBean.class);
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldRepeatedlyThrowExceptionWhenRemovingBeanWouldViolateSingularDependencies() {
    try {
      injector.remove(SimpleBean.class);
      fail();
    }
    catch(UnresolvableDependencyException e) {
      // expected
    }

    injector.remove(SimpleBean.class);
  }

  @Test
  public void shouldBeAbleToRemoveProviderWhichIsOnlyOptionallyDependedOn() throws BeanResolutionException {
    Provider<UnavailableBean> provider = new Provider<UnavailableBean>() {
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
  }

  @Test
  public void shouldRegisterSameStringInstancesWithDifferentQualifiers() {
    injector.registerInstance("a", AnnotationDescriptor.describe(Named.class, new Value("value", "name-1")));
    injector.registerInstance("a", AnnotationDescriptor.describe(Named.class, new Value("value", "name-2")));
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringDuplicate() {
    injector.register(String.class);

    thrown.expect(DuplicateBeanException.class);

    injector.register(String.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenRegisteringInterface() {
    injector.register(List.class);
  }

  @Test(expected = UnresolvableDependencyException.class)
  public void shouldThrowExceptionWhenRegisteringBeanWithUnresolvedDependencies() {
    injector.register(BeanWithUnresolvedDependency.class);
  }

  @Test(expected = UnresolvableDependencyException.class)
  public void shouldThrowExceptionWhenRegisteringBeanWithUnresolvedProviderDependencies() {
    injector.register(BeanWithUnresolvedProviderDependency.class);
  }

  @Test(expected = UnresolvableDependencyException.class)
  public void shouldThrowExceptionWhenRegisteringBeanWithAmbigiousDependencies() {
    injector.register(BeanWithDirectCollectionItemDependency.class);
  }

  @Test(expected = UnresolvableDependencyException.class)
  public void shouldRepeatedlyThrowExceptionWhenRegisteringBeanWithUnresolvedDependencies() {
    try {
      injector.register(BeanWithUnresolvedDependency.class);
      fail();
    }
    catch(UnresolvableDependencyException e) {
      // expected
    }

    injector.register(BeanWithUnresolvedDependency.class);
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldThrowExceptionWhenRegisteringBeanWouldViolateSingularDependencies() {
    injector.register(SimpleChildBean.class);
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldRepeatedlyThrowExceptionWhenRegisteringBeanWouldViolateSingularDependencies() {
    try {
      injector.register(SimpleChildBean.class);
      fail();
    }
    catch(UnresolvableDependencyException e) {
      // expected
    }

    injector.register(SimpleChildBean.class);
  }

  /*
   * Singleton tests
   */

  @Test
  public void shouldRespectSingletonAnnotation() throws BeanResolutionException {
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

    thrown.expect(UnresolvableDependencyException.class);

    injector.register(BeanWithBigRedInjection.class);  // Won't match BigBean, so won't match anything
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringBeanWithMoreQualifiersWhenItWouldViolateSingularDependencies() {
    injector.register(BigBean.class);
    injector.register(BeanWithBigInjection.class);

    thrown.expect(ViolatesSingularDependencyException.class);

    injector.register(BigRedBean.class);  // Would also match injection for BeanWithBigInjection
  }

  @Test
  public void shouldThrowExceptionWhenRemovingRequiredBeanWithMoreQualifiers() {
    injector.register(BigRedBean.class);
    injector.register(BeanWithBigInjection.class);

    thrown.expect(ViolatesSingularDependencyException.class);

    injector.remove(BigRedBean.class);
  }

  @Test
  public void shouldGetBeanWithInjectionWithMultipleTypeMatchesWhenDisambiguatedWithQualifier() throws BeanResolutionException {
    injector.register(SimpleCollectionItemImpl3.class);  // One of several, this one qualified
    injector.register(BeanWithDirectRedCollectionItemDependency.class);  // Depends on the specific one above using qualifier

    assertNotNull(injector.getInstance(BeanWithDirectRedCollectionItemDependency.class));
  }

  /*
   * PostConstruct/PreDestroy tests
   */

  @Test
  public void shouldCallPostConstruct() throws BeanResolutionException {
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
  public void shouldInjectCollection() throws BeanResolutionException {
    BeanWithCollection bean = injector.getInstance(BeanWithCollection.class);

    assertNotNull(bean.getInjectedValues());
    assertEquals(2, bean.getInjectedValues().size());

    injector.remove(SimpleCollectionItemImpl2.class);

    BeanWithCollection bean2 = injector.getInstance(BeanWithCollection.class);

    assertEquals(2, bean.getInjectedValues().size());
    assertEquals(1, bean2.getInjectedValues().size());
  }

  @Test
  public void shouldInjectEmptyCollection() throws BeanResolutionException {
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
  public void shouldInjectCollectionProvider() throws BeanResolutionException {
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

  @Test(expected = NoSuchInjectableException.class)
  public void shouldThrowExceptionWhenRemovingUnregisteredSuperClass() {
    injector.remove(UnregisteredParentBean.class);
  }

  /*
   * Field injection
   */

  @Test
  public void shouldThrowExceptionWhenFinalFieldAnnotatedWithInject() throws NoSuchFieldException, SecurityException {
    thrown.expect(BindingException.class);
    thrown.expectMessage("Cannot inject final field: " + FieldInjectionSampleWithAnnotatedFinalField.class.getDeclaredField("injectedValue") + " in: " + FieldInjectionSampleWithAnnotatedFinalField.class);

    injector.register(FieldInjectionSampleWithAnnotatedFinalField.class);
  }

  /*
   * Constructor Injection
   */

  @Test
  public void shouldInjectConstructor() throws BeanResolutionException {
    injector.register(ConstructorInjectionSample.class);

    ConstructorInjectionSample instance = injector.getInstance(ConstructorInjectionSample.class);

    assertNotNull(instance);
    assertThat(instance.getInjectedValue(), isA(SimpleBean.class));
  }

  @Test
  public void shouldThrowExceptionWhenMultipleConstructorsAnnotatedWithInject() {
    thrown.expect(BindingException.class);
    thrown.expectMessage("Multiple @Inject annotated constructors found, but only one allowed: " + ConstructorInjectionSampleWithMultipleAnnotatedConstructors.class);

    injector.register(ConstructorInjectionSampleWithMultipleAnnotatedConstructors.class);
  }

  /*
   * Provider use
   */

  @Test
  public void shouldRegisterAndUseProvider() throws BeanResolutionException {
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
    Provider<String> provider = new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    };

    injector.registerInstance(provider);
    injector.removeInstance(provider);
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldThrowExceptionWhenRegisteringProviderWouldViolateSingularDependencies() {
    injector.registerInstance(new Provider<SimpleChildBean>() {
      @Override
      public SimpleChildBean get() {
        return new SimpleChildBean();
      }
    });
  }

  @Test(expected = NoSuchInjectableException.class)
  public void shouldThrowExceptionWhenRemovingSimilarProvider() {
    injector.registerInstance(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });

    injector.removeInstance(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });
  }

  @Test(expected = NoSuchInjectableException.class)
  public void shouldThrowExceptionWhenRemovingProviderByClass() {
    injector.registerInstance(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });

    injector.remove(String.class);
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
  public void shouldInjectSuperClass() throws BeanResolutionException {
    injector.register(SubclassOfBeanWithInjection.class);

    SubclassOfBeanWithInjection bean = injector.getInstance(SubclassOfBeanWithInjection.class);

    assertEquals(injector.getInstance(SimpleBean.class), bean.getInjectedValue());
  }

  @Test
  public void shouldInjectSuperAndSubClassEvenIfFieldsAreSameName() throws BeanResolutionException {
    injector.register(SubclassOfBeanWithInjectionWithSameNamedInjection.class);

    SubclassOfBeanWithInjectionWithSameNamedInjection bean = injector.getInstance(SubclassOfBeanWithInjectionWithSameNamedInjection.class);
    SimpleBean simpleBean = injector.getInstance(SimpleBean.class);

    assertEquals(simpleBean, bean.getInjectedValue());
    assertEquals(simpleBean, bean.getInjectedValueInSubClass());
  }

  @Test
  public void shouldFindInstanceByAbstractSuperClass() throws BeanResolutionException {
    injector.register(SubclassOfAbstractBean.class);

    Set<AbstractBean> beans = injector.getInstances(AbstractBean.class);

    assertEquals(1, beans.size());
  }

  @Test
  public void shouldInjectSameSingletonEachTime() throws BeanResolutionException {
    SimpleBean simpleBean = injector.getInstance(BeanWithInjection.class).getInjectedValue();

    assertEquals(simpleBean, injector.getInstance(BeanWithInjection.class).getInjectedValue());
  }

  @Test(expected = ConstructionException.class)
  public void shouldThrowConstructionExceptionWhenPostConstructHasACircularDependency() throws BeanResolutionException {
    injector.register(BeanWithBadPostConstruct.class);
    injector.register(BeanDependentOnBeanWithBadPostConstruct.class);

    injector.getInstance(BeanWithBadPostConstruct.class);
  }

  @Test
  @Ignore("This test is no longer valid; Providers are injected directly now (no indirection that could check their result) and so null instances can be part of the results if a Provider breaks its contract.")
  public void getInstancesShouldSilentlyIgnoreProvidersThatReturnNull() throws BeanResolutionException {
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

    thrown.expect(ViolatesSingularDependencyException.class);

    injector.register(Bean2.class);
  }

  @Test
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface_2() {
    // First register interface provider, then implementor
    injector.register(Bean2.class);
    injector.register(BeanThatNeedsInterface.class);

    thrown.expect(ViolatesSingularDependencyException.class);

    injector.register(Bean1.class);
  }

  @Test
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface_3() {
    // First register interface implementor, then indirect interface provider
    injector.register(Bean1.class);
    injector.register(BeanThatNeedsInterface.class);

    thrown.expect(ViolatesSingularDependencyException.class);

    injector.register(Bean3.class);
  }

  @Test
  public void shouldNotAllowMultipleBeansProvidingSameInterfaceToBeRegisteredWhenThereIsABeanWithASingularDependencyOnSaidInterface_4() {
    // First register indirect interface provider, then implementor
    injector.register(Bean3.class);
    injector.register(BeanThatNeedsInterface.class);

    thrown.expect(ViolatesSingularDependencyException.class);

    injector.register(Bean1.class);
  }

  @Test
  public void shouldInjectCorrectProvider() throws BeanResolutionException {
    injector.register(Bean3.class);
    injector.register(BeanThatNeedsProviderOfSomeInterface.class);

    BeanThatNeedsProviderOfSomeInterface b = injector.getInstance(BeanThatNeedsProviderOfSomeInterface.class);

    assertTrue(SomeInterface.class.isInstance(b.callProvider()));
  }

  @Test
  public void shouldInjectCorrectProvider2() throws BeanResolutionException {
    injector.register(Bean3.class);
    injector.register(BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider.class);

    BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider b = injector.getInstance(BeanThatNeedsProviderOfSomethingThatIsAlsoAProvider.class);

    assertTrue(Bean3.class.isInstance(b.callProvider()));
  }
}
