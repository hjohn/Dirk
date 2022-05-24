package org.int4.dirk.plugins.test.project;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public abstract class TestConfigurationProvider<T> implements Provider<T> {
  @Inject @Named("configuration") private String config;

  @SuppressWarnings("unused")
  public TestConfigurationProvider(Class<T> configClass, String path) {
  }

  @Override
  public T get() {
    return null;
  }
}