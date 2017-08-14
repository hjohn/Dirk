package hs.ddif.plugins;

import hs.ddif.core.Binding;
import hs.ddif.core.ClassInjectable;
import hs.ddif.core.Key;
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
              dg.addEdge(injectable.getInjectableClass(), classInjectable.getInjectableClass());
            }
          }
        }
      }
    }

    return TopologicalSort.sort(dg);
  }
}
