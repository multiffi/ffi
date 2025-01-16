package io.github.multiffi.ffi;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public final class FFMAllocator {

    private FFMAllocator() {
        throw new AssertionError("No io.github.multiffi.ffi.FFMAllocator instances for you!");
    }

    private static final MethodHandle mallocMethodHandle;
    private static final MethodHandle freeMethodHandle;
    private static final MethodHandle callocMethodHandle;
    private static final MethodHandle reallocMethodHandle;
    private static final MethodHandle memchrMethodHandle;
    private static final MethodHandle memcmpMethodHandle;
    static {
        MemorySegment address = FFMUtil.DEFAULT_LOOKUP.find("malloc")
                .orElseThrow(() -> new UnsatisfiedLinkError("Failed to get symbol: `malloc`"));
        FunctionDescriptor signature = FunctionDescriptor.of(ValueLayout.ADDRESS, FFMUtil.SIZE_T);
        mallocMethodHandle = MethodHandles.filterReturnValue(
                FFMMethodFilters.filterSizeArgument(FFMUtil.LINKER.downcallHandle(address, signature), 0, false),
                FFMMethodFilters.SEGMENT_TO_INT64);
        address = FFMUtil.DEFAULT_LOOKUP.find("free")
                .orElseThrow(() -> new UnsatisfiedLinkError("Failed to get symbol: `free`"));
        signature = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
        freeMethodHandle = MethodHandles.filterArguments(FFMUtil.LINKER.downcallHandle(address, signature), 0,
                FFMMethodFilters.INT64_TO_SEGMENT);
        address = FFMUtil.DEFAULT_LOOKUP.find("calloc")
                .orElseThrow(() -> new UnsatisfiedLinkError("Failed to get symbol: `calloc`"));
        signature = FunctionDescriptor.of(ValueLayout.ADDRESS, FFMUtil.SIZE_T, FFMUtil.SIZE_T);
        callocMethodHandle = MethodHandles.filterReturnValue(
                FFMMethodFilters.filterSizeArgument(
                        FFMMethodFilters.filterSizeArgument(FFMUtil.LINKER.downcallHandle(address, signature), 0, false), 1, false),
                FFMMethodFilters.SEGMENT_TO_INT64);
        address = FFMUtil.DEFAULT_LOOKUP.find("realloc")
                .orElseThrow(() -> new UnsatisfiedLinkError("Failed to get symbol: `realloc`"));
        signature = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, FFMUtil.SIZE_T);
        reallocMethodHandle = MethodHandles.filterReturnValue(
                FFMMethodFilters.filterSizeArgument(
                        MethodHandles.filterArguments(FFMUtil.LINKER.downcallHandle(address, signature), 0,
                                FFMMethodFilters.INT64_TO_SEGMENT), 1, false),
                FFMMethodFilters.SEGMENT_TO_INT64);
        address = FFMUtil.DEFAULT_LOOKUP.find("memchr")
                .orElseThrow(() -> new UnsatisfiedLinkError("Failed to get symbol: `memchr`"));
        signature = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, FFMUtil.INT, FFMUtil.SIZE_T);
        memchrMethodHandle = MethodHandles.filterReturnValue(
                FFMMethodFilters.filterSizeArgument(
                        FFMMethodFilters.filterIntArgument(
                                MethodHandles.filterArguments(FFMUtil.LINKER.downcallHandle(address, signature), 0,
                                        FFMMethodFilters.INT64_TO_SEGMENT), 1, false), 2, false), FFMMethodFilters.SEGMENT_TO_INT64);
        address = FFMUtil.DEFAULT_LOOKUP.find("memcmp")
                .orElseThrow(() -> new UnsatisfiedLinkError("Failed to get symbol: `memcmp`"));
        signature = FunctionDescriptor.of(FFMUtil.INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, FFMUtil.SIZE_T);
        memcmpMethodHandle = FFMMethodFilters.filterIntReturnValue(
                FFMMethodFilters.filterSizeArgument(
                        MethodHandles.filterArguments(
                                MethodHandles.filterArguments(FFMUtil.LINKER.downcallHandle(address, signature)
                                        , 0, FFMMethodFilters.INT64_TO_SEGMENT), 1, FFMMethodFilters.INT64_TO_SEGMENT), 2, false), false);
    }
    
    public static long allocate(long size) {
        try {
            return (long) mallocMethodHandle.invokeExact(size);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static long allocateInitialized(long count, long size) {
        try {
            return (long) callocMethodHandle.invokeExact(count, size);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static long reallocate(long address, long size) {
        try {
            return (long) reallocMethodHandle.invokeExact(address, size);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static void free(long address) {
        try {
            freeMethodHandle.invokeExact(address);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static long search(long address, byte value, long maxLength) {
        try {
            return (long) memchrMethodHandle.invokeExact(address, value, maxLength);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static int compare(long aAddress, long bAddress, long size) {
        try {
            return (int) memcmpMethodHandle.invokeExact(aAddress, bAddress, size);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

}
