package org.int4.dirk.core.store;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.int4.dirk.annotations.Opt;
import org.int4.dirk.api.Injector;
import org.int4.dirk.api.definition.CyclicDependencyException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.core.Injectors;
import org.int4.dirk.core.util.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class InjectorStoreConsistencyPolicyStressTest {
  private final Random rnd = new Random(4);

  @Test
  void shouldSurviveStressTest() throws CreationException {
    Injector injector = Injectors.manual();

    List<Type> actual = new ArrayList<>();
    List<Class<?>> classes = new ArrayList<>(List.of(A.class, B.class, C.class, D.class, E.class, F.class, G.class, H.class, I.class, J.class, K.class));
    Set<Type> classesNeverRegistered = new HashSet<>(classes);
    int total = 20000;
    int cyclics = 0;
    int successes = 0;

    for(int i = 0; i < total; i++) {
      try {
        double x = rnd.nextDouble();

        if(x < 0.02) {
          Collections.shuffle(classes, rnd);
          int count = rnd.nextInt(classes.size() - 1) + 1;
          List<Type> list = new ArrayList<>(classes.subList(0, count));

          if(x < 0.01) {  // Adds random classes, registered or not
            injector.getCandidateRegistry().register(list);
            actual.addAll(list);
            classesNeverRegistered.removeAll(list);
          }
          else {  // Removes random classes, registered or not
            injector.getCandidateRegistry().remove(list);
            actual.removeAll(list);
          }
        }
        else if(x < 0.03) {  // Removes everything
          injector.getCandidateRegistry().remove(new ArrayList<>(actual));
          actual.clear();
        }
        else if((x < 0.55 || actual.size() == 0) && actual.size() < classes.size()) {  // Only adds classes known to not be registered
          List<Type> toAdd = new ArrayList<>(classes);

          toAdd.removeAll(actual);

          Collections.shuffle(toAdd, rnd);

          int amount = rnd.nextInt((toAdd.size() - 1) / 2) + 1;

          toAdd = toAdd.subList(0, amount);
          injector.getCandidateRegistry().register(toAdd);
          actual.addAll(toAdd);
          classesNeverRegistered.removeAll(toAdd);
        }
        else {  // Only removes classes known to be registered
          List<Type> toRemove = new ArrayList<>(actual);

          Collections.shuffle(toRemove, rnd);

          int amount = toRemove.size() > 2 ? rnd.nextInt((toRemove.size() - 1) / 2) + 1 : 1;

          toRemove = toRemove.subList(0, amount);
          injector.getCandidateRegistry().remove(toRemove);
          actual.removeAll(toRemove);
        }

        successes++;
      }
      catch(IllegalStateException | IllegalArgumentException | AssertionError e) {
        throw e;
      }
      catch(CyclicDependencyException e) {
        cyclics++;
      }
      catch(Exception e) {
        // ignore
      }

      for(Class<?> cls : classes) {
        boolean contains = injector.contains(cls);
        if(contains) {
          contains = injector.getInstances(cls).stream().map(Object::getClass).anyMatch(cls::equals);
        }

        if(actual.contains(cls)) {
          assertTrue(contains, "Expected " + cls);
        }
        else {
          assertFalse(contains, "Did not expect " + cls);
        }
      }
    }

    assertTrue(successes > total / 5, "Expected 20%+ succesful calls");
    assertTrue(cyclics > 10, "Expected 10+ cyclic exceptions (attempts to register D)");
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
    @Inject @Opt Provider<D> d;  // not a circular dependency, and not required
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
