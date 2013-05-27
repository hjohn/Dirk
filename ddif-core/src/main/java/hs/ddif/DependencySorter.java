package hs.ddif;

import java.util.List;

public class DependencySorter {

  public static List<Class<?>> getInTopologicalOrder(InjectableStore store) {
    DirectedGraph<Class<?>> dg = new DirectedGraph<>();

    for(Class<?> cls : store.getInjectables()) {
      dg.addNode(cls);
    }

    for(Class<?> cls : store.getInjectables()) {
      for(Binding binding : store.getInjections(cls).values()) {
        Key[] requiredKeys = binding.getRequiredKeys();

        for(Key requiredKey : requiredKeys) {
          for(Injectable injectable : store.resolve(requiredKey)) {
            dg.addEdge(injectable.getInjectableClass(), cls);
          }
        }
      }
    }

    return TopologicalSort.sort(dg);
  }
}
