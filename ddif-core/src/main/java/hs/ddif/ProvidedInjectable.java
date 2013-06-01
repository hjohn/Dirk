package hs.ddif;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.Map;

import javax.inject.Provider;

public class ProvidedInjectable implements Injectable {
  private final Provider<?> provider;
  private final Class<?> injectableClass;

  public ProvidedInjectable(Provider<?> provider) {
    Type type = provider.getClass().getGenericInterfaces()[0];

    this.provider = provider;
    this.injectableClass = Binder.determineClassFromType(Binder.getGenericType(type));
  }

  @Override
  public boolean canBeInstantiated(Map<AccessibleObject, Binding> bindings) {
    return true;
  }

  @Override
  public Class<?> getInjectableClass() {
    return injectableClass;
  }

  @Override
  public Object getInstance(Injector injector, Map<AccessibleObject, Binding> bindings) {
    return provider.get();
  }

  @Override
  public int hashCode() {
    return injectableClass.toString().hashCode() ^ provider.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    return injectableClass.equals(((ProvidedInjectable)obj).injectableClass)
        && provider.equals(((ProvidedInjectable)obj).provider);
  }
}
