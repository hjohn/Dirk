package hs.ddif.test.beans;

import hs.ddif.test.plugin.Database;

import javax.inject.Named;

@Named
public class TextDatabase implements Database {

  public String getType() {
    return "textdb";
  }

}
