package org.int4.dirk.spi;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.DependencyRules;

import java.lang.reflect.Modifier;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = ArchitectureTest.BASE_PACKAGE_NAME)
public class ArchitectureTest {
  static final String BASE_PACKAGE_NAME = "org.int4.dirk.spi";

  @ArchTest
  private final ArchRule packagesShouldBeFreeOfCycles = slices().matching("(**)").should().beFreeOfCycles();

  @ArchTest
  public static final ArchRule noClassesShouldPubliclyImplementInterfaceInSamePackage = noClasses().should(publiclyImplementInterfacesInSamePackage());

  @ArchTest
  private final ArchRule noClassesShouldDependOnUpperPackages = DependencyRules.NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;

  public static ArchCondition<JavaClass> publiclyImplementInterfacesInSamePackage() {
    return new ArchCondition<>("publicly implement interfaces that reside in same package") {
      @Override
      public void check(JavaClass cls, ConditionEvents events) {
        for(JavaClass iface : cls.getAllRawInterfaces()) {
          boolean isSamePackageAndPublic = iface.getPackage().equals(cls.getPackage())
            && Modifier.isPublic(cls.reflect().getModifiers())
            && !Modifier.isAbstract(cls.reflect().getModifiers());

          events.add(new SimpleConditionEvent(iface, isSamePackageAndPublic, cls.getDescription() + " is public and not abstract and implements <" + iface.getFullName() + "> in same package"));
        }
      }
    };
  }
}
