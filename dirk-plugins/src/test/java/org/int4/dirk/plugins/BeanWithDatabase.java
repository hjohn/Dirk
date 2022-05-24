package org.int4.dirk.plugins;

import javax.inject.Inject;

import org.int4.dirk.test.plugin.Database;

public class BeanWithDatabase {

  @Inject
  private Database database;

  public Database getDatabase() {
    return database;
  }
}
