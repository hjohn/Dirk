package hs.ddif.core.store;

import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.injection.Injection;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Injectables {

  public static Injectable create() {
    return new Injectable() {

      @Override
      public Class<?> getType() {
        return String.class;
      }

      @Override
      public Set<Annotation> getQualifiers() {
        return Collections.emptySet();
      }

      @Override
      public List<Binding> getBindings() {
        return List.of();
      }

      @Override
      public Annotation getScope() {
        return null;
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
