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
    @Override
    public I get() throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException {
      throw new IllegalStateException("Should not get called");
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
    void findShouldThrowScopeNotActiveExceptionWhenInactive() {
      assertThatThrownBy(() -> scopeResolver.find("key"))
        .isExactlyInstanceOf(ScopeNotActiveException.class);
    }

    @Test
    void putShouldThrowScopeNotActiveExceptionWhenInactive() {
      assertThatThrownBy(() -> scopeResolver.put("key", creationalContext))
        .isExactlyInstanceOf(ScopeNotActiveException.class);
    }

    @Test
    void removeShouldThrowScopeNotActiveExceptionWhenInactive() {
      assertThatThrownBy(() -> scopeResolver.remove("key"))
        .isExactlyInstanceOf(ScopeNotActiveException.class);
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
      void findShouldReturnNoResult() {
        assertThat(scopeResolver.find("key")).isNull();
      }

      @Test
      void putShouldAddNewContext() {
        scopeResolver.put("key", creationalContext);

        assertThat(scopeResolver.find("key")).isEqualTo(creationalContext);
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
        @BeforeEach
        void beforeEach() {
          scopeResolver.put("key", creationalContext);
        }

        @Test
        void shouldBeActive() {
          assertThat(scopeResolver.isActive()).isTrue();
        }

        @Test
        void findShouldGetCachedInstance() {
          CreationalContext<?> cachedContext = scopeResolver.find("key");

          assertThat(cachedContext).isEqualTo(creationalContext);
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
