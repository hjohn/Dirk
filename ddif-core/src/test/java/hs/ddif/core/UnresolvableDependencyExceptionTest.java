package hs.ddif.core;

import hs.ddif.core.bind.Key;
import hs.ddif.core.inject.consistency.UnresolvableDependencyException;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.store.Injectables;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnresolvableDependencyExceptionTest {

  @Test
  public void constructorShouldAcceptValidParameters() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
    UnresolvableDependencyException e = new UnresolvableDependencyException(
      Injectables.create(),
      String.class.getConstructor(char[].class, int.class, int.class),
      new Key(String.class),
      Collections.<ResolvableInjectable>emptySet()
    );

    assertEquals("Missing dependency of type: [class java.lang.String] required for: java.lang.String#<init>(class [C, int, int)", e.getMessage());

    e = new UnresolvableDependencyException(
      Injectables.create(),
      String.class.getDeclaredField("hash"),
      new Key(String.class),
      new HashSet<>(Arrays.asList(Injectables.create(), Injectables.create()))
    );

    assertEquals("Multiple candidates for dependency of type: [class java.lang.String] required for: field \"hash\" in [java.lang.String]: [Injectable(String.class), Injectable(String.class)]", e.getMessage());

    e = new UnresolvableDependencyException(
      Injectables.create(),
      String.class.getDeclaredMethod("startsWith", String.class),
      new Key(String.class),
      Collections.<ResolvableInjectable>emptySet()
    );

    assertEquals("Missing dependency of type: [class java.lang.String] required for: java.lang.String->public boolean java.lang.String.startsWith(java.lang.String)", e.getMessage());
  }
}

