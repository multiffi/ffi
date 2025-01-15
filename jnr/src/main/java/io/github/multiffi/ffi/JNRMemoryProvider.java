package io.github.multiffi.ffi;

import multiffi.ffi.spi.MemoryProvider;

@SuppressWarnings({"deprecation", "removal"})
public class JNRMemoryProvider extends MemoryProvider {

    private static void checkArray(Object array, long arrayOffset, long size) {
        long arrayLength = Util.getArrayContentSize(array);
        long index = Util.unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
    }

    @Override
    public long allocate(long size) {
        return JNRAllocator.allocate(size);
    }

    @Override
    public long allocateInitialized(long count, long size) {
        return JNRAllocator.allocateInitialized(count, size);
    }

    @Override
    public long reallocate(long address, long size) {
        return JNRAllocator.reallocate(address, size);
    }

    @Override
    public void free(long address) {
        JNRAllocator.free(address);
    }

    @Override
    public long allocateAligned(long size, long alignment) {
        return JNRAlignedAllocator.allocate(size, alignment);
    }

    @Override
    public long allocateInitializedAligned(long count, long size, long alignment) {
        return JNRAlignedAllocator.allocateInitialized(count, size, alignment);
    }

    @Override
    public long reallocateAligned(long address, long size, long alignment) {
        return JNRAlignedAllocator.reallocate(address, size, alignment);
    }

    @Override
    public void freeAligned(long address) {
        JNRAlignedAllocator.free(address);
    }

    @Override
    public long search(long address, byte value, long maxLength) {
        return JNRAllocator.search(address, value, maxLength);
    }

    @Override
    public long search(long address, int value, long maxLength) {
        return JNRUtil.MEMORY_IO.memchr(address, value, maxLength);
    }

    @Override
    public long search(Object array, long arrayOffset, byte value, long maxLength) {
        long arrayLength = Util.getArrayContentSize(array);
        if (arrayOffset < 0 || arrayOffset >= arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(arrayOffset));
        long remaining = arrayLength - arrayOffset;
        if (maxLength < 0 || maxLength > remaining) maxLength = remaining;
        long arrayBaseOffset = JNRUtil.UNSAFE.arrayBaseOffset(array.getClass());
        for (long i = 0; i < maxLength; i ++) {
            if (JNRUtil.UNSAFE.getByte(array, arrayBaseOffset + arrayOffset + i) == value) return arrayOffset + i;
        }
        return -1;
    }

    @Override
    public int compare(long aAddress, long bAddress, long size) {
        return JNRAllocator.compare(aAddress, bAddress, size);
    }

    @Override
    public int compare(long aAddress, Object bArray, long bArrayOffset, long size) {
        long arrayLength = Util.getArrayContentSize(bArray);
        long index = Util.unsignedAddExact(bArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        Util.unsignedAddExact(aAddress, size);
        long arrayBaseOffset = JNRUtil.UNSAFE.arrayBaseOffset(bArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Util.compareUnsigned(JNRUtil.MEMORY_IO.getByte(aAddress + i),
                    JNRUtil.UNSAFE.getByte(bArray, arrayBaseOffset + bArrayOffset + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public int compare(Object aArray, long aArrayOffset, long bAddress, long size) {
        long arrayLength = Util.getArrayContentSize(aArray);
        long index = Util.unsignedAddExact(aArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        Util.unsignedAddExact(bAddress, size);
        long arrayBaseOffset = JNRUtil.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Util.compareUnsigned(JNRUtil.UNSAFE.getByte(aArray, arrayBaseOffset + aArrayOffset + i),
                    JNRUtil.MEMORY_IO.getByte(bAddress + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public int compare(Object aArray, long aArrayOffset, Object bArray, long bArrayOffset, long size) {
        long aArrayLength = Util.getArrayContentSize(aArray);
        long bArrayLength = Util.getArrayContentSize(bArray);
        long index = Util.unsignedAddExact(aArrayOffset, size);
        if (index < 0 || index > aArrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        index = Util.unsignedAddExact(bArrayOffset, size);
        if (index < 0 || index > bArrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        long aArrayBaseOffset = JNRUtil.UNSAFE.arrayBaseOffset(aArray.getClass());
        long bArrayBaseOffset = JNRUtil.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Util.compareUnsigned(JNRUtil.UNSAFE.getByte(aArray, aArrayBaseOffset + aArrayOffset + i),
                    JNRUtil.UNSAFE.getByte(bArray, bArrayBaseOffset + bArrayOffset + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public long fill(long address, byte value, long size) {
        JNRUtil.MEMORY_IO.setMemory(address, size, value);
        return address;
    }

    @Override
    public Object fill(Object array, long arrayOffset, byte value, long size) {
        long arrayLength = Util.getArrayContentSize(array);
        long index = Util.unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        JNRUtil.UNSAFE.setMemory(array, arrayOffset, size, value);
        return array;
    }

    @Override
    public long copy(long destAddress, long srcAddress, long size) {
        JNRUtil.MEMORY_IO.copyMemory(srcAddress, destAddress, size);
        return destAddress;
    }

    @Override
    public long copy(long destAddress, Object srcArray, long srcArrayOffset, long size) {
        long arrayLength = Util.getArrayContentSize(srcArray);
        long index = Util.unsignedAddExact(srcArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        Util.unsignedAddExact(destAddress, size);
        long arrayBaseOffset = JNRUtil.UNSAFE.arrayBaseOffset(srcArray.getClass());
        for (long i = 0; i < size; i ++) {
            JNRUtil.MEMORY_IO.putByte(destAddress + i,
                    JNRUtil.UNSAFE.getByte(srcArray, arrayBaseOffset + srcArrayOffset + i));
        }
        return destAddress;
    }

    @Override
    public Object copy(Object destArray, long destArrayOffset, long srcAddress, long size) {
        long arrayLength = Util.getArrayContentSize(destArray);
        long index = Util.unsignedAddExact(destArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        Util.unsignedAddExact(srcAddress, size);
        long arrayBaseOffset = JNRUtil.UNSAFE.arrayBaseOffset(destArray.getClass());
        for (long i = 0; i < size; i ++) {
            JNRUtil.UNSAFE.putByte(destArray, arrayBaseOffset + destArrayOffset + i,
                    JNRUtil.MEMORY_IO.getByte(srcAddress + i));
        }
        return destArray;
    }

    @Override
    public Object copy(Object destArray, long destArrayOffset, Object srcArray, long srcArrayOffset, long size) {
        long destArrayLength = Util.getArrayContentSize(destArray);
        long srcArrayLength = Util.getArrayContentSize(srcArray);
        long index = Util.unsignedAddExact(destArrayOffset, size);
        if (index < 0 || index > destArrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        index = Util.unsignedAddExact(srcArrayOffset, size);
        if (index < 0 || index > srcArrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        long destArrayBaseOffset = JNRUtil.UNSAFE.arrayBaseOffset(destArray.getClass());
        long srcArrayBaseOffset = JNRUtil.UNSAFE.arrayBaseOffset(srcArray.getClass());
        JNRUtil.UNSAFE.copyMemory(srcArray, srcArrayBaseOffset + srcArrayOffset,
                destArray, destArrayBaseOffset + destArrayOffset, size);
        return destArray;
    }

    @Override
    public boolean getBoolean(long address) {
        return JNRUtil.MEMORY_IO.getByte(address) != 0;
    }

    @Override
    public byte getInt8(long address) {
        return JNRUtil.MEMORY_IO.getByte(address);
    }

    @Override
    public short getInt16(long address) {
        return JNRUtil.MEMORY_IO.getShort(address);
    }

    @Override
    public char getUTF16(long address) {
        return (char) JNRUtil.MEMORY_IO.getShort(address);
    }

    @Override
    public int getInt32(long address) {
        return JNRUtil.MEMORY_IO.getInt(address);
    }

    @Override
    public long getInt64(long address) {
        return JNRUtil.MEMORY_IO.getLong(address);
    }

    @Override
    public float getFloat(long address) {
        return JNRUtil.MEMORY_IO.getFloat(address);
    }

    @Override
    public double getDouble(long address) {
        return JNRUtil.MEMORY_IO.getDouble(address);
    }

    @Override
    public void setBoolean(long address, boolean value) {
        JNRUtil.MEMORY_IO.putByte(address, (byte) (value ? 1 : 0));
    }

    @Override
    public void setInt8(long address, byte value) {
        JNRUtil.MEMORY_IO.putByte(address, value);
    }

    @Override
    public void setInt16(long address, short value) {
        JNRUtil.MEMORY_IO.putShort(address, value);
    }

    @Override
    public void setUTF16(long address, char value) {
        JNRUtil.MEMORY_IO.putShort(address, (short) value);
    }

    @Override
    public void setInt32(long address, int value) {
        JNRUtil.MEMORY_IO.putInt(address, value);
    }

    @Override
    public void setInt64(long address, long value) {
        JNRUtil.MEMORY_IO.putLong(address, value);
    }

    @Override
    public void setFloat(long address, float value) {
        JNRUtil.MEMORY_IO.putFloat(address, value);
    }

    @Override
    public void setDouble(long address, double value) {
        JNRUtil.MEMORY_IO.putDouble(address, value);
    }

    @Override
    public boolean getBoolean(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return JNRUtil.UNSAFE.getBoolean(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public byte getInt8(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return JNRUtil.UNSAFE.getByte(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public short getInt16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return JNRUtil.UNSAFE.getShort(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public char getUTF16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return JNRUtil.UNSAFE.getChar(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public int getInt32(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return JNRUtil.UNSAFE.getInt(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public long getInt64(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return JNRUtil.UNSAFE.getLong(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public float getFloat(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return JNRUtil.UNSAFE.getFloat(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public double getDouble(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return JNRUtil.UNSAFE.getDouble(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public void setBoolean(Object array, long arrayOffset, boolean value) {
        checkArray(array, arrayOffset, 1);
        JNRUtil.UNSAFE.putBoolean(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt8(Object array, long arrayOffset, byte value) {
        checkArray(array, arrayOffset, 1);
        JNRUtil.UNSAFE.putByte(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt16(Object array, long arrayOffset, short value) {
        checkArray(array, arrayOffset, 2);
        JNRUtil.UNSAFE.putShort(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setUTF16(Object array, long arrayOffset, char value) {
        checkArray(array, arrayOffset, 2);
        JNRUtil.UNSAFE.putChar(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt32(Object array, long arrayOffset, int value) {
        checkArray(array, arrayOffset, 4);
        JNRUtil.UNSAFE.putInt(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt64(Object array, long arrayOffset, long value) {
        checkArray(array, arrayOffset, 8);
        JNRUtil.UNSAFE.putLong(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setFloat(Object array, long arrayOffset, float value) {
        checkArray(array, arrayOffset, 4);
        JNRUtil.UNSAFE.putFloat(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setDouble(Object array, long arrayOffset, double value) {
        checkArray(array, arrayOffset, 8);
        JNRUtil.UNSAFE.putDouble(array, JNRUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

}
