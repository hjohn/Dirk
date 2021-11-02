package hs.ddif.plugins.test.project;

import hs.ddif.annotations.Produces;

import javax.inject.Named;

public class TestVersionProducer {
  @Produces @Named("version") private static final String VERSION = "10.0.2";
}
