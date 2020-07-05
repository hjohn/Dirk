package hs.ddif.test.beans;

import hs.ddif.annotations.PluginScoped;
import hs.ddif.test.plugin.Database;

@PluginScoped
public class TextDatabase implements Database {

  @Override
  public String getType() {
    return "textdb";
  }

}
