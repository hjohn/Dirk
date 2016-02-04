package hs.ddif.core;

import java.util.Map;

public class Value {
  private final String key;
  private final Object value;

  public Value(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public Value(String key, Number value) {
    this.key = key;
    this.value = value;
  }

  public Value(String key, boolean value) {
    this.key = key;
    this.value = value;
  }

  public Value(String key, Class<?> value) {
    this.key = key;
    this.value = value;
  }

  public Value(String key, Map<?, ?>[] map) {
    this.key = key;
    this.value = map;
  }

  public String getKey() {
    return key;
  }

  public Object getValue() {
    return value;
  }
}