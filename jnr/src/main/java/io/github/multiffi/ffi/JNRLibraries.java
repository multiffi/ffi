package io.github.multiffi.ffi;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Platform;
import jnr.ffi.annotations.IgnoreError;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.LongLongByReference;
import jnr.ffi.types.caddr_t;
import jnr.ffi.types.size_t;

public final class JNRLibraries {

    private JNRLibraries() {
        throw new AssertionError("No io.github.multiffi.ffi.JNRLibraries instances for you!");
    }

    public interface Kernel32 {
        Kernel32 INSTANCE = JNRUtil.PLATFORM.getOS() != Platform.OS.WINDOWS ? null :
                LibraryLoader.create(Kernel32.class).load(JNRUtil.PLATFORM.getName().startsWith("Windows CE") ? "coredll" : "kernel32");
        @IgnoreError
        int QueryPerformanceFrequency(@Out LongLongByReference lpFrequency);
        @IgnoreError
        int QueryPerformanceCounter(@Out LongLongByReference lpPerformanceCount);
        @IgnoreError
        void GetSystemTimeAsFileTime(@Out JNRMappedTypes.FILETIME lpSystemTimeAsFileTime);
        @IgnoreError
        int FormatMessageW(int dwFlags, @caddr_t long lpSource, int dwMessageId, int dwLanguageId,
                           @caddr_t long lpBuffer, int nSize, @caddr_t long arguments);
        @IgnoreError
        @caddr_t long LocalFree(@caddr_t long hMem);
    }

    public interface CLibrary {
        CLibrary INSTANCE = JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS ? null :
                LibraryLoader.create(CLibrary.class).load(JNRUtil.PLATFORM.getStandardCLibraryName());
        @IgnoreError
        int clock_gettime(int id, @Out JNRMappedTypes.timespec timespec);
        @IgnoreError
        @caddr_t long strerror(int errno);
    }

    public interface Memory {
        Memory INSTANCE = LibraryLoader.create(Memory.class)
                .load(JNRUtil.PLATFORM.getName().startsWith("Windows CE") ? "coredll" : JNRUtil.PLATFORM.getStandardCLibraryName());
        @IgnoreError
        @caddr_t long calloc(@size_t long count, @size_t long size);
        @IgnoreError
        @caddr_t long realloc(@caddr_t long address, @size_t long size);
        @IgnoreError
        int memcmp(@caddr_t long aAddress, @caddr_t long bAddress, @size_t long size);
    }

}
