package io.github.multiffi.ffi;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

@SuppressWarnings({"deprecation", "removal"})
public final class JNAAlignedAllocator {
    
    private JNAAlignedAllocator() {
        throw new AssertionError("No io.github.multiffi.ffi.JNAAlignedAllocator instances for you!");
    }

    private static final boolean SUPPORTED;
    private static native Pointer _aligned_malloc(PointerSize size, PointerSize alignment);
    private static native Pointer _aligned_realloc(Pointer address, PointerSize size, PointerSize alignment);
    private static native void _aligned_free(Pointer address);
    static {
        boolean supported = false;
        if (Platform.isWindows()) {
            try {
                Native.register(JNAAlignedAllocator.class, Platform.C_LIBRARY_NAME);
                supported = true;
            } catch (Throwable ignored) {
            }
        }
        SUPPORTED = supported;
    }

    public static long allocate(long size, long alignment) {
        if (alignment == 0 || (alignment & (alignment - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
        if (SUPPORTED) return Pointer.nativeValue(_aligned_malloc(new PointerSize(size), new PointerSize(alignment)));
        else {
            long original;
            long offset = alignment - 1 + Native.POINTER_SIZE;
            if ((original = Native.malloc(Util.unsignedAddExact(size, offset))) == 0L) return 0L;
            long aligned = (original + offset) & -alignment;
            JNAUtil.UNSAFE.putAddress(aligned - Native.POINTER_SIZE, original);
            return aligned;
        }
    }

    public static long allocateInitialized(long count, long size, long alignment) {
        long address = allocate(Util.unsignedMultiplyExact(count, size), alignment);
        JNAUtil.UNSAFE.setMemory(address, size, (byte) 0);
        return address;
    }

    public static long reallocate(long address, long size, long alignment) {
        if (alignment == 0 || (alignment & (alignment - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
        if (SUPPORTED) return Pointer.nativeValue(_aligned_realloc(new Pointer(address), new PointerSize(size), new PointerSize(alignment)));
        else {
            long aligned = JNAUtil.UNSAFE.getAddress(address - Native.POINTER_SIZE);
            long original;
            long offset = alignment - 1 + Native.POINTER_SIZE;
            if ((original = JNAAllocator.reallocate(aligned, Util.unsignedAddExact(size, offset))) == 0L)
                return 0L;
            if (original != aligned) Native.free(aligned);
            aligned = (original + offset) & -alignment;
            JNAUtil.UNSAFE.putAddress(aligned - Native.POINTER_SIZE, original);
            return aligned;
        }
    }

    public static void free(long address) {
        if (SUPPORTED) _aligned_free(new Pointer(address));
        else {
            Native.free(JNAUtil.UNSAFE.getAddress(address - Native.POINTER_SIZE));
        }
    }
    
}
