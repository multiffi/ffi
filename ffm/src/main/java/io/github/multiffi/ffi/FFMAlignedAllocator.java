package io.github.multiffi.ffi;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;

@SuppressWarnings({"deprecation", "removal"})
public final class FFMAlignedAllocator {

    private FFMAlignedAllocator() {
        throw new AssertionError("No io.github.multiffi.ffi.FFMAlignedAllocator instances for you!");
    }

    @FunctionalInterface
    private interface LongTernaryOperator {
        long applyAsLong(long first, long second, long third);
    }
    
    private static final LongBinaryOperator allocateAlignedFunction;
    private static final LongTernaryOperator reallocateAlignedFunction;
    private static final LongConsumer freeAlignedFunction;

    static {
        LongBinaryOperator _allocateAlignedFunction = null;
        LongTernaryOperator _reallocateAlignedFunction = null;
        LongConsumer _freeAlignedFunction = null;
        if (FFMUtil.IS_WINDOWS) {
            MemorySegment _aligned_mallocAddress = FFMUtil.DEFAULT_LOOKUP.find("_aligned_malloc").orElse(null);
            MemorySegment _aligned_reallocAddress = FFMUtil.DEFAULT_LOOKUP.find("_aligned_realloc").orElse(null);
            MemorySegment _aligned_freeAddress = FFMUtil.DEFAULT_LOOKUP.find("_aligned_free").orElse(null);
            if (_aligned_mallocAddress != null && _aligned_reallocAddress != null && _aligned_freeAddress != null) {
                FunctionDescriptor signature = FunctionDescriptor.of(ValueLayout.ADDRESS, FFMUtil.SIZE_T, FFMUtil.SIZE_T);
                MethodHandle _aligned_malloc = MethodHandles.filterReturnValue(
                        FFMMethodFilters.filterAddressArgument(
                                FFMMethodFilters.filterAddressArgument(
                                        FFMUtil.LINKER.downcallHandle(_aligned_mallocAddress, signature), 0, false), 1, false),
                        FFMMethodFilters.SEGMENT_TO_INT64);
                _allocateAlignedFunction = (size, alignment) -> {
                    try {
                        return (long) _aligned_malloc.invokeExact(size, alignment);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
                signature = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, FFMUtil.SIZE_T, FFMUtil.SIZE_T);
                MethodHandle _aligned_realloc = MethodHandles.filterReturnValue(
                        FFMMethodFilters.filterAddressArgument(
                                FFMMethodFilters.filterAddressArgument(
                                        MethodHandles.filterArguments(
                                                FFMUtil.LINKER.downcallHandle(_aligned_reallocAddress, signature)
                                                , 0, FFMMethodFilters.INT64_TO_SEGMENT), 1, false), 2, false),
                        FFMMethodFilters.SEGMENT_TO_INT64);
                _reallocateAlignedFunction = (address, size, alignment) -> {
                    try {
                        return (long) _aligned_realloc.invokeExact(address, size, alignment);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
                signature = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
                MethodHandle _aligned_free = MethodHandles.filterArguments(
                        FFMUtil.LINKER.downcallHandle(_aligned_freeAddress, signature)
                        , 0, FFMMethodFilters.INT64_TO_SEGMENT);
                _freeAlignedFunction = address -> {
                    try {
                        _aligned_free.invokeExact(address);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
            }
        }
        if (_allocateAlignedFunction == null) _allocateAlignedFunction = (size, alignment) -> {
            long original;
            long offset = alignment - 1 + FFMUtil.ADDRESS_SIZE;
            if ((original = FFMAllocator.allocate(Util.unsignedAddExact(size, offset))) == 0L)
                return 0L;
            long aligned = (original + offset) & -alignment;
            FFMUtil.UNSAFE.putAddress(aligned - FFMUtil.ADDRESS_SIZE, original);
            return aligned;
        };
        if (_reallocateAlignedFunction == null) _reallocateAlignedFunction = (address, size, alignment) -> {
            long aligned = FFMUtil.UNSAFE.getAddress(address - FFMUtil.ADDRESS_SIZE);
            long original;
            long offset = alignment - 1 + FFMUtil.ADDRESS_SIZE;
            if ((original = FFMAllocator.reallocate(aligned, Util.unsignedAddExact(size, offset))) == 0L)
                return 0L;
            if (original != aligned) FFMAllocator.free(aligned);
            aligned = (original + offset) & -alignment;
            FFMUtil.UNSAFE.putAddress(aligned - FFMUtil.ADDRESS_SIZE, original);
            return aligned;
        };
        if (_freeAlignedFunction == null) _freeAlignedFunction = address -> {
            FFMAllocator.free(FFMUtil.UNSAFE.getAddress(address - FFMUtil.ADDRESS_SIZE));
        };
        allocateAlignedFunction = _allocateAlignedFunction;
        reallocateAlignedFunction = _reallocateAlignedFunction;
        freeAlignedFunction = _freeAlignedFunction;
    }
    
    public static long allocate(long size, long alignment) {
        if (alignment == 0 || (alignment & (alignment - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
        return allocateAlignedFunction.applyAsLong(size, alignment);
    }
    
    public static long allocateInitialized(long count, long size, long alignment) {
        if (alignment == 0 || (alignment & (alignment - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
        long address = allocateAlignedFunction.applyAsLong(FFMUtil.unsignedMultiplyExact(count, size), alignment);
        FFMUtil.UNSAFE.setMemory(address, size, (byte) 0);
        return address;
    }
    
    public static long reallocate(long address, long size, long alignment) {
        if (alignment == 0 || (alignment & (alignment - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
        return reallocateAlignedFunction.applyAsLong(address, size, alignment);
    }
    
    public static void free(long address) {
        freeAlignedFunction.accept(address);
    }
    
}
