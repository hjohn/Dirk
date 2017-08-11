package hs.ddif.scan;

import hs.ddif.core.Binding;
import hs.ddif.core.ClassInjectable;
import hs.ddif.core.Injectable;
import hs.ddif.core.InjectableStore;
import hs.ddif.core.Key;

import java.util.List;
import java.util.Set;

public class DependencySorter {

  public static List<Class<?>> getInTopologicalOrder(InjectableStore store, Set<ClassInjectable> classInjectables) {
    DirectedGraph<Class<?>> dg = new DirectedGraph<>();

    for(ClassInjectable injectable : classInjectables) {
      dg.addNode(injectable.getInjectableClass());
    }

    for(ClassInjectable classInjectable : classInjectables) {
      for(Binding binding : classInjectable.getBindings().values()) {
        Key[] requiredKeys = binding.getRequiredKeys();

        for(Key requiredKey : requiredKeys) {
          for(Injectable injectable : store.resolve(requiredKey.getType(), (Object[])requiredKey.getQualifiersAsArray())) {
            dg.addEdge(injectable.getInjectableClass(), classInjectable.getInjectableClass());
          }
        }
      }
    }

    return TopologicalSort.sort(dg);
  }
}
