package hs.ddif.test.beans;

import hs.ddif.test.plugin.Database;

import javax.inject.Singleton;

@Singleton
public class TextDatabase implements Database {

  @Override
  public String getType() {
    return "textdb";
  }

}
