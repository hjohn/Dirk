package hs.ddif.plugins;

import hs.ddif.core.ProvidedInjectable;
import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.Key;
import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.store.Injectable;
import hs.ddif.core.store.InjectableStore;

import java.util.List;
import java.util.Set;

public class DependencySorter {

  public static List<Class<?>> getInTopologicalOrder(InjectableStore<Injectable> store, Set<ClassInjectable> classInjectables) {
    DirectedGraph<Class<?>> dg = new DirectedGraph<>();

    for(ClassInjectable injectable : classInjectables) {
      dg.addNode(injectable.getInjectableClass());
    }

    for(ClassInjectable classInjectable : classInjectables) {
      for(Binding[] bindings : classInjectable.getBindings().values()) {
        for(Binding binding : bindings) {
          Key requiredKey = binding.getRequiredKey();

          if(requiredKey != null) {
            for(Injectable injectable : store.resolve(requiredKey.getType(), (Object[])requiredKey.getQualifiersAsArray())) {
              Class<?> requiredClass = injectable.getInjectableClass();

              if(injectable instanceof ProvidedInjectable) {
                ProvidedInjectable providedInjectable = (ProvidedInjectable)injectable;

                requiredClass = providedInjectable.getClassImplementingProvider();
              }

              dg.addEdge(requiredClass, classInjectable.getInjectableClass());
            }
          }
        }
      }
    }

    return TopologicalSort.sort(dg);
  }
}
