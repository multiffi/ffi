package io.github.multiffi;

import multiffi.UnsatisfiedLinkException;
import multiffi.spi.AllocatorProvider;
import sun.misc.Unsafe;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

@SuppressWarnings({"restricted", "deprecated", "removal"})
public class FFMAllocatorProvider extends AllocatorProvider {

    private static final Unsafe UNSAFE;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to get the sun.misc.Unsafe instance");
        }
    }

    private static final ValueLayout SIZE_T = UNSAFE.addressSize() == 8 ? JAVA_LONG : JAVA_INT;
    private static final Linker LINKER = Linker.nativeLinker();
    private static final MethodHandle CALLOC;
    private static final MethodHandle MEMCHR;
    private static final MethodHandle MEMCMP;
    static {
        SymbolLookup stdlib = LINKER.defaultLookup();

        MemorySegment address = stdlib.find("calloc")
                .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `calloc`"));
        FunctionDescriptor signature = FunctionDescriptor.of(SIZE_T, SIZE_T, SIZE_T);
        CALLOC = LINKER.downcallHandle(address, signature);

        address = stdlib.find("memchr")
                .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `memchr`"));
        signature = FunctionDescriptor.of(SIZE_T, SIZE_T, JAVA_INT, SIZE_T);
        MEMCHR = LINKER.downcallHandle(address, signature);

        address = stdlib.find("memcmp")
                .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `memcmp`"));
        signature = FunctionDescriptor.of(JAVA_INT, SIZE_T, SIZE_T, SIZE_T);
        MEMCMP = LINKER.downcallHandle(address, signature);
    }

    private static long checkAddress(long address) {
        if (UNSAFE.addressSize() == 4) {
            // Accept both zero and sign extended pointers. A valid
            // pointer will, after the +1 below, either have produced
            // the value 0x0 or 0x1. Masking off the low bit allows
            // for testing against 0.
            if ((((address >> 32) + 1) & ~1) != 0) throw new ArithmeticException("integer overflow");
        }
        return address;
    }

    private static long checkAddress(long address, long offset) {
        return checkAddress(unsignedAddExact(address, offset));
    }
    
    private static long getArrayContentSize(Object array) {
        if (array == null || !array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())
            throw new IllegalArgumentException("not a primitive array");
        else if (array.getClass().getComponentType() == boolean.class) throw new IllegalArgumentException("boolean array not supported");
        else return (long) Array.getLength(array) * UNSAFE.arrayIndexScale(array.getClass());
    }

    private static void checkArray(Object array, long arrayOffset, long size) {
        long arrayLength = getArrayContentSize(array);
        long index = unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
    }

    @Override
    public long allocate(long size) {
        return UNSAFE.allocateMemory(size);
    }

    @Override
    public long allocateInitialized(long count, long size) {
        try {
            return (long) CALLOC.invokeExact(count, size);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public long reallocate(long address, long size) {
        return UNSAFE.reallocateMemory(address, size);
    }

    @Override
    public void free(long address) {
        UNSAFE.freeMemory(address);
    }

    @Override
    public long search(long address, byte value, long maxLength) {
        try {
            return (long) MEMCHR.invokeExact(checkAddress(address), value, maxLength);
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
        long arrayBaseOffset = UNSAFE.arrayBaseOffset(array.getClass());
        for (long i = 0; i < maxLength; i ++) {
            if (UNSAFE.getByte(array, arrayBaseOffset + arrayOffset + i) == value) return arrayOffset + i;
        }
        return -1;
    }

    @Override
    public int compare(long aAddress, long bAddress, long size) {
        try {
            return (int) MEMCMP.invokeExact(checkAddress(aAddress), checkAddress(bAddress), size);
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
        checkAddress(aAddress, size);
        long arrayBaseOffset = UNSAFE.arrayBaseOffset(bArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Byte.compareUnsigned(UNSAFE.getByte(aAddress + i),
                    UNSAFE.getByte(bArray, arrayBaseOffset + bArrayOffset + i));
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
        checkAddress(bAddress, size);
        long arrayBaseOffset = UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Byte.compareUnsigned(UNSAFE.getByte(aArray, arrayBaseOffset + aArrayOffset + i),
                    UNSAFE.getByte(bAddress + i));
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
        long aArrayBaseOffset = UNSAFE.arrayBaseOffset(aArray.getClass());
        long bArrayBaseOffset = UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Byte.compareUnsigned(UNSAFE.getByte(aArray, aArrayBaseOffset + aArrayOffset + i),
                    UNSAFE.getByte(bArrayBaseOffset + bArrayOffset + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public long fill(long address, byte value, long size) {
        UNSAFE.setMemory(address, size, value);
        return address;
    }

    @Override
    public Object fill(Object array, long arrayOffset, byte value, long size) {
        long arrayLength = getArrayContentSize(array);
        long index = unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        UNSAFE.setMemory(array, arrayOffset, size, value);
        return array;
    }

    @Override
    public long copy(long destAddress, long srcAddress, long size) {
        UNSAFE.copyMemory(srcAddress, destAddress, size);
        return destAddress;
    }

    @Override
    public long copy(long destAddress, Object srcArray, long srcArrayOffset, long size) {
        long arrayLength = getArrayContentSize(srcArray);
        long index = unsignedAddExact(srcArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        checkAddress(destAddress, size);
        MemorySegment memorySegment;
        if (srcArray instanceof byte[]) memorySegment = MemorySegment.ofArray((byte[]) srcArray);
        else if (srcArray instanceof char[]) memorySegment = MemorySegment.ofArray((char[]) srcArray);
        else if (srcArray instanceof short[]) memorySegment = MemorySegment.ofArray((short[]) srcArray);
        else if (srcArray instanceof int[]) memorySegment = MemorySegment.ofArray((int[]) srcArray);
        else if (srcArray instanceof long[]) memorySegment = MemorySegment.ofArray((long[]) srcArray);
        else if (srcArray instanceof float[]) memorySegment = MemorySegment.ofArray((float[]) srcArray);
        else if (srcArray instanceof double[]) memorySegment = MemorySegment.ofArray((double[]) srcArray);
        else {
            long arrayBaseOffset = UNSAFE.arrayBaseOffset(srcArray.getClass());
            for (long i = 0; i < size; i ++) {
                UNSAFE.putByte(destAddress + i, UNSAFE.getByte(srcArray, arrayBaseOffset + srcArrayOffset + i));
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
        checkAddress(srcAddress, size);
        MemorySegment memorySegment;
        if (destArray instanceof byte[]) memorySegment = MemorySegment.ofArray((byte[]) destArray);
        else if (destArray instanceof char[]) memorySegment = MemorySegment.ofArray((char[]) destArray);
        else if (destArray instanceof short[]) memorySegment = MemorySegment.ofArray((short[]) destArray);
        else if (destArray instanceof int[]) memorySegment = MemorySegment.ofArray((int[]) destArray);
        else if (destArray instanceof long[]) memorySegment = MemorySegment.ofArray((long[]) destArray);
        else if (destArray instanceof float[]) memorySegment = MemorySegment.ofArray((float[]) destArray);
        else if (destArray instanceof double[]) memorySegment = MemorySegment.ofArray((double[]) destArray);
        else {
            long arrayBaseOffset = UNSAFE.arrayBaseOffset(destArray.getClass());
            for (long i = 0; i < size; i ++) {
                UNSAFE.putByte(destArray, arrayBaseOffset + destArrayOffset + i, UNSAFE.getByte(srcAddress + i));
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
        long destArrayBaseOffset = UNSAFE.arrayBaseOffset(destArray.getClass());
        long srcArrayBaseOffset = UNSAFE.arrayBaseOffset(srcArray.getClass());
        UNSAFE.copyMemory(srcArray, srcArrayBaseOffset + srcArrayOffset,
                destArray, destArrayBaseOffset + destArrayOffset, size);
        return destArray;
    }

    @Override
    public boolean getBoolean(long address) {
        return UNSAFE.getByte(address) != 0;
    }

    @Override
    public byte getInt8(long address) {
        return UNSAFE.getByte(address);
    }

    @Override
    public short getInt16(long address) {
        return UNSAFE.getShort(address);
    }

    @Override
    public char getUTF16(long address) {
        return UNSAFE.getChar(address);
    }

    @Override
    public int getInt32(long address) {
        return UNSAFE.getInt(address);
    }

    @Override
    public long getInt64(long address) {
        return UNSAFE.getLong(address);
    }

    @Override
    public float getFloat(long address) {
        return UNSAFE.getFloat(address);
    }

    @Override
    public double getDouble(long address) {
        return UNSAFE.getDouble(address);
    }

    @Override
    public void setBoolean(long address, boolean value) {
        UNSAFE.putByte(address, (byte) (value ? 1 : 0));
    }

    @Override
    public void setInt8(long address, byte value) {
        UNSAFE.putByte(address, value);
    }

    @Override
    public void setInt16(long address, short value) {
        UNSAFE.putShort(address, value);
    }

    @Override
    public void setUTF16(long address, char value) {
        UNSAFE.putChar(address, value);
    }

    @Override
    public void setInt32(long address, int value) {
        UNSAFE.putInt(address, value);
    }

    @Override
    public void setInt64(long address, long value) {
        UNSAFE.putLong(address, value);
    }

    @Override
    public void setFloat(long address, float value) {
        UNSAFE.putFloat(address, value);
    }

    @Override
    public void setDouble(long address, double value) {
        UNSAFE.putDouble(address, value);
    }

    @Override
    public boolean getBoolean(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return UNSAFE.getBoolean(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public byte getInt8(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return UNSAFE.getByte(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public short getInt16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return UNSAFE.getShort(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public char getUTF16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return UNSAFE.getChar(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public int getInt32(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return UNSAFE.getInt(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public long getInt64(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return UNSAFE.getLong(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public float getFloat(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return UNSAFE.getFloat(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public double getDouble(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return UNSAFE.getDouble(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public void setBoolean(Object array, long arrayOffset, boolean value) {
        checkArray(array, arrayOffset, 1);
        UNSAFE.putBoolean(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt8(Object array, long arrayOffset, byte value) {
        checkArray(array, arrayOffset, 1);
        UNSAFE.putByte(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt16(Object array, long arrayOffset, short value) {
        checkArray(array, arrayOffset, 2);
        UNSAFE.putShort(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setUTF16(Object array, long arrayOffset, char value) {
        checkArray(array, arrayOffset, 2);
        UNSAFE.putChar(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt32(Object array, long arrayOffset, int value) {
        checkArray(array, arrayOffset, 4);
        UNSAFE.putInt(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt64(Object array, long arrayOffset, long value) {
        checkArray(array, arrayOffset, 8);
        UNSAFE.putLong(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setFloat(Object array, long arrayOffset, float value) {
        checkArray(array, arrayOffset, 4);
        UNSAFE.putFloat(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setDouble(Object array, long arrayOffset, double value) {
        checkArray(array, arrayOffset, 8);
        UNSAFE.putDouble(array, UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    private static long unsignedAddExact(long x, long y) {
        long sum = x + y;
        if (Long.compareUnsigned(x, sum) > 0 && Long.compareUnsigned(y, sum) > 0) throw new ArithmeticException("long overflow");
        return sum;
    }

}
