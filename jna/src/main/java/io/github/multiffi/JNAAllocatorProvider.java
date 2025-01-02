package io.github.multiffi;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import multiffi.spi.AllocatorProvider;

import java.lang.reflect.Array;

public class JNAAllocatorProvider extends AllocatorProvider {

    private static native Pointer calloc(SizeT count, SizeT size);
    private static native Pointer realloc(Pointer address, SizeT size);
    private static native Pointer memchr(Pointer address, int value, SizeT maxLength);
    private static native int memcmp(Pointer aAddress, Pointer bAddress, SizeT size);
    
    static {
        Native.register(JNAAllocatorProvider.class, Platform.C_LIBRARY_NAME);
    }

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
        else return (long) Array.getLength(array) * JNAUtil.UnsafeHolder.UNSAFE.arrayIndexScale(array.getClass());
    }

    private static void checkArray(Object array, long arrayOffset, long size) {
        long arrayLength = getArrayContentSize(array);
        long index = unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
    }

    @Override
    public long allocate(long size) {
        return Native.malloc(size);
    }

    @Override
    public long allocateInitialized(long count, long size) {
        return Pointer.nativeValue(calloc(new SizeT(count), new SizeT(size)));
    }

    @Override
    public long reallocate(long address, long size) {
        return Pointer.nativeValue(realloc(new Pointer(address), new SizeT(size)));
    }

    @Override
    public void free(long address) {
        Native.free(address);
    }

    @Override
    public long search(long address, byte value, long maxLength) {
        return Pointer.nativeValue(memchr(new Pointer(address), value & 0xFF, new SizeT(maxLength)));
    }

    @Override
    public long search(Object array, long arrayOffset, byte value, long maxLength) {
        long arrayLength = getArrayContentSize(array);
        if (arrayOffset < 0 || arrayOffset >= arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(arrayOffset));
        long remaining = arrayLength - arrayOffset;
        if (maxLength < 0 || maxLength > remaining) maxLength = remaining;
        long arrayBaseOffset = JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass());
        for (long i = 0; i < maxLength; i ++) {
            if (JNAUtil.UnsafeHolder.UNSAFE.getByte(array, arrayBaseOffset + arrayOffset + i) == value) return arrayOffset + i;
        }
        return -1;
    }

    @Override
    public int compare(long aAddress, long bAddress, long size) {
        return memcmp(new Pointer(aAddress), new Pointer(bAddress), new SizeT(size));
    }

    @Override
    public int compare(long aAddress, Object bArray, long bArrayOffset, long size) {
        long arrayLength = getArrayContentSize(bArray);
        long index = unsignedAddExact(bArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        unsignedAddExact(aAddress, size);
        long arrayBaseOffset = JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(bArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = compareUnsigned(JNAUtil.UnsafeHolder.UNSAFE.getByte(aAddress + i),
                    JNAUtil.UnsafeHolder.UNSAFE.getByte(bArray, arrayBaseOffset + bArrayOffset + i));
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
        long arrayBaseOffset = JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = compareUnsigned(JNAUtil.UnsafeHolder.UNSAFE.getByte(aArray, arrayBaseOffset + aArrayOffset + i),
                    JNAUtil.UnsafeHolder.UNSAFE.getByte(bAddress + i));
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
        long aArrayBaseOffset = JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(aArray.getClass());
        long bArrayBaseOffset = JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(aArray.getClass());
        for (long i = 0; i < size; i ++) {
            int cmp = compareUnsigned(JNAUtil.UnsafeHolder.UNSAFE.getByte(aArray, aArrayBaseOffset + aArrayOffset + i),
                    JNAUtil.UnsafeHolder.UNSAFE.getByte(bArrayBaseOffset + bArrayOffset + i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public long fill(long address, byte value, long size) {
        JNAUtil.UnsafeHolder.UNSAFE.setMemory(address, size, value);
        return address;
    }

    @Override
    public Object fill(Object array, long arrayOffset, byte value, long size) {
        long arrayLength = getArrayContentSize(array);
        long index = unsignedAddExact(arrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        JNAUtil.UnsafeHolder.UNSAFE.setMemory(array, arrayOffset, size, value);
        return array;
    }

    @Override
    public long copy(long destAddress, long srcAddress, long size) {
        JNAUtil.UnsafeHolder.UNSAFE.copyMemory(srcAddress, destAddress, size);
        return destAddress;
    }

    @Override
    public long copy(long destAddress, Object srcArray, long srcArrayOffset, long size) {
        long arrayLength = getArrayContentSize(srcArray);
        long index = unsignedAddExact(srcArrayOffset, size);
        if (index < 0 || index > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        unsignedAddExact(destAddress, size);
        long arrayBaseOffset = JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(srcArray.getClass());
        for (long i = 0; i < size; i ++) {
            JNAUtil.UnsafeHolder.UNSAFE.putByte(destAddress + i,
                    JNAUtil.UnsafeHolder.UNSAFE.getByte(srcArray, arrayBaseOffset + srcArrayOffset + i));
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
        long arrayBaseOffset = JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(destArray.getClass());
        for (long i = 0; i < size; i ++) {
            JNAUtil.UnsafeHolder.UNSAFE.putByte(destArray, arrayBaseOffset + destArrayOffset + i,
                    JNAUtil.UnsafeHolder.UNSAFE.getByte(srcAddress + i));
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
        long destArrayBaseOffset = JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(destArray.getClass());
        long srcArrayBaseOffset = JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(srcArray.getClass());
        JNAUtil.UnsafeHolder.UNSAFE.copyMemory(srcArray, srcArrayBaseOffset + srcArrayOffset,
                destArray, destArrayBaseOffset + destArrayOffset, size);
        return destArray;
    }

    @Override
    public boolean getBoolean(long address) {
        return JNAUtil.UnsafeHolder.UNSAFE.getByte(address) != 0;
    }

    @Override
    public byte getInt8(long address) {
        return JNAUtil.UnsafeHolder.UNSAFE.getByte(address);
    }

    @Override
    public short getInt16(long address) {
        return JNAUtil.UnsafeHolder.UNSAFE.getShort(address);
    }

    @Override
    public char getUTF16(long address) {
        return JNAUtil.UnsafeHolder.UNSAFE.getChar(address);
    }

    @Override
    public int getInt32(long address) {
        return JNAUtil.UnsafeHolder.UNSAFE.getInt(address);
    }

    @Override
    public long getInt64(long address) {
        return JNAUtil.UnsafeHolder.UNSAFE.getLong(address);
    }

    @Override
    public float getFloat(long address) {
        return JNAUtil.UnsafeHolder.UNSAFE.getFloat(address);
    }

    @Override
    public double getDouble(long address) {
        return JNAUtil.UnsafeHolder.UNSAFE.getDouble(address);
    }

    @Override
    public void setBoolean(long address, boolean value) {
        JNAUtil.UnsafeHolder.UNSAFE.putByte(address, (byte) (value ? 1 : 0));
    }

    @Override
    public void setInt8(long address, byte value) {
        JNAUtil.UnsafeHolder.UNSAFE.putByte(address, value);
    }

    @Override
    public void setInt16(long address, short value) {
        JNAUtil.UnsafeHolder.UNSAFE.putShort(address, value);
    }

    @Override
    public void setUTF16(long address, char value) {
        JNAUtil.UnsafeHolder.UNSAFE.putChar(address, value);
    }

    @Override
    public void setInt32(long address, int value) {
        JNAUtil.UnsafeHolder.UNSAFE.putInt(address, value);
    }

    @Override
    public void setInt64(long address, long value) {
        JNAUtil.UnsafeHolder.UNSAFE.putLong(address, value);
    }

    @Override
    public void setFloat(long address, float value) {
        JNAUtil.UnsafeHolder.UNSAFE.putFloat(address, value);
    }

    @Override
    public void setDouble(long address, double value) {
        JNAUtil.UnsafeHolder.UNSAFE.putDouble(address, value);
    }

    @Override
    public boolean getBoolean(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return JNAUtil.UnsafeHolder.UNSAFE.getBoolean(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public byte getInt8(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 1);
        return JNAUtil.UnsafeHolder.UNSAFE.getByte(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public short getInt16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return JNAUtil.UnsafeHolder.UNSAFE.getShort(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public char getUTF16(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 2);
        return JNAUtil.UnsafeHolder.UNSAFE.getChar(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public int getInt32(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return JNAUtil.UnsafeHolder.UNSAFE.getInt(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public long getInt64(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return JNAUtil.UnsafeHolder.UNSAFE.getLong(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public float getFloat(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 4);
        return JNAUtil.UnsafeHolder.UNSAFE.getFloat(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public double getDouble(Object array, long arrayOffset) {
        checkArray(array, arrayOffset, 8);
        return JNAUtil.UnsafeHolder.UNSAFE.getDouble(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset);
    }

    @Override
    public void setBoolean(Object array, long arrayOffset, boolean value) {
        checkArray(array, arrayOffset, 1);
        JNAUtil.UnsafeHolder.UNSAFE.putBoolean(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt8(Object array, long arrayOffset, byte value) {
        checkArray(array, arrayOffset, 1);
        JNAUtil.UnsafeHolder.UNSAFE.putByte(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt16(Object array, long arrayOffset, short value) {
        checkArray(array, arrayOffset, 2);
        JNAUtil.UnsafeHolder.UNSAFE.putShort(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setUTF16(Object array, long arrayOffset, char value) {
        checkArray(array, arrayOffset, 2);
        JNAUtil.UnsafeHolder.UNSAFE.putChar(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt32(Object array, long arrayOffset, int value) {
        checkArray(array, arrayOffset, 4);
        JNAUtil.UnsafeHolder.UNSAFE.putInt(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setInt64(Object array, long arrayOffset, long value) {
        checkArray(array, arrayOffset, 8);
        JNAUtil.UnsafeHolder.UNSAFE.putLong(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setFloat(Object array, long arrayOffset, float value) {
        checkArray(array, arrayOffset, 4);
        JNAUtil.UnsafeHolder.UNSAFE.putFloat(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

    @Override
    public void setDouble(Object array, long arrayOffset, double value) {
        checkArray(array, arrayOffset, 8);
        JNAUtil.UnsafeHolder.UNSAFE.putDouble(array, JNAUtil.UnsafeHolder.UNSAFE.arrayBaseOffset(array.getClass()) + arrayOffset, value);
    }

}
