package io.github.multiffi.ffi;

import multiffi.ffi.spi.MemoryProvider;

@SuppressWarnings({"deprecation", "removal"})
public class JNAMemoryProvider extends MemoryProvider {

    private static void checkArray(Object array, long arrayOffset, long size) {
        long arrayLength = Util.getArrayContentSize(array);
        long index = Util.unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
    }

    @Override
    public long allocate(long size) {
        return JNAAllocator.allocate(size);
    }

    @Override
    public long allocateInitialized(long count, long size) {
        return JNAAllocator.allocateInitialized(count, size);
    }

    @Override
    public long reallocate(long address, long size) {
        return JNAAllocator.reallocate(address, size);
    }

    @Override
    public void free(long address) {
        JNAAllocator.free(address);
    }

    @Override
    public long allocateAligned(long size, long alignment) {
        return JNAAlignedAllocator.allocate(size, alignment);
    }

    @Override
    public long allocateInitializedAligned(long count, long size, long alignment) {
        return JNAAlignedAllocator.allocateInitialized(count, size, alignment);
    }

    @Override
    public long reallocateAligned(long address, long size, long alignment) {
        return JNAAlignedAllocator.reallocate(address, size, alignment);
    }

    @Override
    public void freeAligned(long address) {
        JNAAlignedAllocator.free(address);
    }

    @Override
    public long search(long address, byte value, long maxLength) {
        return JNAAllocator.search(address, value, maxLength);
    }

    @Override
    public long search(Object array, long arrayOffset, byte value, long maxLength) {
        long arrayLength = Util.getArrayContentSize(array);
        if (arrayOffset < 0 || arrayOffset >= arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(arrayOffset));
        long remaining = arrayLength - arrayOffset;
        if (maxLength < 0 || maxLength > remaining) maxLength = remaining;
        long arrayBaseOffset = JNAUtil.UNSAFE.arrayBaseOffset(array.getClass());
        for (long i = 0; i < maxLength; i ++) {
            if (JNAUtil.UNSAFE.getByte(array, arrayBaseOffset + arrayOffset + i) == value) return arrayOffset + i;
        }
        return -1;
    }

    @Override
    public int compare(long aAddress, long bAddress, long size) {
        return JNAAllocator.compare(aAddress, bAddress, size);
    }

    @Override
    public int compare(long aAddress, Object bArray, long bArrayOffset, long size) {
        long arrayLength = Util.getArrayContentSize(bArray);
        long index = Util.unsignedAddExact(bArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        Util.unsignedAddExact(aAddress, size);
        long arrayBaseOffset = JNAUtil.UNSAFE.arrayBaseOffset(bArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Util.compareUnsigned(JNAUtil.UNSAFE.getByte(aAddress + i),
                    JNAUtil.UNSAFE.getByte(bArray, arrayBaseOffset + bArrayOffset + i));
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
        long arrayBaseOffset = JNAUtil.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Util.compareUnsigned(JNAUtil.UNSAFE.getByte(aArray, arrayBaseOffset + aArrayOffset + i),
                    JNAUtil.UNSAFE.getByte(bAddress + i));
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
        long aArrayBaseOffset = JNAUtil.UNSAFE.arrayBaseOffset(aArray.getClass());
        long bArrayBaseOffset = JNAUtil.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Util.compareUnsigned(JNAUtil.UNSAFE.getByte(aArray, aArrayBaseOffset + aArrayOffset + i),
                    JNAUtil.UNSAFE.getByte(bArray, bArrayBaseOffset + bArrayOffset + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public long fill(long address, byte value, long size) {
        JNAUtil.UNSAFE.setMemory(address, size, value);
        return address;
    }

    @Override
    public Object fill(Object array, long arrayOffset, byte value, long size) {
        long arrayLength = Util.getArrayContentSize(array);
        long index = Util.unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        JNAUtil.UNSAFE.setMemory(array, arrayOffset, size, value);
        return array;
    }

    @Override
    public long copy(long destAddress, long srcAddress, long size) {
        JNAUtil.UNSAFE.copyMemory(srcAddress, destAddress, size);
        return destAddress;
    }

    @Override
    public long copy(long destAddress, Object srcArray, long srcArrayOffset, long size) {
        long arrayLength = Util.getArrayContentSize(srcArray);
        long index = Util.unsignedAddExact(srcArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        Util.unsignedAddExact(destAddress, size);
        long arrayBaseOffset = JNAUtil.UNSAFE.arrayBaseOffset(srcArray.getClass());
        for (long i = 0; i < size; i ++) {
            JNAUtil.UNSAFE.putByte(destAddress + i,
                    JNAUtil.UNSAFE.getByte(srcArray, arrayBaseOffset + srcArrayOffset + i));
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
        long arrayBaseOffset = JNAUtil.UNSAFE.arrayBaseOffset(destArray.getClass());
        for (long i = 0; i < size; i ++) {
            JNAUtil.UNSAFE.putByte(destArray, arrayBaseOffset + destArrayOffset + i,
                    JNAUtil.UNSAFE.getByte(srcAddress + i));
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
        long destArrayBaseOffset = JNAUtil.UNSAFE.arrayBaseOffset(destArray.getClass());
        long srcArrayBaseOffset = JNAUtil.UNSAFE.arrayBaseOffset(srcArray.getClass());
        JNAUtil.UNSAFE.copyMemory(srcArray, srcArrayBaseOffset + srcArrayOffset,
                destArray, destArrayBaseOffset + destArrayOffset, size);
        return destArray;
    }

    @Override
    public boolean getBoolean(long address) {
        return JNAUtil.UNSAFE.getByte(address) != 0;
    }

    @Override
    public byte getInt8(long address) {
        return JNAUtil.UNSAFE.getByte(address);
    }

    @Override
    public short getInt16(long address) {
        return JNAUtil.UNSAFE.getShort(address);
    }

    @Override
    public char getUTF16(long address) {
        return JNAUtil.UNSAFE.getChar(address);
    }

    @Override
    public int getInt32(long address) {
        return JNAUtil.UNSAFE.getInt(address);
    }

    @Override
    public long getInt64(long address) {
        return JNAUtil.UNSAFE.getLong(address);
    }

    @Override
    public float getFloat(long address) {
        return JNAUtil.UNSAFE.getFloat(address);
    }

    @Override
    public double getDouble(long address) {
        return JNAUtil.UNSAFE.getDouble(address);
    }

    @Override
    public void setBoolean(long address, boolean value) {
        JNAUtil.UNSAFE.putByte(address, (byte) (value ? 1 : 0));
    }

    @Override
    public void setInt8(long address, byte value) {
        JNAUtil.UNSAFE.putByte(address, value);
    }

    @Override
    public void setInt16(long address, short value) {
        JNAUtil.UNSAFE.putShort(address, value);
    }

    @Override
    public void setUTF16(long address, char value) {
        JNAUtil.UNSAFE.putChar(address, value);
    }

    @Override
    public void setInt32(long address, int value) {
        JNAUtil.UNSAFE.putInt(address, value);
    }

    @Override
    public void setInt64(long address, long value) {
        JNAUtil.UNSAFE.putLong(address, value);
    }

    @Override
    public void setFloat(long address, float value) {
        JNAUtil.UNSAFE.putFloat(address, value);
    }

    @Override
    public void setDouble(long address, double value) {
        JNAUtil.UNSAFE.putDouble(address, value);
    }

    @Override
    public boolean getBoolean(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return JNAUtil.UNSAFE.getBoolean(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public byte getInt8(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return JNAUtil.UNSAFE.getByte(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public short getInt16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return JNAUtil.UNSAFE.getShort(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public char getUTF16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return JNAUtil.UNSAFE.getChar(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public int getInt32(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return JNAUtil.UNSAFE.getInt(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public long getInt64(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return JNAUtil.UNSAFE.getLong(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public float getFloat(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return JNAUtil.UNSAFE.getFloat(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public double getDouble(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return JNAUtil.UNSAFE.getDouble(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public void setBoolean(Object array, long arrayOffset, boolean value) {
        checkArray(array, arrayOffset, 1);
        JNAUtil.UNSAFE.putBoolean(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt8(Object array, long arrayOffset, byte value) {
        checkArray(array, arrayOffset, 1);
        JNAUtil.UNSAFE.putByte(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt16(Object array, long arrayOffset, short value) {
        checkArray(array, arrayOffset, 2);
        JNAUtil.UNSAFE.putShort(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setUTF16(Object array, long arrayOffset, char value) {
        checkArray(array, arrayOffset, 2);
        JNAUtil.UNSAFE.putChar(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt32(Object array, long arrayOffset, int value) {
        checkArray(array, arrayOffset, 4);
        JNAUtil.UNSAFE.putInt(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt64(Object array, long arrayOffset, long value) {
        checkArray(array, arrayOffset, 8);
        JNAUtil.UNSAFE.putLong(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setFloat(Object array, long arrayOffset, float value) {
        checkArray(array, arrayOffset, 4);
        JNAUtil.UNSAFE.putFloat(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setDouble(Object array, long arrayOffset, double value) {
        checkArray(array, arrayOffset, 8);
        JNAUtil.UNSAFE.putDouble(array, JNAUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

}
