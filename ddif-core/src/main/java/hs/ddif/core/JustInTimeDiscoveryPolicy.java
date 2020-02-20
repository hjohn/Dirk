package hs.ddif.core;

import hs.ddif.core.store.DiscoveryPolicy;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.TypeUtils;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * This discovery policy will automatically attempt to add classes to the injectable
 * store when a dependency is encountered that is not known yet.<p>
 *
 * Note that the discovery can occur when a dependency is first required (for example
 * when a call to {@link Injector#getInstance(Class, java.lang.annotation.Annotation...)}
 * is made with a class that was not registered yet with the {@link Injector}).  This means
 * that dependencies that cannot be resolved will result in an exception thrown at
 * a later stage when it's actually first needed; this contrasts with the alternative
 * where dependencies are registered explicitly with an {@link Injector}.  Explicitely
 * registering will immediately detect any unresolvable dependencies of an injectable,
 * which allows for early detection of problems.
 *
 * TODO just in time discovery can leave the store in a modified state when several dependencies are correctly discovered, but a failure occurs at a later stage resulting in the top level Injectable to remain unresolved.
 */
public class JustInTimeDiscoveryPolicy implements DiscoveryPolicy<ScopedInjectable> {

  @Override
  public void discoverType(InjectableStore<ScopedInjectable> injectableStore, Type type) {
    Class<?> typeClass = TypeUtils.determineClassFromType(type);

    if(!Modifier.isAbstract(typeClass.getModifiers())) {
      injectableStore.put(new ClassInjectable(typeClass));
    }
  }
}
