package io.github.multiffi.ffi;

import jnr.ffi.LibraryLoader;
import jnr.ffi.annotations.IgnoreError;
import jnr.ffi.types.caddr_t;
import jnr.ffi.types.size_t;

public final class JNRAllocator {

    private JNRAllocator() {
        throw new AssertionError("No io.github.multiffi.ffi.JNRAllocator instances for you!");
    }

    public interface CLibrary {
        CLibrary INSTANCE = LibraryLoader.create(CLibrary.class)
                .load(JNRUtil.PLATFORM.getName().startsWith("Windows CE") ? "coredll" : JNRUtil.PLATFORM.getStandardCLibraryName());
        @IgnoreError
        @caddr_t
        long calloc(@size_t long count, @size_t long size);
        @IgnoreError
        @caddr_t long realloc(@caddr_t long address, @size_t long size);
        @IgnoreError
        int memcmp(@caddr_t long aAddress, @caddr_t long bAddress, @size_t long size);
    }

    public static long allocate(long size) {
        return JNRUtil.MEMORY_IO.allocateMemory(size, false);
    }

    public static long allocateInitialized(long count, long size) {
        return CLibrary.INSTANCE.calloc(count, size);
    }

    public static long reallocate(long address, long size) {
        return CLibrary.INSTANCE.realloc(address, size);
    }

    public static void free(long address) {
        JNRUtil.MEMORY_IO.freeMemory(address);
    }

    public static long search(long address, byte value, long maxLength) {
        return JNRUtil.MEMORY_IO.memchr(address, value & 0xFF, maxLength);
    }

    public static int compare(long aAddress, long bAddress, long size) {
        return CLibrary.INSTANCE.memcmp(aAddress, bAddress, size);
    }
    
}
