package org.int4.dirk.plugins.test.project;

import javax.inject.Named;

import org.int4.dirk.annotations.Produces;

public class TestVersionProducer {
  @Produces @Named("version") private static final String VERSION = "10.0.2";
}
