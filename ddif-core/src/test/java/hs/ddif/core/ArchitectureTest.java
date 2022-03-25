package hs.ddif.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.DependencyRules;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = ArchitectureTest.BASE_PACKAGE_NAME, importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {
  static final String BASE_PACKAGE_NAME = "hs.ddif.core";

  @ArchTest
  private final ArchRule packagesShouldBeFreeOfCycles = slices().matching(BASE_PACKAGE_NAME + ".(**)").should().beFreeOfCycles();

  @ArchTest
  private final ArchRule noClassesShouldDependOnUpperPackages = DependencyRules.NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;
}
