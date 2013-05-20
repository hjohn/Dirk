import hs.ddif.Module;
import hs.ddif.test.beans.TextDatabase;
import hs.ddif.test.beans.TextUppercaser;
import hs.ddif.test.textprovider.FancyTextProvider;
import hs.ddif.test.textprovider.UppercaseTextProvider;

import java.util.ArrayList;
import java.util.List;

public class PluginModule implements Module {

  public List<Class<?>> getClasses() {
    return new ArrayList<Class<?>>() {{
      add(TextUppercaser.class);
      add(FancyTextProvider.class);
      add(UppercaseTextProvider.class);
      add(TextDatabase.class);
    }};
  }

}
