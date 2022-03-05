package hs.ddif.core;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LifeCycleTest {
  private static final List<Class<?>> POST_CONSTRUCTS = new ArrayList<>();
  private static final List<Class<?>> PRE_DESTROYS = new ArrayList<>();

  private final Injector injector = Injectors.manual();

  @Test
  void shouldManageLifeCycleForSingleton() {
    injector.register(S.class);

    assertPostConstructs();
    assertPreDestroys();

    injector.getInstance(S.class);

    assertPostConstructs(S.class);
    assertPreDestroys();

    injector.getInstance(S.class);

    assertPostConstructs();
    assertPreDestroys();

    injector.remove(S.class);

    assertPostConstructs();
    assertPreDestroys(S.class);
  }

  @Test
  void shouldManageLifeCycleForSingletonAndItsDependents() {
    injector.register(G.class);
    injector.register(F.class);
    injector.register(E.class);
    injector.register(D.class);
    injector.register(DS.class);

    assertPostConstructs();
    assertPreDestroys();

    injector.getInstance(DS.class);

    assertPostConstructs(G.class, F.class, E.class, D.class, DS.class);
    assertPreDestroys();

    injector.getInstance(DS.class);

    assertPostConstructs();
    assertPreDestroys();

    injector.remove(DS.class);

    assertPostConstructs();
    assertPreDestroys(DS.class, D.class, E.class);  // F is a singleton depending on G, it should not be cleaned up as part of singleton DS

    injector.remove(List.of(D.class, E.class));

    assertPostConstructs();
    assertPreDestroys();

    injector.remove(F.class);

    assertPostConstructs();
    assertPreDestroys(F.class, G.class);
  }

  @Test
  void shouldIgnoreExceptionsInDestroyMethods() {
    injector.register(Z.class);
    injector.register(Y.class);
    injector.register(X.class);

    assertPostConstructs();
    assertPreDestroys();

    injector.getInstance(X.class);

    assertPostConstructs(Z.class, Y.class, X.class);
    assertPreDestroys();

    injector.remove(X.class);

    assertPostConstructs();
    assertPreDestroys(X.class, Y.class, Z.class);
  }

  private static void assertPostConstructs(Class<?>... classes) {
    assertThat(POST_CONSTRUCTS).containsExactly(classes);

    POST_CONSTRUCTS.clear();
  }

  private static void assertPreDestroys(Class<?>... classes) {
    assertThat(PRE_DESTROYS).containsExactly(classes);

    PRE_DESTROYS.clear();
  }

  public static abstract class AbstractLifeCycleLogger {
    @PostConstruct
    void postConstruct() {
      POST_CONSTRUCTS.add(getClass());
    }

    @PreDestroy
    void preDestroy() {
      PRE_DESTROYS.add(getClass());
    }
  }

  @Singleton
  public static class S extends AbstractLifeCycleLogger {
  }

  @Singleton
  public static class DS extends AbstractLifeCycleLogger {
    @Inject D d;
  }

  public static class D extends AbstractLifeCycleLogger {
    @Inject E e;
  }

  public static class E extends AbstractLifeCycleLogger {
    @Inject F f;
  }

  @Singleton
  public static class F extends AbstractLifeCycleLogger {
    @Inject G g;
  }

  public static class G extends AbstractLifeCycleLogger {
  }

  @Singleton
  public static class X extends AbstractLifeCycleLogger {
    @Inject Y y;

    @PreDestroy
    void badPreDestroy() {
      throw new RuntimeException("oops X");
    }
  }

  public static class Y extends AbstractLifeCycleLogger {
    @Inject Z z;

    @PreDestroy
    void badPreDestroy() {
      throw new RuntimeException("oops Y");
    }
  }

  public static class Z extends AbstractLifeCycleLogger {
    @PreDestroy
    void badPreDestroy() {
      throw new RuntimeException("oops Z");
    }
  }
}
