package hs.ddif;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;

import javax.inject.Inject;

/**
 * This discovery policy will automatically add eligable classes to the injectable
 * store when a dependency is encountered that is not known yet.  Eligable classes
 * are classes with an {@link Inject} annotated constructor or a default constructor.<p>
 *
 * Note that the discovery can occur when a dependency is first required (for example
 * when a call to {@link Injector#getInstance(Class, java.lang.annotation.Annotation...)}
 * is made with a class that was not registered yet with the {@link Injector}).  This means
 * that dependencies that cannot be resolved will result in an exception thrown at
 * a later stage when it's actually first needed; this constrasts with the alternative
 * where dependencies are registered explicitly with an {@link Injector}.  Explicitely
 * registering will immediately detect any unresolvable dependencies of an injectable,
 * which allows for early detection of problems.
 */
public class JustInTimeDiscoveryPolicy implements DiscoveryPolicy {

  @Override
  public void discoverType(InjectableStore injectableStore, Type type) {
    Class<?> typeClass = Binder.determineClassFromType(type);

    if(!typeClass.isInterface() && !Modifier.isAbstract(typeClass.getModifiers())) {
      try {
        injectableStore.put(new ClassInjectable(typeClass));
      }
      catch(DiscoveryException e) {
        // discovery halted, the type does not have a suitable constructor or has undiscoverable dependencies
      }
    }
  }

  @Override
  public void discoverDependencies(InjectableStore injectableStore, Injectable injectable, Map<AccessibleObject, Binding> bindings) {
    if(!injectable.canBeInstantiated(bindings)) {
      throw new DiscoveryException();
    }

    for(Map.Entry<AccessibleObject, Binding> entry : bindings.entrySet()) {
      for(Key key : entry.getValue().getRequiredKeys()) {
        if(injectableStore.resolve(key).isEmpty()) {
          throw new UnresolvedDependencyException(injectable, entry.getKey(), key);
        }
      }
    }
  }

  static class DiscoveryException extends RuntimeException {
  }
}
