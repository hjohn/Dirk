import java.lang.reflect.Type;
import java.util.List;

import org.int4.dirk.plugins.Module;
import org.int4.dirk.test.beans.TextDatabase;
import org.int4.dirk.test.beans.TextUppercaser;
import org.int4.dirk.test.textprovider.FancyTextProvider;
import org.int4.dirk.test.textprovider.UppercaseTextProvider;

public class PluginModule implements Module {

  @Override
  public List<Type> getTypes() {
    return List.of(
      TextUppercaser.class,
      FancyTextProvider.class,
      UppercaseTextProvider.class,
      TextDatabase.class
    );
  }

}
