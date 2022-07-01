package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.int4.dirk.api.Injector;
import org.int4.dirk.api.TypeLiteral;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.RootInstanceFactory.RootInstance;
import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.core.test.scope.TestScope;
import org.int4.dirk.extensions.proxy.ByteBuddyProxyStrategy;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.Instance;
import org.int4.dirk.spi.instantiation.Resolution;
import org.int4.dirk.spi.scope.AbstractScopeResolver;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.TypeVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

public class DependentsLifeCycleTest {
  private static final List<String> POST_CONSTRUCTS = new ArrayList<>();
  private static final List<String> PRE_DESTROYS = new ArrayList<>();

  private final TestScopeResolver scopeResolver = new TestScopeResolver();

  private final Injector injector = InjectorBuilder.builder()
    .useDefaultTypeRegistrationExtensions()
    .useDefaultInjectionTargetExtensions()
    .proxyStrategy(new ByteBuddyProxyStrategy())
    .add(scopeResolver)
    .add(new InjectionTargetExtension<Instance<Object>, Object>(
      TypeVariables.get(Instance.class, 0),
      Resolution.LAZY,
      context -> context
    ))
    .add(new InjectionTargetExtension<Supplier<Object>, Object>(  // bad extension, LAZY but calls context immediately
      TypeVariables.get(Supplier.class, 0),
      Resolution.LAZY,
      context -> {
        context.get();
        return () -> "test";
      }
    ))
    .build();

  private String currentScope;

  @BeforeEach
  void beforeEach() {
    AbstractLifeCycleLogger.index = 0;

    POST_CONSTRUCTS.clear();
    PRE_DESTROYS.clear();

    InstanceFactory.useStrictOrdering();
  }

  public static class SubDependent extends AbstractLifeCycleLogger {
  }

  public static class Dependent extends AbstractLifeCycleLogger {
    @Inject SubDependent subDependent;
  }

  @Test
  void shouldDestroyDependent() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);

    Instance<Dependent> instance = injector.getInstance(new TypeLiteral<Instance<Dependent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    Dependent dependent = instance.get();

    assertPostConstructs("SubDependent-0", "Dependent-1");
    assertPreDestroys();

    instance.destroy(dependent);

    assertPostConstructs();
    assertPreDestroys("Dependent-1", "SubDependent-0");
  }

  public static class Container extends AbstractLifeCycleLogger {
    @Inject Dependent dependent;
  }

  @Test
  void shouldDestroyParentAndDependent() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(Container.class);

    Instance<Container> instance = injector.getInstance(new TypeLiteral<Instance<Container>>() {});

    assertPostConstructs();
    assertPreDestroys();

    Container parent = instance.get();

    assertPostConstructs("SubDependent-0", "Dependent-1", "Container-2");
    assertPreDestroys();

    instance.destroy(parent);

    assertPostConstructs();
    assertPreDestroys("Container-2", "Dependent-1", "SubDependent-0");
  }

  @Red
  public static class Parent extends AbstractLifeCycleLogger {
    @Inject Instance<Dependent> dependent;
  }

  @Test
  void shouldDestroyParentOnlyIfIndirectDependentNotUsed() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(Parent.class);

    Instance<Parent> instance = injector.getInstance(new TypeLiteral<Instance<Parent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    Parent parent = instance.get();

    assertPostConstructs("Parent-0");
    assertPreDestroys();

    instance.destroy(parent);

    assertPostConstructs();
    assertPreDestroys("Parent-0");
  }

  @Test
  void shouldDestroyParentAndAnyIndirectDependentsQueried() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(Parent.class);

    Instance<Parent> instance = injector.getInstance(new TypeLiteral<Instance<Parent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    Parent parent = instance.get();

    assertPostConstructs("Parent-0");
    assertPreDestroys();

    parent.dependent.get();
    parent.dependent.get();

    assertPostConstructs("SubDependent-1", "Dependent-2", "SubDependent-3", "Dependent-4");
    assertPreDestroys();

    instance.destroy(parent);

    assertPostConstructs();
    assertPreDestroys("Parent-0", "Dependent-2", "SubDependent-1", "Dependent-4", "SubDependent-3");
  }

  @Test
  void shouldDestroyParentOnlyWhenIndirectDependentsQueriedWereDestroyedAlready() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(Parent.class);

    Instance<Parent> instance = injector.getInstance(new TypeLiteral<Instance<Parent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    Parent parent = instance.get();

    assertPostConstructs("Parent-0");
    assertPreDestroys();

    Dependent dependent = parent.dependent.get();

    assertPostConstructs("SubDependent-1", "Dependent-2");
    assertPreDestroys();

    parent.dependent.destroy(dependent);

    assertPostConstructs();
    assertPreDestroys("Dependent-2", "SubDependent-1");

    instance.destroy(parent);

    assertPostConstructs();
    assertPreDestroys("Parent-0");
  }

  public static class GrandParent extends AbstractLifeCycleLogger {
    @Inject Instance<Parent> parent;
  }

  @Test
  void shouldDestroyAllUndestroyedChildrenWhenGrandParentDestroyed() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(Parent.class);
    injector.register(GrandParent.class);

    Instance<GrandParent> instance = injector.getInstance(new TypeLiteral<Instance<GrandParent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    GrandParent grandParent0 = instance.get();

    assertPostConstructs("GrandParent-0");
    assertPreDestroys();

    Parent parent1 = grandParent0.parent.get();

    assertPostConstructs("Parent-1");
    assertPreDestroys();

    Parent parent2 = grandParent0.parent.get();

    assertPostConstructs("Parent-2");
    assertPreDestroys();

    grandParent0.parent.get();

    assertPostConstructs("Parent-3");
    assertPreDestroys();

    grandParent0.parent.select(Annotations.of(Red.class)).get();

    assertPostConstructs("Parent-4");
    assertPreDestroys();

    parent1.dependent.get();
    Dependent dependent8 = parent2.dependent.get();
    parent2.dependent.get();

    assertPostConstructs("SubDependent-5", "Dependent-6", "SubDependent-7", "Dependent-8", "SubDependent-9", "Dependent-10");
    assertPreDestroys();

    parent1.dependent.destroy(dependent8);  // nothing happens, as dependent8 was not created with parent1

    assertPostConstructs();
    assertPreDestroys();

    parent2.dependent.destroy(dependent8);

    assertPostConstructs();
    assertPreDestroys("Dependent-8", "SubDependent-7");

    instance.destroy(grandParent0);

    assertPostConstructs();
    assertPreDestroys("GrandParent-0", "Parent-1", "Dependent-6", "SubDependent-5", "Parent-2", "Dependent-10", "SubDependent-9", "Parent-3", "Parent-4");
  }

  @Singleton
  public static class SingletonScoped extends AbstractLifeCycleLogger {
  }

  @Test
  void shouldDestroyWrappedTypes() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(SingletonScoped.class);

    Instance<List<AbstractLifeCycleLogger>> instance = injector.getInstance(new TypeLiteral<Instance<List<AbstractLifeCycleLogger>>>() {});

    assertPostConstructs();
    assertPreDestroys();

    List<AbstractLifeCycleLogger> list = instance.get();

    assertThat(list).hasSize(3);
    assertPostConstructs("SubDependent-0", "Dependent-1", "SingletonScoped-2", "SubDependent-3");
    assertPreDestroys();

    @SuppressWarnings("unchecked")
    RootInstance<List<AbstractLifeCycleLogger>, ?> castContext = (RootInstance<List<AbstractLifeCycleLogger>, ?>)instance;

    assertThat(castContext.hasContextFor(list)).isTrue();

    instance.destroy(list);

    assertPostConstructs();
    assertPreDestroys("Dependent-1", "SubDependent-0", "SubDependent-3");
  }

  @Test
  void shouldNotAllowDestroyingRewrappedTypes() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(SingletonScoped.class);

    Instance<List<AbstractLifeCycleLogger>> instance = injector.getInstance(new TypeLiteral<Instance<List<AbstractLifeCycleLogger>>>() {});

    assertPostConstructs();
    assertPreDestroys();

    List<AbstractLifeCycleLogger> list = instance.get();

    assertThat(list).hasSize(3);
    assertPostConstructs("SubDependent-0", "Dependent-1", "SingletonScoped-2", "SubDependent-3");
    assertPreDestroys();

    List<AbstractLifeCycleLogger> rewrappedList = new ArrayList<>(list);

    instance.destroy(rewrappedList);

    assertPostConstructs();
    assertPreDestroys();

    instance.destroy(list);  // has to be original for it to work

    assertPostConstructs();
    assertPreDestroys("Dependent-1", "SubDependent-0", "SubDependent-3");
  }

  @Test
  void shouldDestroyAll() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(SingletonScoped.class);

    Instance<AbstractLifeCycleLogger> instance = injector.getInstance(new TypeLiteral<Instance<AbstractLifeCycleLogger>>() {});

    assertPostConstructs();
    assertPreDestroys();

    List<AbstractLifeCycleLogger> list = instance.getAll();

    assertThat(list).hasSize(3);
    assertPostConstructs("SubDependent-0", "Dependent-1", "SingletonScoped-2", "SubDependent-3");
    assertPreDestroys();

    instance.destroyAll(list);

    assertPostConstructs();
    assertPreDestroys("Dependent-1", "SubDependent-0", "SubDependent-3");
  }

  public static class SingletonParent extends AbstractLifeCycleLogger {
    @Inject Instance<SingletonScoped> singletonScoped;
  }

  @Test
  void shouldNotDestroySingletonProducedByContextWhenDestroyingItsParent() {
    injector.register(SingletonScoped.class);
    injector.register(SingletonParent.class);

    Instance<SingletonParent> instance = injector.getInstance(new TypeLiteral<Instance<SingletonParent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    SingletonParent singletonParent = instance.get();

    assertPostConstructs("SingletonParent-0");
    assertPreDestroys();

    singletonParent.singletonScoped.get();
    singletonParent.singletonScoped.get();

    assertPostConstructs("SingletonScoped-1");
    assertPreDestroys();

    instance.destroy(singletonParent);

    assertPostConstructs();
    assertPreDestroys("SingletonParent-0");
  }

  public static class Indestructable {
    @Inject private DestructableDependent dependent;
  }

  public static class DestructableDependent extends AbstractLifeCycleLogger {
  }

  @Test
  void shouldTrackParentIfItHasDependentsThatNeedToBeDestroyed() {
    injector.register(DestructableDependent.class);
    injector.register(Indestructable.class);

    Instance<Indestructable> instance = injector.getInstance(new TypeLiteral<Instance<Indestructable>>() {});

    Indestructable i = instance.get();

    assertPostConstructs("DestructableDependent-0");
    assertPreDestroys();

    instance.destroy(i);

    assertPostConstructs();
    assertPreDestroys("DestructableDependent-0");
  }

  public static class SimpleElement {
  }

  @Test
  void shouldNotTrackListWhenNothingInItNeedsDestroying() {
    injector.register(SimpleElement.class);

    Instance<List<SimpleElement>> instance = injector.getInstance(new TypeLiteral<Instance<List<SimpleElement>>>() {});

    List<SimpleElement> list = instance.get();

    assertThat(list).hasSize(1);

    @SuppressWarnings("unchecked")
    RootInstance<List<SimpleElement>, ?> castContext = (RootInstance<List<SimpleElement>, ?>)instance;

    assertThat(castContext.hasContextFor(list)).isFalse();
  }

  @Singleton
  public static class SingletonWithDependent extends AbstractLifeCycleLogger {
    @Inject Dependent d;
  }

  @Test
  void shouldNotDestroySingletonWithDependent() {
    injector.register(List.of(SingletonWithDependent.class, Dependent.class, SubDependent.class));

    Instance<SingletonWithDependent> instance = injector.getInstance(new TypeLiteral<Instance<SingletonWithDependent>>() {});

    SingletonWithDependent s = instance.get();

    assertPostConstructs("SubDependent-0", "Dependent-1", "SingletonWithDependent-2");
    assertPreDestroys();

    instance.destroy(s);

    assertPostConstructs();
    assertPreDestroys();
  }

  @TestScope
  public static class TestScoped extends AbstractLifeCycleLogger {
    @Inject Dependent dependent;

    public Dependent getDependent() {
      return dependent;
    }
  }

  @Test
  void shouldDestroyDependentsWhenScopedObjectIsDestroyed() {
    injector.register(List.of(TestScoped.class, Dependent.class, SubDependent.class));

    Instance<TestScoped> instance = injector.getInstance(new TypeLiteral<Instance<TestScoped>>() {});

    assertThatThrownBy(() -> instance.get()).isExactlyInstanceOf(ScopeNotActiveException.class);

    currentScope = "A";

    assertPostConstructs();
    assertPreDestroys();

    TestScoped testScoped = instance.get();

    assertPostConstructs("SubDependent-0", "Dependent-1", "TestScoped-2");
    assertPreDestroys();

    instance.destroy(testScoped);  // expect nothing, scoped objects cannot be destroyed this way

    assertPostConstructs();
    assertPreDestroys();

    scopeResolver.deleteScope("A");

    assertPostConstructs();
    assertPreDestroys("TestScoped-2", "Dependent-1", "SubDependent-0");
  }

  public static class DependentOnTestScoped extends AbstractLifeCycleLogger {
    @Inject TestScoped testScoped;  // will be a proxy
  }

  @Test
  void shouldDestroyDependentsCreatedByProxyWhenScopedIsDestroyed() {
    injector.register(List.of(DependentOnTestScoped.class, TestScoped.class, Dependent.class, SubDependent.class));

    Instance<DependentOnTestScoped> instance = injector.getInstance(new TypeLiteral<Instance<DependentOnTestScoped>>() {});

    DependentOnTestScoped root = instance.get();

    assertPostConstructs("DependentOnTestScoped-0");
    assertPreDestroys();

    assertThatThrownBy(() -> root.testScoped.getDependent()).isExactlyInstanceOf(ScopeNotActiveException.class);

    currentScope = "A";

    root.testScoped.getDependent();  // proxy should create a TestScoped instance now

    assertPostConstructs("SubDependent-1", "Dependent-2", "TestScoped-3");
    assertPreDestroys();

    instance.destroy(root);  // expect only root to be destroyed, proxied objects cannot be destroyed this way

    assertPostConstructs();
    assertPreDestroys("DependentOnTestScoped-0");

    scopeResolver.deleteScope("A");

    assertPostConstructs();
    assertPreDestroys("TestScoped-3", "Dependent-2", "SubDependent-1");
  }

  public static class BadExtensionUser extends AbstractLifeCycleLogger {
    @Inject Supplier<Dependent> supplier;
  }

  @Test
  void shouldDetectBadLazyExtensions() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(BadExtensionUser.class);

    assertThatThrownBy(() -> injector.getInstance(BadExtensionUser.class))
      .isExactlyInstanceOf(CreationException.class)
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasMessage("Incorrectly implemented extension. Lazy extensions are not allowed to access the creational context during instance creation!")
      .hasNoCause();
  }

  public static class BadExtensionUserWithSingleton extends AbstractLifeCycleLogger {
    @Inject Supplier<String> supplier;
  }

  @Test
  void shouldDetectBadLazyExtensionsWithSingleton() {
    injector.registerInstance("A");
    injector.register(BadExtensionUserWithSingleton.class);

    assertThatThrownBy(() -> injector.getInstance(BadExtensionUserWithSingleton.class))
      .isExactlyInstanceOf(CreationException.class)
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasMessage("Incorrectly implemented extension. Lazy extensions are not allowed to access the creational context during instance creation!")
      .hasNoCause();
  }

  private static void assertPostConstructs(String... names) {
    assertThat(POST_CONSTRUCTS).containsExactly(names);

    POST_CONSTRUCTS.clear();
  }

  private static void assertPreDestroys(String... names) {
    assertThat(PRE_DESTROYS)
      .withFailMessage(Arrays.toString(names) + " vs " + PRE_DESTROYS)
      .containsExactly(names);

    PRE_DESTROYS.clear();
  }

  public static abstract class AbstractLifeCycleLogger {
    private static int index;

    private String constructName;

    @PostConstruct
    void postConstruct() {
      constructName = getClass().getSimpleName() + "-" + index++;

      POST_CONSTRUCTS.add(constructName);
    }

    @PreDestroy
    void preDestroy() {
      PRE_DESTROYS.add(constructName);
    }

    @Override
    public String toString() {
      return constructName;
    }
  }

  private class TestScopeResolver extends AbstractScopeResolver<String> {
    @Override
    public Annotation getAnnotation() {
      return Annotations.of(TestScope.class);
    }

    @Override
    protected String getCurrentScope() {
      return currentScope;
    }

    public void deleteScope(String scope) {
      destroyScope(scope);
    }
  }
}
