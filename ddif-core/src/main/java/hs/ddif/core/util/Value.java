package hs.ddif.core.util;

import java.util.Map;

/**
 * Tuple class representing the field name and value of an annotation, used for annotation descriptors.
 */
public class Value {
  private final String key;
  private final Object value;

  private Value(String key, Object value) {
    if(key == null) {
      throw new IllegalArgumentException("key cannot be null");
    }

    this.key = key;
    this.value = value;
  }

  /**
   * Constructs a new instance.
   *
   * @param key a key, cannot be null
   * @param value a value, can be null
   */
  public Value(String key, String value) {
    this(key, (Object)value);
  }

  /**
   * Constructs a new instance.
   *
   * @param key a key, cannot be null
   * @param value a value, can be null
   */
  public Value(String key, Number value) {
    this(key, (Object)value);
  }

  /**
   * Constructs a new instance.
   *
   * @param key a key, cannot be null
   * @param value a value, can be null
   */
  public Value(String key, boolean value) {
    this(key, (Object)value);
  }

  /**
   * Constructs a new instance.
   *
   * @param key a key, cannot be null
   * @param value a value, can be null
   */
  public Value(String key, Class<?> value) {
    this(key, (Object)value);
  }

  /**
   * Constructs a new instance.
   *
   * @param key a key, cannot be null
   * @param value a value, can be null
   */
  public Value(String key, Map<?, ?>[] value) {
    this(key, (Object)value);
  }

  /**
   * Returns the key.
   *
   * @return the key, never null
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns the value.
   *
   * @return the value
   */
  public Object getValue() {
    return value;
  }
}