package hs.ddif.core.store;

import hs.ddif.api.scope.ScopeResolver;
import hs.ddif.core.config.SingletonScopeResolver;
import hs.ddif.core.definition.BadQualifiedTypeException;
import hs.ddif.core.definition.Binding;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.QualifiedType;
import hs.ddif.core.definition.injection.Injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import jakarta.inject.Singleton;

public class Injectables {
  private static final SingletonScopeResolver SINGLETON_SCOPE_RESOLVER = new SingletonScopeResolver(Singleton.class);

  public static Injectable<Object> create() throws BadQualifiedTypeException {
    QualifiedType qualifiedType = new QualifiedType(String.class);

    return new Injectable<>() {

      @Override
      public Type getType() {
        return qualifiedType.getType();
      }

      @Override
      public Set<Type> getTypes() {
        return Set.of(String.class, Object.class);
      }

      @Override
      public Set<Annotation> getQualifiers() {
        return qualifiedType.getQualifiers();
      }

      @Override
      public List<Binding> getBindings() {
        return List.of();
      }

      @Override
      public ScopeResolver getScopeResolver() {
        return SINGLETON_SCOPE_RESOLVER;
      }

      @Override
      public Object create(List<Injection> injections) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void destroy(Object instance) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String toString() {
        return "Injectable(String.class)";
      }
    };
  }
}
