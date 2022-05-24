package org.int4.dirk.library;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.spi.discovery.TypeRegistrationExtension.Registry;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.inject.Provider;

public class ProviderTypeRegistrationExtensionTest {
  private Registry registry = mock(Registry.class);
  private ProviderTypeRegistrationExtension extension;

  @BeforeEach
  void beforeEach() throws NoSuchMethodException, SecurityException {
    extension = new ProviderTypeRegistrationExtension(Provider.class.getMethod("get"));
  }

  @Test
  void shouldFindProvider() throws DefinitionException, NoSuchMethodException, SecurityException {
    extension.deriveTypes(registry, A.class);

    verify(registry).add(A.class.getDeclaredMethod("get"), A.class);
  }

  @Test
  void shouldNotFindProviderWhenNotImplemented() throws Exception {
    extension.deriveTypes(registry, Bad_A.class);

    verifyNoInteractions(registry);
  }

  @Test
  void shouldNotFindProviderWhenProviderInterfaceExtended() throws Exception {
    extension.deriveTypes(registry, Bad_B.class);

    verifyNoInteractions(registry);
  }

  @Test
  void shouldNotFindProviderForWildcardType() throws Exception {
    extension.deriveTypes(registry, Types.wildcardExtends(String.class));

    verifyNoInteractions(registry);
  }

  public static class A implements Provider<B> {
    @Override
    public B get() {
      return new B();
    }
  }

  public static class B {
  }

  public static class Bad_A {  // doesn't implement Provider
  }

  public static interface Bad_B<T> extends Provider<T> {  // it is an interface
  }
}
