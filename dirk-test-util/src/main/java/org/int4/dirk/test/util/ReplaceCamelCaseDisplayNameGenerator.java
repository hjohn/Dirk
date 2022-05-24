package org.int4.dirk.test.util;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayNameGenerator;

/**
 * A {@link DisplayNameGenerator} which converts camel case test names to normal
 * text. Test names can use underscores to quote parts of the name that should
 * be kept exactly as written.<p>
 *
 * For example: "When_getName_IsCalled" becomes "When getName is called"
 */
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
    StringBuilder sb = new StringBuilder();
    String[] split = text.split("_");

    for(int i = 0; i < split.length; i++) {
      String part = split[i];

      if(i > 0) {
        sb.append(" ");
      }

      if(i % 2 == 0) {
        sb.append(UPPERCASE_LETTER.matcher(part).replaceAll(mr -> " " + mr.group(1).toLowerCase()).trim());
      }
      else {
        sb.append(part);
      }
    }

    sb.replace(0, 1, sb.substring(0, 1).toUpperCase());

    return sb.toString();
  }
}
