package hs.ddif.core;

import hs.ddif.core.store.Injectables;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnresolvedDependencyExceptionTest {

  @Test
  public void constructorShouldAcceptValidParameters() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
    UnresolvedDependencyException e = new UnresolvedDependencyException(Injectables.create(), String.class.getConstructor(char[].class, int.class, int.class), new Key(String.class));

    assertEquals("[class java.lang.String] required for: java.lang.String#<init>(class [C, int, int)", e.getMessage());

    e = new UnresolvedDependencyException(Injectables.create(), String.class.getDeclaredField("hash"), new Key(String.class));

    assertEquals("[class java.lang.String] required for: field \"hash\" in [java.lang.String]", e.getMessage());

    e = new UnresolvedDependencyException(Injectables.create(), String.class.getDeclaredMethod("startsWith", String.class), new Key(String.class));

    assertEquals("[class java.lang.String] required for: java.lang.String->public boolean java.lang.String.startsWith(java.lang.String)", e.getMessage());
  }
}

