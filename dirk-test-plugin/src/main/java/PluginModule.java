import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.int4.dirk.plugins.Module;
import org.int4.dirk.test.beans.TextDatabase;
import org.int4.dirk.test.beans.TextUppercaser;
import org.int4.dirk.test.textprovider.FancyTextProvider;
import org.int4.dirk.test.textprovider.UppercaseTextProvider;

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
