package io.github.multiffi.ffi;

import multiffi.ffi.spi.AllocatorProvider;

import java.lang.reflect.Array;

@SuppressWarnings({"deprecation", "removal"})
public class JNRAllocatorProvider extends AllocatorProvider {

    private static int compareUnsigned(byte a, byte b) {
        return Byte.toUnsignedInt(a) - Byte.toUnsignedInt(b);
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
        else return (long) Array.getLength(array) * JNRUtil.UnsafeHolder.UNSAFE.arrayIndexScale(array.getClass());
    }

    private static void checkArray(Object array, long arrayOffset, long size) {
        long arrayLength = getArrayContentSize(array);
        long index = unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
    }

    @Override
    public long allocate(long size) {
        return JNRUtil.UnsafeHolder.MEMORY_IO.allocateMemory(size, false);
    }

    @Override
    public long allocateInitialized(long count, long size) {
        return JNRLibraries.Memory.INSTANCE.calloc(count, size);
    }

    @Override
    public long reallocate(long address, long size) {
        return JNRLibraries.Memory.INSTANCE.realloc(address, size);
    }

    @Override
    public void free(long address) {
        JNRUtil.UnsafeHolder.MEMORY_IO.freeMemory(address);
    }

    @Override
    public long search(long address, byte value, long maxLength) {
        return JNRUtil.UnsafeHolder.MEMORY_IO.memchr(address, value, maxLength);
    }

    @Override
    public long search(Object array, long arrayOffset, byte value, long maxLength) {
        long arrayLength = getArrayContentSize(array);
        if (arrayOffset < 0 || arrayOffset >= arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(arrayOffset));
        long remaining = arrayLength - arrayOffset;
        if (maxLength < 0 || maxLength > remaining) maxLength = remaining;
        long arrayBaseOffset = JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass());
        for (long i = 0; i < maxLength; i ++) {
            if (JNRUtil.UnsafeHolder.UNSAFE.getByte(array, arrayBaseOffset + arrayOffset + i) == value) return arrayOffset + i;
        }
        return -1;
    }

    @Override
    public int compare(long aAddress, long bAddress, long size) {
        return JNRLibraries.Memory.INSTANCE.memcmp(aAddress, bAddress, size);
    }

    @Override
    public int compare(long aAddress, Object bArray, long bArrayOffset, long size) {
        long arrayLength = getArrayContentSize(bArray);
        long index = unsignedAddExact(bArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        unsignedAddExact(aAddress, size);
        long arrayBaseOffset = JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(bArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = compareUnsigned(JNRUtil.UnsafeHolder.MEMORY_IO.getByte(aAddress + i),
                    JNRUtil.UnsafeHolder.UNSAFE.getByte(bArray, arrayBaseOffset + bArrayOffset + i));
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
        long arrayBaseOffset = JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = compareUnsigned(JNRUtil.UnsafeHolder.UNSAFE.getByte(aArray, arrayBaseOffset + aArrayOffset + i),
                    JNRUtil.UnsafeHolder.MEMORY_IO.getByte(bAddress + i));
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
        long aArrayBaseOffset = JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(aArray.getClass());
        long bArrayBaseOffset = JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = compareUnsigned(JNRUtil.UnsafeHolder.UNSAFE.getByte(aArray, aArrayBaseOffset + aArrayOffset + i),
                    JNRUtil.UnsafeHolder.UNSAFE.getByte(bArray, bArrayBaseOffset + bArrayOffset + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public long fill(long address, byte value, long size) {
        JNRUtil.UnsafeHolder.MEMORY_IO.setMemory(address, size, value);
        return address;
    }

    @Override
    public Object fill(Object array, long arrayOffset, byte value, long size) {
        long arrayLength = getArrayContentSize(array);
        long index = unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        JNRUtil.UnsafeHolder.UNSAFE.setMemory(array, arrayOffset, size, value);
        return array;
    }

    @Override
    public long copy(long destAddress, long srcAddress, long size) {
        JNRUtil.UnsafeHolder.MEMORY_IO.copyMemory(srcAddress, destAddress, size);
        return destAddress;
    }

    @Override
    public long copy(long destAddress, Object srcArray, long srcArrayOffset, long size) {
        long arrayLength = getArrayContentSize(srcArray);
        long index = unsignedAddExact(srcArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        unsignedAddExact(destAddress, size);
        long arrayBaseOffset = JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(srcArray.getClass());
        for (long i = 0; i < size; i ++) {
            JNRUtil.UnsafeHolder.MEMORY_IO.putByte(destAddress + i,
                    JNRUtil.UnsafeHolder.UNSAFE.getByte(srcArray, arrayBaseOffset + srcArrayOffset + i));
        }
        return destAddress;
    }

    @Override
    public Object copy(Object destArray, long destArrayOffset, long srcAddress, long size) {
        long arrayLength = getArrayContentSize(destArray);
        long index = unsignedAddExact(destArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        unsignedAddExact(srcAddress, size);
        long arrayBaseOffset = JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(destArray.getClass());
        for (long i = 0; i < size; i ++) {
            JNRUtil.UnsafeHolder.UNSAFE.putByte(destArray, arrayBaseOffset + destArrayOffset + i,
                    JNRUtil.UnsafeHolder.MEMORY_IO.getByte(srcAddress + i));
        }
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
        long destArrayBaseOffset = JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(destArray.getClass());
        long srcArrayBaseOffset = JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(srcArray.getClass());
        JNRUtil.UnsafeHolder.UNSAFE.copyMemory(srcArray, srcArrayBaseOffset + srcArrayOffset,
                destArray, destArrayBaseOffset + destArrayOffset, size);
        return destArray;
    }

    @Override
    public boolean getBoolean(long address) {
        return JNRUtil.UnsafeHolder.MEMORY_IO.getByte(address) != 0;
    }

    @Override
    public byte getInt8(long address) {
        return JNRUtil.UnsafeHolder.MEMORY_IO.getByte(address);
    }

    @Override
    public short getInt16(long address) {
        return JNRUtil.UnsafeHolder.MEMORY_IO.getShort(address);
    }

    @Override
    public char getUTF16(long address) {
        return (char) JNRUtil.UnsafeHolder.MEMORY_IO.getShort(address);
    }

    @Override
    public int getInt32(long address) {
        return JNRUtil.UnsafeHolder.MEMORY_IO.getInt(address);
    }

    @Override
    public long getInt64(long address) {
        return JNRUtil.UnsafeHolder.MEMORY_IO.getLong(address);
    }

    @Override
    public float getFloat(long address) {
        return JNRUtil.UnsafeHolder.MEMORY_IO.getFloat(address);
    }

    @Override
    public double getDouble(long address) {
        return JNRUtil.UnsafeHolder.MEMORY_IO.getDouble(address);
    }

    @Override
    public void setBoolean(long address, boolean value) {
        JNRUtil.UnsafeHolder.MEMORY_IO.putByte(address, (byte) (value ? 1 : 0));
    }

    @Override
    public void setInt8(long address, byte value) {
        JNRUtil.UnsafeHolder.MEMORY_IO.putByte(address, value);
    }

    @Override
    public void setInt16(long address, short value) {
        JNRUtil.UnsafeHolder.MEMORY_IO.putShort(address, value);
    }

    @Override
    public void setUTF16(long address, char value) {
        JNRUtil.UnsafeHolder.MEMORY_IO.putShort(address, (short) value);
    }

    @Override
    public void setInt32(long address, int value) {
        JNRUtil.UnsafeHolder.MEMORY_IO.putInt(address, value);
    }

    @Override
    public void setInt64(long address, long value) {
        JNRUtil.UnsafeHolder.MEMORY_IO.putLong(address, value);
    }

    @Override
    public void setFloat(long address, float value) {
        JNRUtil.UnsafeHolder.MEMORY_IO.putFloat(address, value);
    }

    @Override
    public void setDouble(long address, double value) {
        JNRUtil.UnsafeHolder.MEMORY_IO.putDouble(address, value);
    }

    @Override
    public boolean getBoolean(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return JNRUtil.UnsafeHolder.UNSAFE.getBoolean(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public byte getInt8(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return JNRUtil.UnsafeHolder.UNSAFE.getByte(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public short getInt16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return JNRUtil.UnsafeHolder.UNSAFE.getShort(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public char getUTF16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return JNRUtil.UnsafeHolder.UNSAFE.getChar(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public int getInt32(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return JNRUtil.UnsafeHolder.UNSAFE.getInt(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public long getInt64(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return JNRUtil.UnsafeHolder.UNSAFE.getLong(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public float getFloat(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return JNRUtil.UnsafeHolder.UNSAFE.getFloat(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public double getDouble(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return JNRUtil.UnsafeHolder.UNSAFE.getDouble(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public void setBoolean(Object array, long arrayOffset, boolean value) {
        checkArray(array, arrayOffset, 1);
        JNRUtil.UnsafeHolder.UNSAFE.putBoolean(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt8(Object array, long arrayOffset, byte value) {
        checkArray(array, arrayOffset, 1);
        JNRUtil.UnsafeHolder.UNSAFE.putByte(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt16(Object array, long arrayOffset, short value) {
        checkArray(array, arrayOffset, 2);
        JNRUtil.UnsafeHolder.UNSAFE.putShort(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setUTF16(Object array, long arrayOffset, char value) {
        checkArray(array, arrayOffset, 2);
        JNRUtil.UnsafeHolder.UNSAFE.putChar(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt32(Object array, long arrayOffset, int value) {
        checkArray(array, arrayOffset, 4);
        JNRUtil.UnsafeHolder.UNSAFE.putInt(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt64(Object array, long arrayOffset, long value) {
        checkArray(array, arrayOffset, 8);
        JNRUtil.UnsafeHolder.UNSAFE.putLong(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setFloat(Object array, long arrayOffset, float value) {
        checkArray(array, arrayOffset, 4);
        JNRUtil.UnsafeHolder.UNSAFE.putFloat(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setDouble(Object array, long arrayOffset, double value) {
        checkArray(array, arrayOffset, 8);
        JNRUtil.UnsafeHolder.UNSAFE.putDouble(array, JNRUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

}
