package jnr.ffi;

import java.util.List;

public final class JNRAccessor {

    private JNRAccessor() {
        throw new AssertionError("No io.github.multiffi.JNRAccessor instances for you!");
    }

    public static final List<String> DEFAULT_SEARCH_PATHS = LibraryLoader.DefaultLibPaths.PATHS;

}
