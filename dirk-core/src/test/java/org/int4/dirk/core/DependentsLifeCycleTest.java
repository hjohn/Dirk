package org.int4.dirk.core;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.int4.dirk.api.Injector;
import org.int4.dirk.api.TypeLiteral;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.RootInstantiationContextFactory.RootInstantiationContext;
import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.InstantiationContext;
import org.int4.dirk.spi.instantiation.TypeTrait;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

public class DependentsLifeCycleTest {
  private static final List<String> POST_CONSTRUCTS = new ArrayList<>();
  private static final List<String> PRE_DESTROYS = new ArrayList<>();

  private final Injector injector = InjectorBuilder.builder()
    .useDefaultTypeRegistrationExtensions()
    .useDefaultInjectionTargetExtensions()
    .add(new InjectionTargetExtension<InstantiationContext<Object>, Object>() {
      @Override
      public Class<?> getTargetClass() {
        return InstantiationContext.class;
      }

      @Override
      public Type getElementType(Type type) {
        return Types.getTypeParameter(type, InstantiationContext.class, InstantiationContext.class.getTypeParameters()[0]);
      }

      @Override
      public Set<TypeTrait> getTypeTraits() {
        return EnumSet.of(TypeTrait.LAZY);
      }

      @Override
      public InstantiationContext<Object> getInstance(InstantiationContext<Object> context) throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException, ScopeNotActiveException {
        return context;
      }
    })
    .build();

  @BeforeEach
  void beforeEach() {
    AbstractLifeCycleLogger.index = 0;

    POST_CONSTRUCTS.clear();
    PRE_DESTROYS.clear();

    InstantiationContextFactory.useStrictOrdering();
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

    InstantiationContext<Dependent> instance = injector.getInstance(new TypeLiteral<InstantiationContext<Dependent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    Dependent dependent = instance.create();

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

    InstantiationContext<Container> instance = injector.getInstance(new TypeLiteral<InstantiationContext<Container>>() {});

    assertPostConstructs();
    assertPreDestroys();

    Container parent = instance.create();

    assertPostConstructs("SubDependent-0", "Dependent-1", "Container-2");
    assertPreDestroys();

    instance.destroy(parent);

    assertPostConstructs();
    assertPreDestroys("Container-2", "Dependent-1", "SubDependent-0");
  }

  @Red
  public static class Parent extends AbstractLifeCycleLogger {
    @Inject InstantiationContext<Dependent> dependent;
  }

  @Test
  void shouldDestroyParentOnlyIfIndirectDependentNotUsed() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(Parent.class);

    InstantiationContext<Parent> instance = injector.getInstance(new TypeLiteral<InstantiationContext<Parent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    Parent parent = instance.create();

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

    InstantiationContext<Parent> instance = injector.getInstance(new TypeLiteral<InstantiationContext<Parent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    Parent parent = instance.create();

    assertPostConstructs("Parent-0");
    assertPreDestroys();

    parent.dependent.create();
    parent.dependent.create();

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

    InstantiationContext<Parent> instance = injector.getInstance(new TypeLiteral<InstantiationContext<Parent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    Parent parent = instance.create();

    assertPostConstructs("Parent-0");
    assertPreDestroys();

    Dependent dependent = parent.dependent.create();

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
    @Inject InstantiationContext<Parent> parent;
  }

  @Test
  void shouldDestroyAllUndestroyedChildrenWhenGrandParentDestroyed() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(Parent.class);
    injector.register(GrandParent.class);

    InstantiationContext<GrandParent> instance = injector.getInstance(new TypeLiteral<InstantiationContext<GrandParent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    GrandParent grandParent0 = instance.create();

    assertPostConstructs("GrandParent-0");
    assertPreDestroys();

    Parent parent1 = grandParent0.parent.create();

    assertPostConstructs("Parent-1");
    assertPreDestroys();

    Parent parent2 = grandParent0.parent.create();

    assertPostConstructs("Parent-2");
    assertPreDestroys();

    grandParent0.parent.create();

    assertPostConstructs("Parent-3");
    assertPreDestroys();

    grandParent0.parent.select(Annotations.of(Red.class)).create();

    assertPostConstructs("Parent-4");
    assertPreDestroys();

    parent1.dependent.create();
    Dependent dependent8 = parent2.dependent.create();
    parent2.dependent.create();

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

    InstantiationContext<List<AbstractLifeCycleLogger>> context = injector.getInstance(new TypeLiteral<InstantiationContext<List<AbstractLifeCycleLogger>>>() {});

    assertPostConstructs();
    assertPreDestroys();

    List<AbstractLifeCycleLogger> list = context.create();

    assertThat(list).hasSize(3);
    assertPostConstructs("SubDependent-0", "Dependent-1", "SingletonScoped-2", "SubDependent-3");
    assertPreDestroys();

    @SuppressWarnings("unchecked")
    RootInstantiationContext<List<AbstractLifeCycleLogger>, ?> castContext = (RootInstantiationContext<List<AbstractLifeCycleLogger>, ?>)context;

    assertThat(castContext.hasContextFor(list)).isTrue();

    context.destroy(list);

    assertPostConstructs();
    assertPreDestroys("Dependent-1", "SubDependent-0", "SubDependent-3");
  }

  @Test
  void shouldNotAllowDestroyingRewrappedTypes() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(SingletonScoped.class);

    InstantiationContext<List<AbstractLifeCycleLogger>> context = injector.getInstance(new TypeLiteral<InstantiationContext<List<AbstractLifeCycleLogger>>>() {});

    assertPostConstructs();
    assertPreDestroys();

    List<AbstractLifeCycleLogger> list = context.create();

    assertThat(list).hasSize(3);
    assertPostConstructs("SubDependent-0", "Dependent-1", "SingletonScoped-2", "SubDependent-3");
    assertPreDestroys();

    List<AbstractLifeCycleLogger> rewrappedList = new ArrayList<>(list);

    context.destroy(rewrappedList);

    assertPostConstructs();
    assertPreDestroys();

    context.destroy(list);  // has to be original for it to work

    assertPostConstructs();
    assertPreDestroys("Dependent-1", "SubDependent-0", "SubDependent-3");
  }

  @Test
  void shouldDestroyAll() {
    injector.register(SubDependent.class);
    injector.register(Dependent.class);
    injector.register(SingletonScoped.class);

    InstantiationContext<AbstractLifeCycleLogger> context = injector.getInstance(new TypeLiteral<InstantiationContext<AbstractLifeCycleLogger>>() {});

    assertPostConstructs();
    assertPreDestroys();

    List<AbstractLifeCycleLogger> list = context.createAll();

    assertThat(list).hasSize(3);
    assertPostConstructs("SubDependent-0", "Dependent-1", "SingletonScoped-2", "SubDependent-3");
    assertPreDestroys();

    context.destroyAll(list);

    assertPostConstructs();
    assertPreDestroys("Dependent-1", "SubDependent-0", "SubDependent-3");
  }

  public static class SingletonParent extends AbstractLifeCycleLogger {
    @Inject InstantiationContext<SingletonScoped> singletonScoped;
  }

  @Test
  void shouldNotDestroySingletonProducedByContextWhenDestroyingItsParent() {
    injector.register(SingletonScoped.class);
    injector.register(SingletonParent.class);

    InstantiationContext<SingletonParent> context = injector.getInstance(new TypeLiteral<InstantiationContext<SingletonParent>>() {});

    assertPostConstructs();
    assertPreDestroys();

    SingletonParent singletonParent = context.create();

    assertPostConstructs("SingletonParent-0");
    assertPreDestroys();

    singletonParent.singletonScoped.create();
    singletonParent.singletonScoped.create();

    assertPostConstructs("SingletonScoped-1");
    assertPreDestroys();

    context.destroy(singletonParent);

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

    InstantiationContext<Indestructable> context = injector.getInstance(new TypeLiteral<InstantiationContext<Indestructable>>() {});

    Indestructable i = context.create();

    assertPostConstructs("DestructableDependent-0");
    assertPreDestroys();

    context.destroy(i);

    assertPostConstructs();
    assertPreDestroys("DestructableDependent-0");
  }

  public static class SimpleElement {
  }

  @Test
  void shouldNotTrackListWhenNothingInItNeedsDestroying() {
    injector.register(SimpleElement.class);

    InstantiationContext<List<SimpleElement>> context = injector.getInstance(new TypeLiteral<InstantiationContext<List<SimpleElement>>>() {});

    List<SimpleElement> list = context.create();

    assertThat(list).hasSize(1);

    @SuppressWarnings("unchecked")
    RootInstantiationContext<List<SimpleElement>, ?> castContext = (RootInstantiationContext<List<SimpleElement>, ?>)context;

    assertThat(castContext.hasContextFor(list)).isFalse();
  }

  @Singleton
  public static class SingletonWithDependent extends AbstractLifeCycleLogger {
    @Inject Dependent d;
  }

  @Test
  void shouldNotDestroySingletonWithDependent() {
    injector.register(List.of(SingletonWithDependent.class, Dependent.class, SubDependent.class));

    InstantiationContext<SingletonWithDependent> context = injector.getInstance(new TypeLiteral<InstantiationContext<SingletonWithDependent>>() {});

    SingletonWithDependent s = context.create();

    assertPostConstructs("SubDependent-0", "Dependent-1", "SingletonWithDependent-2");
    assertPreDestroys();

    context.destroy(s);

    assertPostConstructs();
    assertPreDestroys();
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
}
