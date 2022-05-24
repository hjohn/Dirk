package org.int4.dirk.plugins;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PluginTest {

  @Test
  void constructorShouldRejectInvalidParameters() {
    assertThrows(IllegalArgumentException.class, () -> new Plugin(null, null, getClass().getClassLoader()));
    assertThrows(IllegalArgumentException.class, () -> new Plugin(null, List.of(), null));
    assertThrows(IllegalArgumentException.class, () -> new Plugin(null, Arrays.asList(new Type[] {null}), getClass().getClassLoader()));
  }

  @Test
  void constructorShouldAcceptValidParameters() {
    Plugin plugin = new Plugin("name", List.of(String.class), getClass().getClassLoader());

    assertEquals(getClass().getClassLoader(), plugin.getClassLoader());
    assertEquals(List.of(String.class), plugin.getTypes());
    assertEquals("Plugin[name -> [class java.lang.String]]", plugin.toString());
    assertEquals(false, plugin.isUnloaded());
  }
}
