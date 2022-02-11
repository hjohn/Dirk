package hs.ddif.plugins;

import hs.ddif.annotations.PluginScoped;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.InjectableFactories;
import hs.ddif.core.scope.OutOfScopeException;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PluginScopeResolverTest {
  private static final ClassInjectableFactory classInjectableFactory = InjectableFactories.forClass();
  private static final Injectable INJECTABLE = classInjectableFactory.create(String.class);

  private final Plugin plugin = new Plugin("name", List.of(String.class), getClass().getClassLoader());
  private final Plugin plugin2 = new Plugin("name2", List.of(String.class), getClass().getClassLoader());
  private final PluginScopeResolver resolver = new PluginScopeResolver();

  @Test
  void shouldHavePluginScopedAsScopeAnnotationClass() {
    assertEquals(PluginScoped.class, resolver.getScopeAnnotationClass());
  }

  @Test
  void shouldThrowOutOfScopeExceptionWhenNoScopeActive() {
    assertThrows(OutOfScopeException.class, () -> resolver.get(INJECTABLE, () -> "Hello"));
  }

  @Test
  void shouldRejectUnregisteringTypesThatWereNotRegisteredBefore() {
    assertThrows(IllegalStateException.class, () -> resolver.unregister(plugin));
  }

  @Nested
  class WhenAPluginIsRegistered {

    {
      resolver.register(plugin);
    }

    @Test
    void shouldRejectRegisteringSameTypes() {
      assertThrows(IllegalStateException.class, () -> resolver.register(plugin));
    }

    @Test
    void shouldRejectUnregisteringSameTypesInDifferentPlugin() {
      assertThrows(IllegalStateException.class, () -> resolver.unregister(plugin2));
    }

    @Test
    void shouldUnregisterPlugin() {
      resolver.unregister(plugin);
    }

    @Nested
    class AndAnInstanceIsGet {

      @Test
      void shouldReturnInstance() throws Exception {
        assertEquals("Hello", resolver.get(INJECTABLE, () -> "Hello"));
      }
    }
  }
}
