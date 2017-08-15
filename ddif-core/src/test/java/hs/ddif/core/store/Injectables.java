package hs.ddif.core.store;

import hs.ddif.core.util.AnnotationDescriptor;

import java.util.Collections;
import java.util.Set;

public class Injectables {

  public static Injectable create() {
    return new Injectable() {

      @Override
      public Class<?> getInjectableClass() {
        return String.class;
      }

      @Override
      public Set<AnnotationDescriptor> getQualifiers() {
        return Collections.emptySet();
      }
    };
  }
}
