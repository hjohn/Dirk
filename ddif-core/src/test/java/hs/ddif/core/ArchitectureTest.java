package hs.ddif.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "hs.ddif.core", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {
  @ArchTest
  public static final ArchRule cycles = slices().matching("hs.ddif.(**)").should().beFreeOfCycles();

}
