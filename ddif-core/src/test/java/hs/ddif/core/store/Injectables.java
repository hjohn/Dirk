package hs.ddif.core.store;

import hs.ddif.core.definition.BadQualifiedTypeException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.QualifiedType;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.injection.Injection;

import java.lang.annotation.Annotation;
import java.util.List;

public class Injectables {

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
