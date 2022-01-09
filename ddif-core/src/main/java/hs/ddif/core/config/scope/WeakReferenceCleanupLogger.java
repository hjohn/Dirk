package hs.ddif.core.config.scope;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.logging.Logger;

class WeakReferenceCleanupLogger {
  private static final Logger LOGGER = Logger.getLogger(WeakReferenceCleanupLogger.class.getName());

  private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
  private final Thread thread;

  WeakReferenceCleanupLogger() {
    thread = new Thread(() -> {
      for(;;) {
        try {
          Reference<? extends Object> ref = referenceQueue.remove();

          LOGGER.info("Weak Singleton was garbage collected as it was no longer referenced: " + ref);
        }
        catch(InterruptedException e) {
          // ignore
        }
      }
    });

    thread.setName(getClass().getName());
    thread.setDaemon(true);
    thread.start();
  }

  ReferenceQueue<Object> getReferenceQueue() {
    return referenceQueue;
  }
}
