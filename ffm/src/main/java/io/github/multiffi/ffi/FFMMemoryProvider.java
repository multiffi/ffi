package io.github.multiffi.ffi;

import multiffi.ffi.spi.MemoryProvider;

import java.lang.foreign.MemorySegment;

@SuppressWarnings({"deprecation", "removal"})
public class FFMMemoryProvider extends MemoryProvider {

    private static void checkArray(Object array, long arrayOffset, long size) {
        long arrayLength = Util.getArrayContentSize(array);
        long index = Util.unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
    }

    @Override
    public long allocate(long size) {
        return FFMAllocator.allocate(size);
    }

    @Override
    public long allocateInitialized(long count, long size) {
        return FFMAllocator.allocateInitialized(count, size);
    }

    @Override
    public long reallocate(long address, long size) {
        return FFMAllocator.reallocate(address, size);
    }

    @Override
    public void free(long address) {
        FFMAllocator.free(address);
    }

    @Override
    public long allocateAligned(long size, long alignment) {
        return FFMAlignedAllocator.allocate(size, alignment);
    }

    @Override
    public long allocateInitializedAligned(long count, long size, long alignment) {
        return FFMAlignedAllocator.allocateInitialized(count, size, alignment);
    }

    @Override
    public long reallocateAligned(long address, long size, long alignment) {
        return FFMAlignedAllocator.reallocate(address, size, alignment);
    }

    @Override
    public void freeAligned(long address) {
        FFMAlignedAllocator.free(address);
    }

    @Override
    public long search(long address, byte value, long maxLength) {
        return FFMAllocator.search(address, value, maxLength);
    }

    @Override
    public long search(Object array, long arrayOffset, byte value, long maxLength) {
        long arrayLength = Util.getArrayContentSize(array);
        if (arrayOffset < 0 || arrayOffset >= arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(arrayOffset));
        long remaining = arrayLength - arrayOffset;
        if (maxLength < 0 || maxLength > remaining) maxLength = remaining;
        long arrayBaseOffset = FFMUtil.UNSAFE.arrayBaseOffset(array.getClass());
        for (long i = 0; i < maxLength; i ++) {
            if (FFMUtil.UNSAFE.getByte(array, arrayBaseOffset + arrayOffset + i) == value) return arrayOffset + i;
        }
        return -1;
    }

    @Override
    public int compare(long aAddress, long bAddress, long size) {
        return FFMAllocator.compare(aAddress, bAddress, size);
    }

    @Override
    public int compare(long aAddress, Object bArray, long bArrayOffset, long size) {
        long arrayLength = Util.getArrayContentSize(bArray);
        long index = Util.unsignedAddExact(bArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        Util.unsignedAddExact(aAddress, size);
        long arrayBaseOffset = FFMUtil.UNSAFE.arrayBaseOffset(bArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Byte.compareUnsigned(FFMUtil.UNSAFE.getByte(aAddress + i), 
                    FFMUtil.UNSAFE.getByte(bArray, arrayBaseOffset + bArrayOffset + i));
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
        long arrayBaseOffset = FFMUtil.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Byte.compareUnsigned(FFMUtil.UNSAFE.getByte(aArray, arrayBaseOffset + aArrayOffset + i),
                    FFMUtil.UNSAFE.getByte(bAddress + i));
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
        long aArrayBaseOffset = FFMUtil.UNSAFE.arrayBaseOffset(aArray.getClass());
        long bArrayBaseOffset = FFMUtil.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = Byte.compareUnsigned(FFMUtil.UNSAFE.getByte(aArray, aArrayBaseOffset + aArrayOffset + i),
                    FFMUtil.UNSAFE.getByte(bArray, bArrayBaseOffset + bArrayOffset + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public long fill(long address, byte value, long size) {
        FFMUtil.UNSAFE.setMemory(address, size, value);
        return address;
    }

    @Override
    public Object fill(Object array, long arrayOffset, byte value, long size) {
        long arrayLength = Util.getArrayContentSize(array);
        long index = Util.unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        FFMUtil.UNSAFE.setMemory(array, arrayOffset, size, value);
        return array;
    }

    @Override
    public long copy(long destAddress, long srcAddress, long size) {
        FFMUtil.UNSAFE.copyMemory(srcAddress, destAddress, size);
        return destAddress;
    }

    @Override
    public long copy(long destAddress, Object srcArray, long srcArrayOffset, long size) {
        long arrayLength = Util.getArrayContentSize(srcArray);
        long index = Util.unsignedAddExact(srcArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        Util.unsignedAddExact(destAddress, size);
        MemorySegment memorySegment;
        if (srcArray instanceof byte[]) memorySegment = MemorySegment.ofArray((byte[]) srcArray);
        else if (srcArray instanceof char[]) memorySegment = MemorySegment.ofArray((char[]) srcArray);
        else if (srcArray instanceof short[]) memorySegment = MemorySegment.ofArray((short[]) srcArray);
        else if (srcArray instanceof int[]) memorySegment = MemorySegment.ofArray((int[]) srcArray);
        else if (srcArray instanceof long[]) memorySegment = MemorySegment.ofArray((long[]) srcArray);
        else if (srcArray instanceof float[]) memorySegment = MemorySegment.ofArray((float[]) srcArray);
        else if (srcArray instanceof double[]) memorySegment = MemorySegment.ofArray((double[]) srcArray);
        else {
            long arrayBaseOffset = FFMUtil.UNSAFE.arrayBaseOffset(srcArray.getClass());
            for (long i = 0; i < size; i ++) {
                FFMUtil.UNSAFE.putByte(destAddress + i,
                        FFMUtil.UNSAFE.getByte(srcArray, arrayBaseOffset + srcArrayOffset + i));
            }
            return destAddress;
        }
        MemorySegment.ofAddress(destAddress).reinterpret(size).copyFrom(memorySegment.asSlice(srcArrayOffset));
        return destAddress;
    }

    @Override
    public Object copy(Object destArray, long destArrayOffset, long srcAddress, long size) {
        long arrayLength = Util.getArrayContentSize(destArray);
        long index = Util.unsignedAddExact(destArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        Util.unsignedAddExact(srcAddress, size);
        MemorySegment memorySegment;
        if (destArray instanceof byte[]) memorySegment = MemorySegment.ofArray((byte[]) destArray);
        else if (destArray instanceof char[]) memorySegment = MemorySegment.ofArray((char[]) destArray);
        else if (destArray instanceof short[]) memorySegment = MemorySegment.ofArray((short[]) destArray);
        else if (destArray instanceof int[]) memorySegment = MemorySegment.ofArray((int[]) destArray);
        else if (destArray instanceof long[]) memorySegment = MemorySegment.ofArray((long[]) destArray);
        else if (destArray instanceof float[]) memorySegment = MemorySegment.ofArray((float[]) destArray);
        else if (destArray instanceof double[]) memorySegment = MemorySegment.ofArray((double[]) destArray);
        else {
            long arrayBaseOffset = FFMUtil.UNSAFE.arrayBaseOffset(destArray.getClass());
            for (long i = 0; i < size; i ++) {
                FFMUtil.UNSAFE.putByte(destArray, arrayBaseOffset + destArrayOffset + i,
                        FFMUtil.UNSAFE.getByte(srcAddress + i));
            }
            return destArray;
        }
        memorySegment.asSlice(destArrayOffset).copyFrom(MemorySegment.ofAddress(srcAddress).reinterpret(size));
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
        long destArrayBaseOffset = FFMUtil.UNSAFE.arrayBaseOffset(destArray.getClass());
        long srcArrayBaseOffset = FFMUtil.UNSAFE.arrayBaseOffset(srcArray.getClass());
        FFMUtil.UNSAFE.copyMemory(srcArray, srcArrayBaseOffset + srcArrayOffset,
                destArray, destArrayBaseOffset + destArrayOffset, size);
        return destArray;
    }

    @Override
    public boolean getBoolean(long address) {
        return FFMUtil.UNSAFE.getByte(address) != 0;
    }

    @Override
    public byte getInt8(long address) {
        return FFMUtil.UNSAFE.getByte(address);
    }

    @Override
    public short getInt16(long address) {
        return FFMUtil.UNSAFE.getShort(address);
    }

    @Override
    public char getUTF16(long address) {
        return FFMUtil.UNSAFE.getChar(address);
    }

    @Override
    public int getInt32(long address) {
        return FFMUtil.UNSAFE.getInt(address);
    }

    @Override
    public long getInt64(long address) {
        return FFMUtil.UNSAFE.getLong(address);
    }

    @Override
    public float getFloat(long address) {
        return FFMUtil.UNSAFE.getFloat(address);
    }

    @Override
    public double getDouble(long address) {
        return FFMUtil.UNSAFE.getDouble(address);
    }

    @Override
    public void setBoolean(long address, boolean value) {
        FFMUtil.UNSAFE.putByte(address, (byte) (value ? 1 : 0));
    }

    @Override
    public void setInt8(long address, byte value) {
        FFMUtil.UNSAFE.putByte(address, value);
    }

    @Override
    public void setInt16(long address, short value) {
        FFMUtil.UNSAFE.putShort(address, value);
    }

    @Override
    public void setUTF16(long address, char value) {
        FFMUtil.UNSAFE.putChar(address, value);
    }

    @Override
    public void setInt32(long address, int value) {
        FFMUtil.UNSAFE.putInt(address, value);
    }

    @Override
    public void setInt64(long address, long value) {
        FFMUtil.UNSAFE.putLong(address, value);
    }

    @Override
    public void setFloat(long address, float value) {
        FFMUtil.UNSAFE.putFloat(address, value);
    }

    @Override
    public void setDouble(long address, double value) {
        FFMUtil.UNSAFE.putDouble(address, value);
    }

    @Override
    public boolean getBoolean(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return FFMUtil.UNSAFE.getBoolean(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public byte getInt8(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return FFMUtil.UNSAFE.getByte(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public short getInt16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return FFMUtil.UNSAFE.getShort(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public char getUTF16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return FFMUtil.UNSAFE.getChar(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public int getInt32(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return FFMUtil.UNSAFE.getInt(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public long getInt64(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return FFMUtil.UNSAFE.getLong(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public float getFloat(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return FFMUtil.UNSAFE.getFloat(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public double getDouble(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return FFMUtil.UNSAFE.getDouble(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public void setBoolean(Object array, long arrayOffset, boolean value) {
        checkArray(array, arrayOffset, 1);
        FFMUtil.UNSAFE.putBoolean(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt8(Object array, long arrayOffset, byte value) {
        checkArray(array, arrayOffset, 1);
        FFMUtil.UNSAFE.putByte(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt16(Object array, long arrayOffset, short value) {
        checkArray(array, arrayOffset, 2);
        FFMUtil.UNSAFE.putShort(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setUTF16(Object array, long arrayOffset, char value) {
        checkArray(array, arrayOffset, 2);
        FFMUtil.UNSAFE.putChar(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt32(Object array, long arrayOffset, int value) {
        checkArray(array, arrayOffset, 4);
        FFMUtil.UNSAFE.putInt(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt64(Object array, long arrayOffset, long value) {
        checkArray(array, arrayOffset, 8);
        FFMUtil.UNSAFE.putLong(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setFloat(Object array, long arrayOffset, float value) {
        checkArray(array, arrayOffset, 4);
        FFMUtil.UNSAFE.putFloat(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setDouble(Object array, long arrayOffset, double value) {
        checkArray(array, arrayOffset, 8);
        FFMUtil.UNSAFE.putDouble(array, FFMUtil.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

}
