package io.github.multiffi.ffi;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

public final class JNAAllocator {

    private JNAAllocator() {
        throw new AssertionError("No io.github.multiffi.ffi.JNAAllocator instances for you!");
    }

    private static native Pointer calloc(PointerSize count, PointerSize size);
    private static native Pointer realloc(Pointer address, PointerSize size);
    private static native Pointer memchr(Pointer address, int value, PointerSize maxLength);
    private static native int memcmp(Pointer aAddress, Pointer bAddress, PointerSize size);
    static {
        Native.register(JNAMemoryProvider.class, Platform.C_LIBRARY_NAME);
    }

    public static long allocate(long size) {
        return Native.malloc(size);
    }

    public static long allocateInitialized(long count, long size) {
        return Pointer.nativeValue(calloc(new PointerSize(count), new PointerSize(size)));
    }

    public static long reallocate(long address, long size) {
        return Pointer.nativeValue(realloc(new Pointer(address), new PointerSize(size)));
    }

    public static void free(long address) {
        Native.free(address);
    }

    public static long search(long address, byte value, long maxLength) {
        return Pointer.nativeValue(memchr(new Pointer(address), value & 0xFF, new PointerSize(maxLength)));
    }

    public static int compare(long aAddress, long bAddress, long size) {
        return memcmp(new Pointer(aAddress), new Pointer(bAddress), new PointerSize(size));
    }
    
}
