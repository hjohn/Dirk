package org.int4.dirk.core;

import java.lang.reflect.Type;
import java.util.List;

import org.int4.dirk.api.Injector;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

public class RegistrationPerformanceTest {
  private Injector injector = Injectors.manual();
  private List<Type> types = List.of(A.class, B.class, C.class, D.class, E.class, F.class, G.class, H.class);

  @Test
  @Disabled
  public void test() throws Exception {
    int iterations = 1000;
    int meausurements = 10;
    long[] times = new long[meausurements];

    for(int i = 0; i < 10; i++) {
      times[i] = run(iterations);
    }

    Thread.sleep(1000);

    for(int i = 0; i < 10; i++) {
      times[i] = run(iterations);
    }

    for(long time : times) {
      System.out.println((double)time / iterations + " ns/op");
    }
  }

  private long run(int iterations) throws Exception {
    long nanos = System.nanoTime();

    for(int i = 0; i < iterations; i++) {
      injector.register(types);
      injector.remove(types);
    }

    return System.nanoTime() - nanos;
  }

  public static class A {
  }

  public static class B {
    @Inject A a;
  }

  public static class C {
    @Inject A a;
  }

  public static class D {
    @Inject C c;
    @Inject A a;
  }

  public static class E {
    @Inject D d;
    @Inject B b;
  }

  public static class F {
    @Inject A a;
    @Inject D d;
    @Inject B b;
  }

  public static class G {
    @Inject A a;
    @Inject C c;
    @Inject E e;
  }

  public static class H {
    @Inject G g;
  }
}
