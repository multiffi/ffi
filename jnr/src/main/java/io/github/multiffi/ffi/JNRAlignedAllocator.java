package io.github.multiffi.ffi;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Platform;
import jnr.ffi.annotations.IgnoreError;
import jnr.ffi.types.caddr_t;
import jnr.ffi.types.size_t;

public final class JNRAlignedAllocator {

    private JNRAlignedAllocator() {
        throw new AssertionError("No io.github.multiffi.ffi.JNRAlignedAllocator instances for you!");
    }

    public interface CLibrary {
        CLibrary INSTANCE = initializeAlignedAllocator();
        @IgnoreError
        @caddr_t long _aligned_malloc(@size_t long size, @size_t long alignment);
        @IgnoreError
        @caddr_t long _aligned_realloc(@caddr_t long address, @size_t long size, @size_t long alignment);
        @IgnoreError
        void _aligned_free(@caddr_t long address);
    }

    private static CLibrary initializeAlignedAllocator() {
        if (JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS) {
            try {
                return LibraryLoader.create(CLibrary.class)
                        .load(JNRUtil.PLATFORM.getName().startsWith("Windows CE") ? "coredll" : JNRUtil.PLATFORM.getStandardCLibraryName());
            }
            catch (Throwable ignored) {
            }
        }
        return new CLibrary() {
            @Override
            public long _aligned_malloc(long size, long alignment) {
                long original;
                long offset = alignment - 1 + JNRUtil.ADDRESS_SIZE;
                if ((original = JNRUtil.MEMORY_IO.allocateMemory(Util.unsignedAddExact(size, offset), false)) == 0L) return 0L;
                long aligned = (original + offset) & -alignment;
                JNRUtil.MEMORY_IO.putAddress(aligned - JNRUtil.ADDRESS_SIZE, original);
                return aligned;
            }
            @Override
            public long _aligned_realloc(long address, long size, long alignment) {
                long aligned = JNRUtil.MEMORY_IO.getAddress(address - JNRUtil.ADDRESS_SIZE);
                long original;
                long offset = alignment - 1 + JNRUtil.ADDRESS_SIZE;
                if ((original = JNRAllocator.reallocate(aligned, Util.unsignedAddExact(size, offset))) == 0L) return 0L;
                if (original != aligned) JNRUtil.MEMORY_IO.freeMemory(aligned);
                aligned = (original + offset) & -alignment;
                JNRUtil.MEMORY_IO.putAddress(aligned - JNRUtil.ADDRESS_SIZE, original);
                return aligned;
            }
            @Override
            public void _aligned_free(long address) {
                JNRUtil.MEMORY_IO.freeMemory(JNRUtil.MEMORY_IO.getAddress(address - JNRUtil.ADDRESS_SIZE));
            }
        };
    }

    public static long allocate(long size, long alignment) {
        if (alignment == 0 || (alignment & (alignment - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
        return CLibrary.INSTANCE._aligned_malloc(size, alignment);
    }

    public static long allocateInitialized(long count, long size, long alignment) {
        if (alignment == 0 || (alignment & (alignment - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
        long address = CLibrary.INSTANCE._aligned_malloc(Util.unsignedMultiplyExact(count, size), alignment);
        JNRUtil.MEMORY_IO.setMemory(address, size, (byte) 0);
        return address;
    }

    public static long reallocate(long address, long size, long alignment) {
        if (alignment == 0 || (alignment & (alignment - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
        return CLibrary.INSTANCE._aligned_realloc(address, size, alignment);
    }

    public static void free(long address) {
        CLibrary.INSTANCE._aligned_free(address);
    }
    
}
