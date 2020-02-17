package hs.ddif.plugins.test.project;

import java.sql.Connection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class TestDatabase {
  @SuppressWarnings("unused")
  @Inject private Provider<Connection> connectionProvider;
}
