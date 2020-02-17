package hs.ddif.plugins.test.project;

import java.sql.Connection;

import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class TestDatabaseSetup implements Provider<Connection> {
  @Override
  public Connection get() {
    return null;
  }
}
