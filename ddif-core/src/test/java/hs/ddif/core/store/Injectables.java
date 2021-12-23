package hs.ddif.core.store;

import java.lang.annotation.Annotation;
import java.util.Collections;
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
      public String toString() {
        return "Injectable(String.class)";
      }
    };
  }
}
