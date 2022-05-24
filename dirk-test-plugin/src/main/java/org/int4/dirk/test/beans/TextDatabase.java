package org.int4.dirk.test.beans;

import javax.inject.Named;

import org.int4.dirk.test.plugin.Database;

@Named
public class TextDatabase implements Database {

  @Override
  public String getType() {
    return "textdb";
  }

}
