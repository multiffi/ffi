package io.github.multiffi.ffi;

import com.sun.jna.JNAAccessor;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class JNASymbolLookup {

    private JNASymbolLookup() {
        throw new AssertionError("No io.github.multiffi.ffi.JNASymbolLookup instances for you!");
    }

    private static final Set<NativeLibrary> NATIVE_LIBRARIES = new HashSet<>();
    static {
        NATIVE_LIBRARIES.add(NativeLibrary.getProcess());
        if (!Platform.isLinux()) NATIVE_LIBRARIES.add(NativeLibrary.getInstance(Platform.C_LIBRARY_NAME));
        if (!Platform.isWindows() && !Platform.isLinux()) NATIVE_LIBRARIES.add(NativeLibrary.getInstance(Platform.MATH_LIBRARY_NAME));
    }
    
    public static void loadLibrary(String libraryName) throws UnsatisfiedLinkError {
        Objects.requireNonNull(libraryName);
        synchronized (NATIVE_LIBRARIES) {
            NATIVE_LIBRARIES.add(NativeLibrary.getInstance(libraryName));
        }
    }
    
    public static void loadLibrary(File libraryFile) throws UnsatisfiedLinkError {
        Objects.requireNonNull(libraryFile);
        synchronized (NATIVE_LIBRARIES) {
            NATIVE_LIBRARIES.add(NativeLibrary.getInstance(libraryFile.getAbsolutePath()));
        }
    }

    public static long getSymbolAddress(String symbolName) {
        Objects.requireNonNull(symbolName);
        synchronized (NATIVE_LIBRARIES) {
            for (NativeLibrary library : NATIVE_LIBRARIES) {
                try {
                    return JNAAccessor.getNativeLibraryAccessor().getSymbolAddress(library, symbolName);
                }
                catch (UnsatisfiedLinkError ignored) {
                }
            }
        }
        throw new UnsatisfiedLinkError(String.format("Failed to get symbol: `%s`", symbolName));
    }
    
}
