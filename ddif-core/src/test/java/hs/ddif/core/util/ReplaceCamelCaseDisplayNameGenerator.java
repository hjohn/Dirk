package hs.ddif.core.util;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayNameGenerator;

public class ReplaceCamelCaseDisplayNameGenerator implements DisplayNameGenerator {
  private static final Pattern UPPERCASE_LETTER = Pattern.compile("([A-Z]|[0-9]+)");

  @Override
  public String generateDisplayNameForClass(Class<?> testClass) {
    return toName(testClass.getSimpleName() + "...");
  }

  @Override
  public String generateDisplayNameForNestedClass(Class<?> nestedClass) {
    return toName(nestedClass.getSimpleName() + "...");
  }

  @Override
  public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
    return toName(testMethod.getName());
  }

  private static String toName(String text) {
    return text.substring(0, 1).toUpperCase()
      + UPPERCASE_LETTER.matcher(text.substring(1)).replaceAll(mr -> " " + (mr.group(1).toLowerCase()));
  }
}