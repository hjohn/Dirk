package hs.ddif.plugins;

import java.util.ArrayList;
import java.util.List;

public class CyclicGraphException extends RuntimeException {
  private final List<Object> location = new ArrayList<>();

  public CyclicGraphException(Object root) {
    super("Cyclic Dependency present");

    location.add(root);
  }

  void addPredecessor(Object predecessor) {
    location.add(predecessor);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    for(Object o : location) {
      if(builder.length() != 0) {
        builder.append(" <- ");
      }
      builder.append(o.toString());
    }

    return super.toString() + ", path: " + builder.toString();
  }
}
