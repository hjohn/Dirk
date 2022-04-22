package hs.ddif.jsr330;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Provider;

public interface Instance<T> extends Provider<T>, Iterable<T> {

  Instance<T> select(Annotation... qualifiers);
  <U extends T> Instance<U> select(Type subtype, Annotation... qualifiers);
}
