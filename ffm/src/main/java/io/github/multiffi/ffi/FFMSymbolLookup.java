package io.github.multiffi.ffi;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class FFMSymbolLookup {
    
    private FFMSymbolLookup() {
        throw new AssertionError("No io.github.multiffi.ffi.FFMSymbolLookup instances for you!");
    }

    private static final AtomicReference<SymbolLookup> LOOKUP_REFERENCE = new AtomicReference<>();
    static {
        // C runtime default lookup
        SymbolLookup lookup = FFMUtil.LINKER.defaultLookup();
        String libraryName;
        if (FFMUtil.IS_WINDOWS) libraryName = FFMUtil.IS_WINDOWS_CE ? "coredll" : "msvcrt";
        else if (FFMUtil.IS_AIX || FFMUtil.IS_IBMI)
            libraryName = FFMUtil.ADDRESS_SIZE == 4L ? "libc.a(shr.o)" : "libc.a(shr_64.o)";
        else libraryName = "c";
        // libc lookup
        if (!FFMUtil.IS_LINUX)
            lookup = lookup.or(SymbolLookup.libraryLookup(FFMUtil.mapLibraryName(libraryName), Arena.global()));
        // libm lookup
        if (!FFMUtil.IS_WINDOWS && !FFMUtil.IS_LINUX && !FFMUtil.IS_AIX && !FFMUtil.IS_IBMI)
            lookup = lookup.or(SymbolLookup.libraryLookup(FFMUtil.mapLibraryName("m"), Arena.global()));
        LOOKUP_REFERENCE.set(lookup);
    }
    
    public static void loadLibrary(String libraryName) throws UnsatisfiedLinkError {
        Objects.requireNonNull(libraryName);
        File libraryFile = new File(libraryName);
        if (libraryFile.isAbsolute()) loadLibrary(libraryFile);
        else {
            try {
                LOOKUP_REFERENCE.getAndUpdate(lookup -> lookup.or(SymbolLookup.libraryLookup(FFMUtil.mapLibraryName(libraryName), Arena.global())));
            }
            catch (IllegalArgumentException e) {
                throw new UnsatisfiedLinkError(e.getMessage());
            }
        }
    }
    
    public static void loadLibrary(File libraryFile) throws UnsatisfiedLinkError {
        Objects.requireNonNull(libraryFile);
        try {
            LOOKUP_REFERENCE.getAndUpdate(lookup -> lookup.or(SymbolLookup.libraryLookup(libraryFile.getAbsolutePath(), Arena.global())));
        }
        catch (IllegalArgumentException e) {
            throw new UnsatisfiedLinkError(e.getMessage());
        }
    }

    public static long getSymbolAddress(String symbolName) throws UnsatisfiedLinkError {
        Objects.requireNonNull(symbolName);
        Optional<MemorySegment> symbol = LOOKUP_REFERENCE.get().find(symbolName);
        if (symbol.isPresent()) return symbol.get().address();
        else throw new UnsatisfiedLinkError(String.format("Failed to get symbol: `%s`", symbolName));
    }
    
}
