import hs.ddif.plugins.Module;
import hs.ddif.test.beans.TextDatabase;
import hs.ddif.test.beans.TextUppercaser;
import hs.ddif.test.textprovider.FancyTextProvider;
import hs.ddif.test.textprovider.UppercaseTextProvider;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PluginModule implements Module {

  @Override
  public List<Type> getTypes() {
    return new ArrayList<>() {{
      add(TextUppercaser.class);
      add(FancyTextProvider.class);
      add(UppercaseTextProvider.class);
      add(TextDatabase.class);
    }};
  }

}
