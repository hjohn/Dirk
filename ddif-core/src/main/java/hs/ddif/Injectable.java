package hs.ddif;

import java.lang.reflect.AccessibleObject;
import java.util.Map;

import javax.inject.Provider;

public interface Injectable {
  Class<?> getInjectableClass();
  Object getInstance(Injector injector, Map<AccessibleObject, Binding> injections);
  Provider<?> getProvider();
}
