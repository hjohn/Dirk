package hs.ddif.core.inject.consistency;

import hs.ddif.core.Injector;
import hs.ddif.core.Injectors;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.InstanceCreationFailure;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.instantiator.Key;
import hs.ddif.core.inject.instantiator.MultipleInstances;
import hs.ddif.core.inject.instantiator.NoSuchInstance;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.SingletonScopeResolver;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.Annotations;
import hs.ddif.core.util.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.TypeFactory;

public class InjectorStoreConsistencyPolicyStressTest {
  private final Random rnd = new Random(4);

  @Test
  void shouldSurviveStressTest() {
    Injector injector = Injectors.manual();

    List<Type> actual = new ArrayList<>();
    List<Type> classes = new ArrayList<>(List.of(A.class, B.class, C.class, D.class, E.class, F.class, G.class, H.class, I.class, J.class, K.class));
    Set<Type> classesNeverRegistered = new HashSet<>(classes);
    int total = 20000;
    int cyclics = 0;
    int successes = 0;

    for(int i = 0; i < total; i++) {
      Collections.shuffle(classes, rnd);
      int count = rnd.nextInt(classes.size() - 1) + 1;
      List<Type> list = new ArrayList<>(classes.subList(0, count));

      try {
        double x = rnd.nextDouble();

        if(x < 0.01) {
          injector.getCandidateRegistry().register(list);
          actual.addAll(list);
          classesNeverRegistered.removeAll(list);
        }
        else if(x < 0.02) {
          injector.getCandidateRegistry().remove(list);
          actual.removeAll(list);
        }
        else if(x < 0.03) {
          injector.getCandidateRegistry().remove(new ArrayList<>(actual));
          actual.clear();
        }
        else if((x < 0.55 || actual.size() == 0) && actual.size() < classes.size()) {
          List<Type> toAdd = new ArrayList<>(classes);

          toAdd.removeAll(actual);

          Collections.shuffle(toAdd, rnd);

          int amount = rnd.nextInt((toAdd.size() - 1) / 2) + 1;

          toAdd = toAdd.subList(0, amount);
          injector.getCandidateRegistry().register(toAdd);
          actual.addAll(toAdd);
          classesNeverRegistered.removeAll(toAdd);
        }
        else {
          List<Type> toRemove = new ArrayList<>(actual);

          Collections.shuffle(toRemove, rnd);

          int amount = rnd.nextInt((toRemove.size() - 1) / 2) + 1;

          toRemove = toRemove.subList(0, amount);
          injector.getCandidateRegistry().remove(toRemove);
          actual.removeAll(toRemove);
        }

        successes++;
      }
      catch(IllegalStateException | AssertionError e) {
        throw e;
      }
      catch(CyclicDependencyException e) {
        cyclics++;
      }
      catch(Exception e) {
        // ignore
      }

      for(Type type : classes) {
        boolean contains = injector.contains(type);
        if(contains) {
          contains = injector.getInstances(type).stream().map(Object::getClass).anyMatch(c -> c.equals(type));
        }

        if(actual.contains(type)) {
          assertTrue("Expected " + type, contains);
        }
        else {
          assertFalse("Did not expect " + type, contains);
        }
      }
    }

    assertTrue("Expected 20%+ succesful calls", successes > total / 5);
    assertTrue("Expected 10+ cyclic exceptions (attempts to register D)", cyclics > 10);
    assertEquals(Set.of(D.class), classesNeverRegistered, "All classes except D must be registered at some point, but weren't: " + classesNeverRegistered);
  }

  @Test
  void largeGraphTest() throws AnnotationFormatException {
    InjectorStoreConsistencyPolicy<ResolvableInjectable> policy = new InjectorStoreConsistencyPolicy<>(new SingletonScopeResolver());
    InjectableStore<ResolvableInjectable> store = new InjectableStore<>(policy);
    List<ResolvableInjectable> knownInjectables = new ArrayList<>();
    List<Class<?>> classes = List.of(String.class, Integer.class, A.class, B.class, C.class, D.class, E.class, F.class, G.class, H.class, I.class, J.class);

    Singleton annotation = TypeFactory.annotation(Singleton.class, Map.of());

    for(int i = 0; i < 10000; i++) {
      policy.checkInvariants();

      int randomBindings = Math.min(rnd.nextInt(4), knownInjectables.size());
      List<Binding> bindings = new ArrayList<>();

      for(int j = 0; j < randomBindings; j++) {
        ResolvableInjectable target = knownInjectables.get(rnd.nextInt(knownInjectables.size()));

        bindings.add(new SimpleBinding(new Key(target.getType(), target.getQualifiers())));
      }

      ResolvableInjectable injectable = new ResolvableInjectable(
        classes.get(rnd.nextInt(classes.size())),
        Set.of(Annotations.named("instance-" + i)),
        bindings,
        annotation,
        null,
        (a, b) -> null
      );

      store.put(injectable);

      knownInjectables.add(injectable);
    }
  }

  private static class SimpleBinding implements Binding {
    private final Key key;

    public SimpleBinding(Key key) {
      this.key = key;
    }

    @Override
    public Key getKey() {
      return key;
    }

    @Override
    public AccessibleObject getAccessibleObject() {
      return null;
    }

    @Override
    public boolean isCollection() {
      return false;
    }

    @Override
    public boolean isDirect() {
      return true;
    }

    @Override
    public boolean isOptional() {
      return false;
    }

    @Override
    public boolean isParameter() {
      return false;
    }

    @Override
    public Object getValue(Instantiator instantiator) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance, OutOfScopeException {
      return null;
    }
  }

  interface Z {
  }

  public static class A {
  }

  public static class E extends A {
  }

  public static class B {
    @Inject Z z;  // cyclic if it is D, not if it is H or K
  }

  public static class C {
    @Inject B b;
    @Inject Provider<D> d;  // not a circular dependency
  }

  public static class D implements Z {
    @Inject C c;
  }

  public static class F {
    @Inject B b;
    @Inject C c;
    @Inject @Nullable Z z;
  }

  public static class G {
    @Inject K k;
  }

  public static class H implements Z {
  }

  public static class I {
    @Inject Z z;
    @Inject E e;
  }

  public static class J {
    @Inject Provider<Z> h;
  }

  public static class K implements Z {
    @Inject A a;
  }
}
