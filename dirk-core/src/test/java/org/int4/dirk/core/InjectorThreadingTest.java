package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.int4.dirk.api.Injector;
import org.int4.dirk.core.test.scope.TestScope;
import org.int4.dirk.spi.scope.AbstractScopeResolver;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class InjectorThreadingTest {
  private static final int SUM_LENGTH = 100000;

  private ThreadLocal<String> currentScope = new ThreadLocal<>();
  private AbstractScopeResolver<String> scopeResolver = new AbstractScopeResolver<>() {

    @Override
    public Annotation getAnnotation() {
      return Annotations.of(TestScope.class);
    }

    @Override
    protected String getCurrentScope() {
      return currentScope.get();
    }
  };

  private Injector injector = Injectors.manual(scopeResolver);

  @BeforeEach
  void beforeEach() {
    injector.register(List.of(Root.class, A.class, B.class, C.class));
  }

  @Test
  void shouldSurviveThreadStressTest() throws InterruptedException, ExecutionException {
    ExecutorService executor = Executors.newFixedThreadPool(20);
    List<Future<Long>> futures = new ArrayList<>();

    Root root = injector.getInstance(Root.class);

    for(int i = 0; i < 1000; i++) {
      long req = i;

      futures.add(executor.submit(() -> {
        currentScope.set("" + req);

        long sum = root.handleRequest(req);

        return sum - SUM_LENGTH * req;
      }));
    }

    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    for(Future<?> future : futures) {
      assertThat(future.get()).isEqualTo(0L);
    }
  }

  @Test
  void shouldSurviveThreadStressTest2() throws InterruptedException, ExecutionException {
    ExecutorService executor = Executors.newFixedThreadPool(20);
    List<Future<Long>> futures = new ArrayList<>();

    for(int i = 0; i < 1000; i++) {
      Provider<A> root = injector.getInstance(Types.parameterize(Provider.class, A.class));
      long req = i;

      futures.add(executor.submit(() -> {
        currentScope.set("" + req);

        long sum = root.get().sum(req);

        return sum - SUM_LENGTH * req;
      }));
    }

    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    for(Future<?> future : futures) {
      assertThat(future.get()).isEqualTo(0L);
    }
  }

  public static class Root {
    @Inject Provider<A> aProvider;

    public long handleRequest(long request) {
      A a = aProvider.get();

      return a.sum(request);
    }
  }

  @TestScope
  public static class A {
    @Inject B b;
    C c;

    @Inject
    public A(C c) {
      this.c = c;
    }

    public long sum(long value) {
      c.fillArray(value);

      return b.doSum(c);
    }
  }

  public static class B {
    long doSum(C c) {
      long sum = 0;

      for(int i = 0; i < c.data.length; i++) {
        sum += c.data[i];
      }

      return sum;
    }
  }

  public static class C {
    long[] data = new long[SUM_LENGTH];

    public C() {
      try {
        Thread.sleep(10);
      }
      catch(InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    void fillArray(long value) {
      for(int i = 0; i < data.length; i++) {
        if(data[i] != 0) {
          throw new IllegalArgumentException("Oops, data for " + value + " was already filled");
        }
        data[i] = value;
      }
    }
  }
}
