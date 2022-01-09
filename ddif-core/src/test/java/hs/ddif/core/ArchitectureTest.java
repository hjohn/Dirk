package hs.ddif.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.DependencyRules;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "hs.ddif.core", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {
  @ArchTest
  public static final ArchRule packagesShouldBeFreeOfCycles = slices().matching("hs.ddif.(**)").should().beFreeOfCycles();

  @ArchTest
  public static final ArchRule noClassesShouldDependOnUpperPackages = DependencyRules.NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;
}
