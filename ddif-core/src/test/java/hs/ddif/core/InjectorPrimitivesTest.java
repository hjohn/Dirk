package hs.ddif.core;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InjectorPrimitivesTest {
  private Injector injector = Injectors.manual();

  @Test
  void injectionsShouldWorkWithPrimitives() {
    injector.registerInstance(7);
    injector.registerInstance(2.5);
    injector.registerInstance(true);
    injector.registerInstance((byte)11);

    injector.register(A.class);
    injector.register(B.class);
    injector.register(C.class);

    assertThat(injector.getInstance(A.class).calculate()).isEqualTo(23.0);
    assertThat(injector.getInstance(B.class).calculate()).isEqualTo(23.0);
    assertThat(injector.getInstance(C.class).calculate()).isEqualTo(23.0);
  }

  static class A {
    @Inject int i;
    @Inject double d;

    private byte b;
    private boolean flag;

    @Inject
    A(byte b, boolean flag) {
      this.b = b;
      this.flag = flag;
    }

    double calculate() {
      return d * i + b / (flag ? 2.0 : 1.5);
    }
  }

  static class B {
    @Inject Integer i;
    @Inject Double d;

    private Byte b;
    private Boolean flag;

    @Inject
    B(Byte b, Boolean flag) {
      this.b = b;
      this.flag = flag;
    }

    double calculate() {
      return d * i + b / (flag ? 2.0 : 1.5);
    }
  }

  static class C {
    @Inject Provider<Integer> i;
    @Inject Provider<Double> d;

    private Provider<Byte> b;
    private Provider<Boolean> flag;

    @Inject
    C(Provider<Byte> b, Provider<Boolean> flag) {
      this.b = b;
      this.flag = flag;
    }

    double calculate() {
      return d.get() * i.get() + b.get() / (flag.get() ? 2.0 : 1.5);
    }
  }
}
