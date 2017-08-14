package hs.ddif.plugins;

import hs.ddif.test.plugin.Database;

import javax.inject.Inject;

public class BeanWithDatabase {

  @Inject
  private Database database;

  public Database getDatabase() {
    return database;
  }
}
