package com.sun.jna;

public final class JNAAccessor {

    public interface NativeAccessor {
        int invokeInt(Function function, long address, int callFlags, Object... args);
        long invokeLong(Function function, long address, int callFlags, Object... args);
        float invokeFloat(Function function, long address, int callFlags, Object... args);
        double invokeDouble(Function function, long address, int callFlags, Object... args);
        void invokeVoid(Function function, long address, int callFlags, Object... args);
        long invokePointer(Function function, long address, int callFlags, Object... args);
        Structure invokeStructure(Structure structure, Function function, long address, int callFlags, Object... args);
    }

    public interface NativeLibraryAccessor {
        long getSymbolAddress(NativeLibrary library, String symbolName);
    }

    private JNAAccessor() {
        throw new AssertionError("No io.github.multiffi.ffi.JNAAccessor instances for you!");
    }

    private static NativeAccessor nativeAccessor;
    private static NativeLibraryAccessor nativeLibraryAccessor;

    public static void setNativeAccessor(NativeAccessor nativeAccessor) {
        JNAAccessor.nativeAccessor = nativeAccessor;
    }

    public static void setNativeLibraryAccessor(NativeLibraryAccessor nativeLibraryAccessor) {
        JNAAccessor.nativeLibraryAccessor = nativeLibraryAccessor;
    }

    public static NativeAccessor getNativeAccessor() {
        return nativeAccessor == null ? DEFAULT_NATIVE_ACCESSOR : nativeAccessor;
    }

    public static NativeLibraryAccessor getNativeLibraryAccessor() {
        return nativeLibraryAccessor == null ? DEFAULT_NATIVE_LIBRARY_ACCESSOR : nativeLibraryAccessor;
    }

    private static final NativeAccessor DEFAULT_NATIVE_ACCESSOR = new NativeAccessor() {
        @Override
        public int invokeInt(Function function, long address, int callFlags, Object... args) {
            return Native.invokeInt(function, address, callFlags, args);
        }
        @Override
        public long invokeLong(Function function, long address, int callFlags, Object... args) {
            return Native.invokeLong(function, address, callFlags, args);
        }
        @Override
        public float invokeFloat(Function function, long address, int callFlags, Object... args) {
            return Native.invokeFloat(function, address, callFlags, args);
        }
        @Override
        public double invokeDouble(Function function, long address, int callFlags, Object... args) {
            return Native.invokeDouble(function, address, callFlags, args);
        }
        @Override
        public void invokeVoid(Function function, long address, int callFlags, Object... args) {
            Native.invokeVoid(function, address, callFlags, args);
        }
        @Override
        public long invokePointer(Function function, long address, int callFlags, Object... args) {
            return Native.invokePointer(function, address, callFlags, args);
        }
        @Override
        public Structure invokeStructure(Structure structure, Function function, long address, int callFlags, Object... args) {
            return Native.invokeStructure(function, address, callFlags, args, structure);
        }
    };
    private static final NativeLibraryAccessor DEFAULT_NATIVE_LIBRARY_ACCESSOR = NativeLibrary::getSymbolAddress;
    static {
        setNativeAccessor(DEFAULT_NATIVE_ACCESSOR);
        setNativeLibraryAccessor(DEFAULT_NATIVE_LIBRARY_ACCESSOR);
    }

}
