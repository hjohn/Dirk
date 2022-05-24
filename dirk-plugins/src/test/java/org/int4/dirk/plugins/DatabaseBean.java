package org.int4.dirk.plugins;

import org.int4.dirk.test.plugin.Database;

public class DatabaseBean implements Database {

  @Override
  public String getType() {
    return "dbbean";
  }

}
