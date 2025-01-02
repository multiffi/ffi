package com.sun.jna;

public final class JNAAccessor {

    private JNAAccessor() {
        throw new AssertionError("No io.github.multiffi.JNAAccessor instances for you!");
    }

    public static int invokeInt(Function function, long address, int callFlags, Object... args) {
        return Native.invokeInt(function, address, callFlags, args);
    }

    public static long invokeLong(Function function, long address, int callFlags, Object... args) {
        return Native.invokeLong(function, address, callFlags, args);
    }

    public static float invokeFloat(Function function, long address, int callFlags, Object... args) {
        return Native.invokeFloat(function, address, callFlags, args);
    }

    public static double invokeDouble(Function function, long address, int callFlags, Object... args) {
        return Native.invokeDouble(function, address, callFlags, args);
    }

    public static void invokeVoid(Function function, long address, int callFlags, Object... args) {
        Native.invokeVoid(function, address, callFlags, args);
    }

    public static long invokePointer(Function function, long address, int callFlags, Object... args) {
        return Native.invokePointer(function, address, callFlags, args);
    }

    public static Structure invokeStructure(Structure structure, Function function, long address, int callFlags, Object... args) {
        return Native.invokeStructure(function, address, callFlags, args, structure);
    }

    public static long getSymbolAddress(NativeLibrary library, String symbolName) {
        return library.getSymbolAddress(symbolName);
    }

}
