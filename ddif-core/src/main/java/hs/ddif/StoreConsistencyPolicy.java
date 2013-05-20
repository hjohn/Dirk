package hs.ddif;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.Map;
import java.util.Set;

public interface StoreConsistencyPolicy {
  void checkAddition(Class<?> concreteClass, Set<Annotation> qualifiers, Map<AccessibleObject, Binding> bindings);
  void checkRemoval(Class<?> concreteClass, Set<Annotation> qualifiers);
}
