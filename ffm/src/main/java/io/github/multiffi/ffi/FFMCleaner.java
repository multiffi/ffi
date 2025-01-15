package io.github.multiffi.ffi;

import java.lang.ref.Cleaner;

public final class FFMCleaner {

    private FFMCleaner() {
        throw new AssertionError("No io.github.multiffi.ffi.FFMCleaner instances for you!");
    }

    private static volatile Cleaner CLEANER = null;
    private static final Object CLEANER_LOCK = new Object();

    public static Cleaner.Cleanable register(Object object, Runnable cleanup) {
        if (CLEANER == null) synchronized (CLEANER_LOCK) {
            if (CLEANER == null) CLEANER = Cleaner.create(runnable -> {
                Thread thread = new Thread(runnable, "Multiffi/FFI FFM Cleaner Thread");
                thread.setDaemon(true);
                return thread;
            });
        }
        return CLEANER.register(object, cleanup);
    }

}
