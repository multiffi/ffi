package io.github.multiffi;

import multiffi.UnsatisfiedLinkException;
import multiffi.spi.AllocatorProvider;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;

@SuppressWarnings({"deprecation", "removal"})
public class FFMAllocatorProvider extends AllocatorProvider {

    private static final class StdlibHolder {
        private StdlibHolder() {
            throw new UnsupportedOperationException();
        }
        public static final MethodHandle MALLOC_HANDLE;
        public static final MethodHandle FREE_HANDLE;
        public static final MethodHandle CALLOC_HANDLE;
        public static final MethodHandle REALLOC_HANDLE;
        public static final MethodHandle MEMCHR_HANDLE;
        public static final MethodHandle MEMCMP_HANDLE;
        static {
            SymbolLookup stdlib = FFMUtil.ABIHolder.LINKER.defaultLookup();
            MemorySegment address = stdlib.find("malloc")
                    .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `malloc`"));
            FunctionDescriptor signature = FunctionDescriptor.of(ValueLayout.ADDRESS, FFMUtil.ABIHolder.SIZE_T);
            MALLOC_HANDLE = MethodHandles.filterReturnValue(
                    FFMMethodFilters.filterAddressArgument(FFMUtil.ABIHolder.LINKER.downcallHandle(address, signature), 0, false),
                    FFMMethodFilters.SEGMENT_TO_INT64);
            address = stdlib.find("free")
                    .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `free`"));
            signature = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
            FREE_HANDLE = MethodHandles.filterArguments(FFMUtil.ABIHolder.LINKER.downcallHandle(address, signature), 0,
                    FFMMethodFilters.INT64_TO_SEGMENT);
            address = stdlib.find("calloc")
                    .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `calloc`"));
            signature = FunctionDescriptor.of(ValueLayout.ADDRESS, FFMUtil.ABIHolder.SIZE_T, FFMUtil.ABIHolder.SIZE_T);
            CALLOC_HANDLE = MethodHandles.filterReturnValue(
                    FFMMethodFilters.filterAddressArgument(
                            FFMMethodFilters.filterAddressArgument(FFMUtil.ABIHolder.LINKER.downcallHandle(address, signature), 0, false), 1, false),
                    FFMMethodFilters.SEGMENT_TO_INT64);
            address = stdlib.find("realloc")
                    .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `realloc`"));
            signature = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, FFMUtil.ABIHolder.SIZE_T);
            REALLOC_HANDLE = MethodHandles.filterReturnValue(
                    FFMMethodFilters.filterAddressArgument(
                            MethodHandles.filterArguments(FFMUtil.ABIHolder.LINKER.downcallHandle(address, signature), 0,
                                    FFMMethodFilters.INT64_TO_SEGMENT), 1, false),
                    FFMMethodFilters.SEGMENT_TO_INT64);
            address = stdlib.find("memchr")
                    .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `memchr`"));
            signature = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, FFMUtil.ABIHolder.INT, FFMUtil.ABIHolder.SIZE_T);
            MEMCHR_HANDLE = MethodHandles.filterReturnValue(
                    FFMMethodFilters.filterAddressArgument(
                            FFMMethodFilters.filterIntArgument(
                                    MethodHandles.filterArguments(FFMUtil.ABIHolder.LINKER.downcallHandle(address, signature), 0,
                                            FFMMethodFilters.INT64_TO_SEGMENT), 1, false), 2, false), FFMMethodFilters.SEGMENT_TO_INT64);
            address = stdlib.find("memcmp")
                    .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `memcmp`"));
            signature = FunctionDescriptor.of(FFMUtil.ABIHolder.INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, FFMUtil.ABIHolder.SIZE_T);
            MEMCMP_HANDLE = FFMMethodFilters.filterIntReturnValue(
                    FFMMethodFilters.filterAddressArgument(
                            MethodHandles.filterArguments(
                                    MethodHandles.filterArguments(FFMUtil.ABIHolder.LINKER.downcallHandle(address, signature)
                                            , 0, FFMMethodFilters.INT64_TO_SEGMENT), 1, FFMMethodFilters.INT64_TO_SEGMENT), 2, false), false);
        }
    }

    private static long unsignedAddExact(long x, long y) {
        long sum = x + y;
        if (Long.compareUnsigned(x, sum) > 0) throw new ArithmeticException("long overflow");
        return sum;
    }

    private static long getArrayContentSize(Object array) {
        if (array == null || !array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())
            throw new IllegalArgumentException("not a primitive array");
        else if (array.getClass().getComponentType() == boolean.class) throw new IllegalArgumentException("boolean array not supported");
        else return (long) Array.getLength(array) * FFMUtil.UnsafeHolder.UNSAFE.arrayIndexScale(array.getClass());
    }

    private static void checkArray(Object array, long arrayOffset, long size) {
        long arrayLength = getArrayContentSize(array);
        long index = unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
    }

    @Override
    public long allocate(long size) {
        try {
            return (long) StdlibHolder.MALLOC_HANDLE.invokeExact(size);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public long allocateInitialized(long count, long size) {
        try {
            return (long) StdlibHolder.CALLOC_HANDLE.invokeExact(count, size);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public long reallocate(long address, long size) {
        try {
            return (long) StdlibHolder.REALLOC_HANDLE.invokeExact(address, size);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void free(long address) {
       try {
           StdlibHolder.FREE_HANDLE.invokeExact(address);
       } catch (RuntimeException | Error e) {
           throw e;
       } catch (Throwable e) {
           throw new IllegalStateException(e);
       }
    }

    @Override
    public long search(long address, byte value, long maxLength) {
        try {
            return (long) StdlibHolder.MEMCHR_HANDLE.invokeExact(address, value, maxLength);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public long search(Object array, long arrayOffset, byte value, long maxLength) {
        long arrayLength = getArrayContentSize(array);
        if (arrayOffset < 0 || arrayOffset >= arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(arrayOffset));
        long remaining = arrayLength - arrayOffset;
        if (maxLength < 0 || maxLength > remaining) maxLength = remaining;
        long arrayBaseOffset = FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass());
        for (long i = 0; i < maxLength; i ++) {
            if (FFMUtil.UnsafeHolder.UNSAFE.getByte(array, arrayBaseOffset + arrayOffset + i) == value) return arrayOffset + i;
        }
        return -1;
    }

    @Override
    public int compare(long aAddress, long bAddress, long size) {
        try {
            return (int) StdlibHolder.MEMCMP_HANDLE.invokeExact(aAddress, bAddress, size);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int compare(long aAddress, Object bArray, long bArrayOffset, long size) {
        long arrayLength = getArrayContentSize(bArray);
        long index = unsignedAddExact(bArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        unsignedAddExact(aAddress, size);
        long arrayBaseOffset = FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(bArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Byte.compareUnsigned(FFMUtil.UnsafeHolder.UNSAFE.getByte(aAddress + i),
                    FFMUtil.UnsafeHolder.UNSAFE.getByte(bArray, arrayBaseOffset + bArrayOffset + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public int compare(Object aArray, long aArrayOffset, long bAddress, long size) {
        long arrayLength = getArrayContentSize(aArray);
        long index = unsignedAddExact(aArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        unsignedAddExact(bAddress, size);
        long arrayBaseOffset = FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Byte.compareUnsigned(FFMUtil.UnsafeHolder.UNSAFE.getByte(aArray, arrayBaseOffset + aArrayOffset + i),
                    FFMUtil.UnsafeHolder.UNSAFE.getByte(bAddress + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public int compare(Object aArray, long aArrayOffset, Object bArray, long bArrayOffset, long size) {
        long aArrayLength = getArrayContentSize(aArray);
        long bArrayLength = getArrayContentSize(bArray);
        long index = unsignedAddExact(aArrayOffset, size);
        if (index < 0 || index > aArrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        index = unsignedAddExact(bArrayOffset, size);
        if (index < 0 || index > bArrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        long aArrayBaseOffset = FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(aArray.getClass());
        long bArrayBaseOffset = FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Byte.compareUnsigned(FFMUtil.UnsafeHolder.UNSAFE.getByte(aArray, aArrayBaseOffset + aArrayOffset + i),
                    FFMUtil.UnsafeHolder.UNSAFE.getByte(bArrayBaseOffset + bArrayOffset + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public long fill(long address, byte value, long size) {
        FFMUtil.UnsafeHolder.UNSAFE.setMemory(address, size, value);
        return address;
    }

    @Override
    public Object fill(Object array, long arrayOffset, byte value, long size) {
        long arrayLength = getArrayContentSize(array);
        long index = unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        FFMUtil.UnsafeHolder.UNSAFE.setMemory(array, arrayOffset, size, value);
        return array;
    }

    @Override
    public long copy(long destAddress, long srcAddress, long size) {
        FFMUtil.UnsafeHolder.UNSAFE.copyMemory(srcAddress, destAddress, size);
        return destAddress;
    }

    @Override
    public long copy(long destAddress, Object srcArray, long srcArrayOffset, long size) {
        long arrayLength = getArrayContentSize(srcArray);
        long index = unsignedAddExact(srcArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        unsignedAddExact(destAddress, size);
        MemorySegment memorySegment;
        if (srcArray instanceof byte[]) memorySegment = MemorySegment.ofArray((byte[]) srcArray);
        else if (srcArray instanceof char[]) memorySegment = MemorySegment.ofArray((char[]) srcArray);
        else if (srcArray instanceof short[]) memorySegment = MemorySegment.ofArray((short[]) srcArray);
        else if (srcArray instanceof int[]) memorySegment = MemorySegment.ofArray((int[]) srcArray);
        else if (srcArray instanceof long[]) memorySegment = MemorySegment.ofArray((long[]) srcArray);
        else if (srcArray instanceof float[]) memorySegment = MemorySegment.ofArray((float[]) srcArray);
        else if (srcArray instanceof double[]) memorySegment = MemorySegment.ofArray((double[]) srcArray);
        else {
            long arrayBaseOffset = FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(srcArray.getClass());
            for (long i = 0; i < size; i ++) {
                FFMUtil.UnsafeHolder.UNSAFE.putByte(destAddress + i,
                        FFMUtil.UnsafeHolder.UNSAFE.getByte(srcArray, arrayBaseOffset + srcArrayOffset + i));
            }
            return destAddress;
        }
        MemorySegment.ofAddress(destAddress).reinterpret(size).copyFrom(memorySegment.asSlice(srcArrayOffset));
        return destAddress;
    }

    @Override
    public Object copy(Object destArray, long destArrayOffset, long srcAddress, long size) {
        long arrayLength = getArrayContentSize(destArray);
        long index = unsignedAddExact(destArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        unsignedAddExact(srcAddress, size);
        MemorySegment memorySegment;
        if (destArray instanceof byte[]) memorySegment = MemorySegment.ofArray((byte[]) destArray);
        else if (destArray instanceof char[]) memorySegment = MemorySegment.ofArray((char[]) destArray);
        else if (destArray instanceof short[]) memorySegment = MemorySegment.ofArray((short[]) destArray);
        else if (destArray instanceof int[]) memorySegment = MemorySegment.ofArray((int[]) destArray);
        else if (destArray instanceof long[]) memorySegment = MemorySegment.ofArray((long[]) destArray);
        else if (destArray instanceof float[]) memorySegment = MemorySegment.ofArray((float[]) destArray);
        else if (destArray instanceof double[]) memorySegment = MemorySegment.ofArray((double[]) destArray);
        else {
            long arrayBaseOffset = FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(destArray.getClass());
            for (long i = 0; i < size; i ++) {
                FFMUtil.UnsafeHolder.UNSAFE.putByte(destArray, arrayBaseOffset + destArrayOffset + i,
                        FFMUtil.UnsafeHolder.UNSAFE.getByte(srcAddress + i));
            }
            return destArray;
        }
        memorySegment.asSlice(destArrayOffset).copyFrom(MemorySegment.ofAddress(srcAddress).reinterpret(size));
        return destArray;
    }

    @Override
    public Object copy(Object destArray, long destArrayOffset, Object srcArray, long srcArrayOffset, long size) {
        long destArrayLength = getArrayContentSize(destArray);
        long srcArrayLength = getArrayContentSize(srcArray);
        long index = unsignedAddExact(destArrayOffset, size);
        if (index < 0 || index > destArrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        index = unsignedAddExact(srcArrayOffset, size);
        if (index < 0 || index > srcArrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        long destArrayBaseOffset = FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(destArray.getClass());
        long srcArrayBaseOffset = FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(srcArray.getClass());
        FFMUtil.UnsafeHolder.UNSAFE.copyMemory(srcArray, srcArrayBaseOffset + srcArrayOffset,
                destArray, destArrayBaseOffset + destArrayOffset, size);
        return destArray;
    }

    @Override
    public boolean getBoolean(long address) {
        return FFMUtil.UnsafeHolder.UNSAFE.getByte(address) != 0;
    }

    @Override
    public byte getInt8(long address) {
        return FFMUtil.UnsafeHolder.UNSAFE.getByte(address);
    }

    @Override
    public short getInt16(long address) {
        return FFMUtil.UnsafeHolder.UNSAFE.getShort(address);
    }

    @Override
    public char getUTF16(long address) {
        return FFMUtil.UnsafeHolder.UNSAFE.getChar(address);
    }

    @Override
    public int getInt32(long address) {
        return FFMUtil.UnsafeHolder.UNSAFE.getInt(address);
    }

    @Override
    public long getInt64(long address) {
        return FFMUtil.UnsafeHolder.UNSAFE.getLong(address);
    }

    @Override
    public float getFloat(long address) {
        return FFMUtil.UnsafeHolder.UNSAFE.getFloat(address);
    }

    @Override
    public double getDouble(long address) {
        return FFMUtil.UnsafeHolder.UNSAFE.getDouble(address);
    }

    @Override
    public void setBoolean(long address, boolean value) {
        FFMUtil.UnsafeHolder.UNSAFE.putByte(address, (byte) (value ? 1 : 0));
    }

    @Override
    public void setInt8(long address, byte value) {
        FFMUtil.UnsafeHolder.UNSAFE.putByte(address, value);
    }

    @Override
    public void setInt16(long address, short value) {
        FFMUtil.UnsafeHolder.UNSAFE.putShort(address, value);
    }

    @Override
    public void setUTF16(long address, char value) {
        FFMUtil.UnsafeHolder.UNSAFE.putChar(address, value);
    }

    @Override
    public void setInt32(long address, int value) {
        FFMUtil.UnsafeHolder.UNSAFE.putInt(address, value);
    }

    @Override
    public void setInt64(long address, long value) {
        FFMUtil.UnsafeHolder.UNSAFE.putLong(address, value);
    }

    @Override
    public void setFloat(long address, float value) {
        FFMUtil.UnsafeHolder.UNSAFE.putFloat(address, value);
    }

    @Override
    public void setDouble(long address, double value) {
        FFMUtil.UnsafeHolder.UNSAFE.putDouble(address, value);
    }

    @Override
    public boolean getBoolean(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return FFMUtil.UnsafeHolder.UNSAFE.getBoolean(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public byte getInt8(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return FFMUtil.UnsafeHolder.UNSAFE.getByte(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public short getInt16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return FFMUtil.UnsafeHolder.UNSAFE.getShort(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public char getUTF16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return FFMUtil.UnsafeHolder.UNSAFE.getChar(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public int getInt32(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return FFMUtil.UnsafeHolder.UNSAFE.getInt(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public long getInt64(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return FFMUtil.UnsafeHolder.UNSAFE.getLong(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public float getFloat(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return FFMUtil.UnsafeHolder.UNSAFE.getFloat(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public double getDouble(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return FFMUtil.UnsafeHolder.UNSAFE.getDouble(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public void setBoolean(Object array, long arrayOffset, boolean value) {
        checkArray(array, arrayOffset, 1);
        FFMUtil.UnsafeHolder.UNSAFE.putBoolean(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt8(Object array, long arrayOffset, byte value) {
        checkArray(array, arrayOffset, 1);
        FFMUtil.UnsafeHolder.UNSAFE.putByte(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt16(Object array, long arrayOffset, short value) {
        checkArray(array, arrayOffset, 2);
        FFMUtil.UnsafeHolder.UNSAFE.putShort(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setUTF16(Object array, long arrayOffset, char value) {
        checkArray(array, arrayOffset, 2);
        FFMUtil.UnsafeHolder.UNSAFE.putChar(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt32(Object array, long arrayOffset, int value) {
        checkArray(array, arrayOffset, 4);
        FFMUtil.UnsafeHolder.UNSAFE.putInt(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt64(Object array, long arrayOffset, long value) {
        checkArray(array, arrayOffset, 8);
        FFMUtil.UnsafeHolder.UNSAFE.putLong(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setFloat(Object array, long arrayOffset, float value) {
        checkArray(array, arrayOffset, 4);
        FFMUtil.UnsafeHolder.UNSAFE.putFloat(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setDouble(Object array, long arrayOffset, double value) {
        checkArray(array, arrayOffset, 8);
        FFMUtil.UnsafeHolder.UNSAFE.putDouble(array, FFMUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

}
