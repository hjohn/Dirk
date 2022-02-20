package hs.ddif.core.store;

import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.definition.BadQualifiedTypeException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.QualifiedType;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.scope.ScopeResolver;

import java.util.List;

public class Injectables {
  private static final SingletonScopeResolver SINGLETON_SCOPE_RESOLVER = new SingletonScopeResolver();

  public static Injectable create() throws BadQualifiedTypeException {
    QualifiedType qualifiedType = new QualifiedType(String.class);

    return new Injectable() {

      @Override
      public QualifiedType getQualifiedType() {
        return qualifiedType;
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
      public Object createInstance(List<Injection> injections) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String toString() {
        return "Injectable(String.class)";
      }
    };
  }
}
