package hs.ddif.core;

import hs.ddif.api.Injector;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.definition.DependencyException;
import hs.ddif.api.util.Annotations;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.inject.Named;

public class InjectorManyNamedTest {

  @Test
  @Disabled
  void shouldPerformWell() throws DefinitionException, DependencyException {
    List<Annotation> annotations = new ArrayList<>();
    int testSize = 20;
    int rounds = 7;
    Random rnd = new Random(1);

    for(int i = 0; i < Math.pow(7, rounds) * testSize; i++) {
      annotations.add(Annotations.of(Named.class, Map.of("value", "some.identifying-key." + i)));
    }

    for(int j = 0; j < rounds; j++) {
      long time = System.nanoTime();
      Injector injector = Injectors.manual();

      for(int i = 0; i < testSize; i++) {
        injector.registerInstance("String" + i, annotations.get(i));
      }

      long insertTime = System.nanoTime() - time;

      time = System.nanoTime();

      for(int i = 0; i < testSize; i++) {
        injector.getInstance(String.class, annotations.get(rnd.nextInt(testSize)));
      }

      long lookupTime = System.nanoTime() - time;

      System.out.println(String.format("Insert %6d: %5.1f ns/element; Lookup %5.1f ns/element", testSize, ((double)insertTime / testSize), ((double)lookupTime / testSize)));

      testSize *= 7;
    }
  }
}
