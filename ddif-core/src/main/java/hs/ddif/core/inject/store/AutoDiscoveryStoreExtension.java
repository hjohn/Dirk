package hs.ddif.core.inject.store;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.Key;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class AutoDiscoveryStoreExtension implements ResolvableInjectableStore.Extension {

  @Override
  public List<Supplier<ResolvableInjectable>> getDerived(Resolver<ResolvableInjectable> resolver, ResolvableInjectable injectable) {
    List<Supplier<ResolvableInjectable>> suppliers = new ArrayList<>();

    for(Key key : gatherKeys(injectable)) {
      suppliers.add(() -> {
        if(!isResolvable(resolver, key)) {
          return attemptCreateInjectable(key);
        }

        return null;
      });
    }

    return suppliers;
  }

  private static List<Key> gatherKeys(ResolvableInjectable injectable) {
    return injectable.getBindings().stream()
      .map(Binding::getRequiredKey)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  /*
   * Attempts to create a {@link ClassInjectable} for a given {@link Key}. This will
   * fail if the type is abstract or if the key has any qualifiers as a class injectable
   * cannot have qualifiers.
   */
  private static ResolvableInjectable attemptCreateInjectable(Key requiredKey) {
    Type type = requiredKey.getType();
    Object[] qualifiersAsArray = requiredKey.getQualifiersAsArray();

    if(qualifiersAsArray.length == 0) {
      return new ClassInjectable(type);
    }

    throw new BindingException("Auto discovered class cannot be required to have qualifiers: " + requiredKey);
  }

  private static boolean isResolvable(Resolver<ResolvableInjectable> resolver, Key requiredKey) {
    Object[] qualifiersAsArray = requiredKey.getQualifiersAsArray();

    return !resolver.resolve(requiredKey.getType(), qualifiersAsArray).isEmpty();
  }
}