package org.int4.dirk.spi.scope;

import java.lang.annotation.Annotation;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.util.Annotations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Singleton;

public class AbstractScopeResolverTest {
  private String currentScope = null;
  private AbstractScopeResolver<String> scopeResolver = new AbstractScopeResolver<>() {

    @Override
    public Annotation getAnnotation() {
      return Annotations.of(Singleton.class);
    }

    @Override
    protected String getCurrentScope() {
      return currentScope;
    }
  };

  private int releaseCalls;

  private CreationalContext<I> creationalContext = new CreationalContext<>() {
    private final I i = new I();

    @Override
    public I get() throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException {
      return i;
    }

    @Override
    public void release() {
      releaseCalls++;
    }
  };

  @Nested
  class WhenNew {
    @Test
    void shouldBeInactive() {
      assertThat(scopeResolver.isActive()).isFalse();
    }

    @Test
    void getShouldThrowScopeNotActiveExceptionWhenInactive() {
      assertThatThrownBy(() -> scopeResolver.get("key", creationalContext))
        .isExactlyInstanceOf(ScopeNotActiveException.class);
    }

    @Test
    void removeShouldIgnoreMissingEntries() {
      assertThatCode(() -> scopeResolver.remove("key")).doesNotThrowAnyException();
    }

    @Test
    void destroyScopeShouldAllowDestroyingNonExistingScope() {
      assertThatCode(() -> scopeResolver.destroyScope("A")).doesNotThrowAnyException();
    }

    @Nested
    class AndAScopeBecomesActive {
      {
        currentScope = "A";
      }

      @Test
      void shouldBeActive() {
        assertThat(scopeResolver.isActive()).isTrue();
      }

      @Test
      void getShouldCreateNewInstance() throws Exception {
        I instance = scopeResolver.get("key", creationalContext);

        assertThat(instance).isInstanceOf(I.class);
      }

      @Test
      void removeShouldIgnoreMissingEntries() {
        assertThatCode(() -> scopeResolver.remove("key")).doesNotThrowAnyException();
      }

      @Test
      void destroyScopeShouldAllowDestroyingExistingScope() {
        assertThatCode(() -> scopeResolver.destroyScope("A")).doesNotThrowAnyException();
      }

      @Nested
      class AndAnObjectWasCreated {
        private I i;

        @BeforeEach
        void beforeEach() throws Exception {
          i = scopeResolver.get("key", creationalContext);
        }

        @Test
        void shouldBeActive() {
          assertThat(scopeResolver.isActive()).isTrue();
        }

        @Test
        void getShouldGetCachedInstance() throws Exception {
          I cachedInstance = scopeResolver.get("key", creationalContext);

          assertThat(cachedInstance).isInstanceOf(I.class);
          assertThat(cachedInstance).isEqualTo(i);
        }

        @Test
        void removeShouldIngoreNonExistingKeys() {
          assertThatCode(() -> scopeResolver.remove("missing-key")).doesNotThrowAnyException();
        }

        @Test
        void removeShouldTriggerCreationContextRelease() {
          assertThat(releaseCalls).isEqualTo(0);

          scopeResolver.remove("key");

          assertThat(releaseCalls).isEqualTo(1);
        }

        @Test
        void destroyScopeShouldTriggerCreationContextRelease() {
          assertThat(releaseCalls).isEqualTo(0);

          scopeResolver.destroyScope("A");

          assertThat(releaseCalls).isEqualTo(1);
        }
      }
    }
  }

  static class I {
  }
}
