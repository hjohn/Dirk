package hs.ddif.core;

import hs.ddif.core.inject.store.ViolatesSingularDependencyException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class InjectorGenericsTest {
  private Injector injector;

  @BeforeEach
  public void beforeEach() {
    injector = Injectors.manual();
  }

  @Test
  public void shouldInjectInstancesWithMatchingGenerics() {
    injector.register(StringToIntConverter.class);
    injector.register(IntToStringConverter.class);
    injector.register(StringToStringListConverter.class);
    injector.register(InjectableWithConverters.class);

    InjectableWithConverters instance = injector.getInstance(InjectableWithConverters.class);

    assertEquals(12345, (int)instance.convertToInt("12345"));
    assertEquals("12", instance.convertToString(12));
    assertEquals(new ArrayList<String>() {{
      add("a");
      add("b");
      add("c");
    }}, instance.convertToStringList("a,b,c"));
  }

  @Test
  public void shouldInjectProviderInstancesWithMatchingGenerics() {
    injector.register(StringToIntConverter.class);
    injector.register(IntToStringConverter.class);
    injector.register(StringToStringListConverter.class);
    injector.register(InjectableWithConverterProviders.class);

    InjectableWithConverterProviders instance = injector.getInstance(InjectableWithConverterProviders.class);

    assertEquals(12345, (int)instance.convertToInt("12345"));
    assertEquals("12", instance.convertToString(12));
    assertEquals(new ArrayList<String>() {{
      add("a");
      add("b");
      add("c");
    }}, instance.convertToStringList("a,b,c"));
  }

  @Test
  public void shouldInjectListInstancesWithMatchingGenerics() {
    injector.register(StringToIntConverter.class);
    injector.register(IntToStringConverter.class);
    injector.register(StringToStringListConverter.class);
    injector.register(InjectableWithConverterLists.class);

    InjectableWithConverterLists instance = injector.getInstance(InjectableWithConverterLists.class);

    assertEquals(12345, (int)instance.convertToInt("12345"));
    assertEquals("12", instance.convertToString(12));
    assertEquals(new ArrayList<String>() {{
      add("a");
      add("b");
      add("c");
    }}, instance.convertToStringList("a,b,c"));
  }

  @Test
  public void shouldNotViolateSingularDependencies() {
    injector.register(StringToIntConverter.class);
    injector.register(IntToStringConverter.class);
    injector.register(StringToStringListConverter.class);
    injector.register(InjectableWithConverters.class);

    assertThatThrownBy(() -> injector.remove(StringToIntConverter.class))
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class);
  }

  public static class InjectableWithConverters {
    @Inject
    private Converter<String, Integer> stringToIntConverter;

    @Inject
    private Converter<Integer, String> intToStringConverter;

    @Inject
    private Converter<String, List<String>> stringToStringListConverter;

    public Integer convertToInt(String s) {
      return stringToIntConverter.convert(s);
    }

    public String convertToString(int input) {
      return intToStringConverter.convert(input);
    }

    public List<String> convertToStringList(String s) {
      return stringToStringListConverter.convert(s);
    }
  }

  public static class InjectableWithConverterProviders {
    @Inject
    private Provider<Converter<String, Integer>> stringToIntConverter;

    @Inject
    private Provider<Converter<Integer, String>> intToStringConverter;

    @Inject
    private Provider<Converter<String, List<String>>> stringToStringListConverter;

    public Integer convertToInt(String s) {
      return stringToIntConverter.get().convert(s);
    }

    public String convertToString(int input) {
      return intToStringConverter.get().convert(input);
    }

    public List<String> convertToStringList(String s) {
      return stringToStringListConverter.get().convert(s);
    }
  }

  public static class InjectableWithConverterLists {
    @Inject
    private List<Converter<String, Integer>> stringToIntConverter;

    @Inject
    private List<Converter<Integer, String>> intToStringConverter;

    @Inject
    private List<Converter<String, List<String>>> stringToStringListConverter;

    public Integer convertToInt(String s) {
      return stringToIntConverter.get(0).convert(s);
    }

    public String convertToString(int input) {
      return intToStringConverter.get(0).convert(input);
    }

    public List<String> convertToStringList(String s) {
      return stringToStringListConverter.get(0).convert(s);
    }
  }

  public static interface Converter<F, T> {
    T convert(F from);
  }

  public static class StringToIntConverter implements Converter<String, Integer> {
    @Override
    public Integer convert(String from) {
      return Integer.parseInt(from);
    }
  }

  public static class StringToStringListConverter implements Converter<String, List<String>> {
    @Override
    public List<String> convert(String from) {
      return Arrays.asList(from.split(","));
    }
  }

  public static class IntToStringConverter implements Converter<Integer, String> {
    @Override
    public String convert(Integer from) {
      return "" + from;
    }
  }

}
