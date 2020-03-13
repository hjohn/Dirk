package hs.ddif.core;

import hs.ddif.core.inject.consistency.CyclicDependencyException;
import hs.ddif.core.inject.instantiator.BeanResolutionException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InjectorStressTest {
  static List<Type> actual = new ArrayList<>();

  Random rnd = new Random(4);

  @Test
  void shouldSurviveStressTest() throws BeanResolutionException {
    Injector injector = new Injector();
    actual.clear();

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
          injector.getStore().register(list);
          actual.addAll(list);
          classesNeverRegistered.removeAll(list);
        }
        else if(x < 0.02) {
          injector.getStore().remove(list);
          actual.removeAll(list);
        }
        else if(x < 0.03) {
          injector.getStore().remove(new ArrayList<>(actual));
          actual.clear();
        }
        else if((x < 0.55 || actual.size() == 0) && actual.size() < classes.size()) {
          List<Type> toAdd = new ArrayList<>(classes);

          toAdd.removeAll(actual);

          Collections.shuffle(toAdd, rnd);

          int amount = rnd.nextInt((toAdd.size() - 1) / 2) + 1;

          toAdd = toAdd.subList(0, amount);
          injector.getStore().register(toAdd);
          actual.addAll(toAdd);
          classesNeverRegistered.removeAll(toAdd);
        }
        else {
          List<Type> toRemove = new ArrayList<>(actual);

          Collections.shuffle(toRemove, rnd);

          int amount = rnd.nextInt((toRemove.size() - 1) / 2) + 1;

          toRemove = toRemove.subList(0, amount);
          injector.getStore().remove(toRemove);
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
