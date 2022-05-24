package org.int4.dirk.test.beans;

import javax.inject.Singleton;

import org.int4.dirk.test.plugin.Database;

@Singleton
public class TextDatabase implements Database {

  @Override
  public String getType() {
    return "textdb";
  }

}
