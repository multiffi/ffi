package multiffi.ffi.spi;

import multiffi.ffi.Foreign;
import multiffi.ffi.Limits;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.ServiceLoader;

public abstract class AllocatorProvider {

    private static volatile AllocatorProvider IMPLEMENTATION;
    private static final Object IMPLEMENTATION_LOCK = new Object();
    public static AllocatorProvider getImplementation() {
        if (IMPLEMENTATION == null) synchronized (IMPLEMENTATION_LOCK) {
            if (IMPLEMENTATION == null) {
                try {
                    IMPLEMENTATION = (AllocatorProvider) Class
                            .forName(Objects.requireNonNull(System.getProperty("multiffi.allocator.provider")))
                            .getDeclaredConstructor()
                            .newInstance();
                } catch (Throwable e) {
                    try {
                        for (AllocatorProvider provider : ServiceLoader.load(AllocatorProvider.class)) {
                            if (provider != null) {
                                IMPLEMENTATION = provider;
                                break;
                            }
                        }
                    }
                    catch (Throwable ex) {
                        IMPLEMENTATION = null;
                    }
                }
                if (IMPLEMENTATION == null) throw new IllegalStateException("Failed to get any installed multiffi.ffi.spi.AllocatorProvider instance");
            }
        }
        return IMPLEMENTATION;
    }

    public abstract long allocate(long size);
    public abstract long allocateInitialized(long count, long size);
    public abstract long reallocate(long address, long size);
    public abstract void free(long address);

    public abstract long search(long address, byte value, long maxLength);
    public long search(long address, byte value) {
        return search(address, value, Limits.ADDRESS_MAX);
    }
    public long search(long address, int value, long maxLength) {
        return search(address, (byte) value, maxLength);
    }
    public long search(long address, int value) {
        return search(address, value, Limits.ADDRESS_MAX);
    }
    public abstract long search(Object array, long arrayOffset, byte value, long maxLength);
    public long search(Object array, long arrayOffset, byte value) {
        return search(array, arrayOffset, value, Limits.ADDRESS_MAX);
    }
    public long search(Object array, long arrayOffset, int value, long maxLength) {
        return search(array, arrayOffset, (byte) value, maxLength);
    }
    public long search(Object array, long arrayOffset, int value) {
        return search(array, arrayOffset, value, Limits.ADDRESS_MAX);
    }
    public long search(long address, long valueAddress, long valueSize, long maxLength) {
        if (maxLength < 0) {
            for (long i = 0; i < Integer.MAX_VALUE; i ++) {
                long index = address + i;
                if (compare(index, valueAddress, valueSize) == 0) return index;
            }
            for (long i = Integer.MIN_VALUE; i < maxLength; i ++) {
                long index = address + i;
                if (compare(index, valueAddress, valueSize) == 0) return index;
            }
        }
        for (long i = 0; i < maxLength; i ++) {
            long index = address + i;
            if (compare(index, valueAddress, valueSize) == 0) return index;
        }
        return 0;
    }
    public long search(long address, long valueAddress, long valueSize) {
        return search(address, valueAddress, valueSize, Limits.ADDRESS_MAX);
    }
    public long search(long address, Object valueArray, int valueArrayOffset, int valueSize, long maxLength) {
        if (maxLength < 0) {
            for (long i = 0; i < Integer.MAX_VALUE; i ++) {
                long index = address + i;
                if (compare(index, valueArray, valueArrayOffset, valueSize) == 0) return index;
            }
            for (long i = Integer.MIN_VALUE; i < maxLength; i ++) {
                long index = address + i;
                if (compare(index, valueArray, valueArrayOffset, valueSize) == 0) return index;
            }
        }
        for (long i = 0; i < maxLength; i ++) {
            long index = address + i;
            if (compare(index, valueArray, valueArrayOffset, valueSize) == 0) return index;
        }
        return 0;
    }
    public long search(long address, Object valueArray, int valueArrayOffset, int valueSize) {
        return search(address, valueArray, valueArrayOffset, valueSize, Limits.ADDRESS_MAX);
    }
    public long search(Object array, long arrayOffset, long valueAddress, int valueSize, long maxLength) {
        if (maxLength < 0) {
            for (long i = 0; i < Integer.MAX_VALUE; i ++) {
                long index = arrayOffset + i;
                if (compare(array, index, valueAddress, valueSize) == 0) return index;
            }
            for (long i = Integer.MIN_VALUE; i < maxLength; i ++) {
                long index = arrayOffset + i;
                if (compare(array, index, valueAddress, valueSize) == 0) return index;
            }
        }
        for (long i = 0; i < maxLength; i ++) {
            long index = arrayOffset + i;
            if (compare(array, index, valueAddress, valueSize) == 0) return index;
        }
        return 0;
    }
    public long search(Object array, long arrayOffset, long valueAddress, int valueSize) {
        return search(array, arrayOffset, valueAddress, valueSize, Limits.ADDRESS_MAX);
    }
    public long search(Object array, long arrayOffset, Object valueArray, int valueArrayOffset, int valueSize, long maxLength) {
        if (maxLength < 0) {
            for (long i = 0; i < Integer.MAX_VALUE; i ++) {
                long index = arrayOffset + i;
                if (compare(array, index, valueArray, valueArrayOffset, valueSize) == 0) return index;
            }
            for (long i = Integer.MIN_VALUE; i < maxLength; i ++) {
                long index = arrayOffset + i;
                if (compare(array, index, valueArray, valueArrayOffset, valueSize) == 0) return index;
            }
        }
        for (long i = 0; i < maxLength; i ++) {
            long index = arrayOffset + i;
            if (compare(array, index, valueArray, valueArrayOffset, valueSize) == 0) return index;
        }
        return 0;
    }
    public long search(Object array, long arrayOffset, Object valueArray, int valueArrayOffset, int valueSize) {
        return search(array, arrayOffset, valueArray, valueArrayOffset, valueSize, Limits.ADDRESS_MAX);
    }
    public abstract int compare(long aAddress, long bAddress, long size);
    public abstract int compare(long aAddress, Object bArray, long bArrayOffset, long size);
    public abstract int compare(Object aArray, long aArrayOffset, long bAddress, long size);
    public abstract int compare(Object aArray, long aArrayOffset, Object bArray, long bArrayOffset, long size);
    public abstract long fill(long address, byte value, long size);
    public long fill(long address, int value, long size) {
        return fill(address, (byte) value, size);
    }
    public abstract Object fill(Object array, long arrayOffset, byte value, long size);
    public Object fill(Object array, long arrayOffset, int value, long size) {
        return fill(array, arrayOffset, (byte) value, size);
    }
    public abstract long copy(long destAddress, long srcAddress, long size);
    public abstract long copy(long destAddress, Object srcArray, long srcArrayOffset, long size);
    public abstract Object copy(Object destArray, long destArrayOffset, long srcAddress, long size);
    public abstract Object copy(Object destArray, long destArrayOffset, Object srcArray, long srcArrayOffset, long size);

    public abstract boolean getBoolean(long address);
    public abstract byte getInt8(long address);
    public abstract short getInt16(long address);
    public abstract char getUTF16(long address);
    public abstract int getInt32(long address);
    public abstract long getInt64(long address);
    public byte getChar(long address) {
        return getInt8(address);
    }
    public long getWChar(long address) {
        return Int32Adapter.WCHAR.get(this, address);
    }
    public long getShort(long address) {
        return Int64Adapter.SHORT.get(this, address);
    }
    public long getInt(long address) {
        return Int64Adapter.INT.get(this, address);
    }
    public long getLong(long address) {
        return Int64Adapter.LONG.get(this, address);
    }
    public long getAddress(long address) {
        return Int64Adapter.ADDRESS.get(this, address);
    }
    public abstract float getFloat(long address);
    public abstract double getDouble(long address);
    public abstract void setBoolean(long address, boolean value);
    public abstract void setInt8(long address, byte value);
    public void setInt8(long address, int value) {
        setInt8(address, (byte) value);
    }
    public abstract void setInt16(long address, short value);
    public void setInt16(long address, int value) {
        setInt16(address, (short) value);
    }
    public abstract void setUTF16(long address, char value);
    public abstract void setInt32(long address, int value);
    public abstract void setInt64(long address, long value);
    public void setChar(long address, byte value) {
        setInt8(address, value);
    }
    public void setChar(long address, int value) {
        setInt8(address, value);
    }
    public void setWChar(long address, int value) {
        Int32Adapter.WCHAR.set(this, address, value);
    }
    public void setShort(long address, long value) {
        Int64Adapter.SHORT.set(this, address, value);
    }
    public void setInt(long address, long value) {
        Int64Adapter.INT.set(this, address, value);
    }
    public void setLong(long address, long value) {
        Int64Adapter.LONG.set(this, address, value);
    }
    public void setAddress(long address, long value) {
        Int64Adapter.ADDRESS.set(this, address, value);
    }
    public abstract void setFloat(long address, float value);
    public abstract void setDouble(long address, double value);

    public void getBooleanArray(long address, boolean[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getBoolean(address + i);
        }
    }
    public void getBooleanArray(long address, boolean[] array) {
        getBooleanArray(address, array, 0, array.length);
    }
    public void setBooleanArray(long address, boolean[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setBoolean(address + i, array[index + i]);
        }
    }
    public void setBooleanArray(long address, boolean[] array) {
        setBooleanArray(address, array, 0, array.length);
    }
    public void getInt8Array(long address, byte[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt8(address + i);
        }
    }
    public void getInt8Array(long address, byte[] array) {
        getInt8Array(address, array, 0, array.length);
    }
    public void setInt8Array(long address, byte[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setInt8(address + i, array[index + i]);
        }
    }
    public void setInt8Array(long address, byte[] array) {
        setInt8Array(address, array, 0, array.length);
    }
    public void getInt16Array(long address, short[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt16(address + (long) i << 1);
        }
    }
    public void getInt16Array(long address, short[] array) {
        getInt16Array(address, array, 0, array.length);
    }
    public void setInt16Array(long address, short[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setInt16(address + (long) i << 1, array[index + i]);
        }
    }
    public void setInt16Array(long address, short[] array) {
        setInt16Array(address, array, 0, array.length);
    }
    public void getUTF16Array(long address, char[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getUTF16(address + (long) i << 1);
        }
    }
    public void getUTF16Array(long address, char[] array) {
        getUTF16Array(address, array, 0, array.length);
    }
    public void setUTF16Array(long address, char[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setUTF16(address + (long) i << 1, array[index + i]);
        }
    }
    public void setUTF16Array(long address, char[] array) {
        setUTF16Array(address, array, 0, array.length);
    }
    public void getInt32Array(long address, int[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt32(address + (long) i << 2);
        }
    }
    public void getInt32Array(long address, int[] array) {
        getInt32Array(address, array, 0, array.length);
    }
    public void setInt32Array(long address, int[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setInt32(address + (long) i << 2, array[index + i]);
        }
    }
    public void setInt32Array(long address, int[] array) {
        setInt32Array(address, array, 0, array.length);
    }
    public void getInt64Array(long address, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt64(address + (long) i << 3);
        }
    }
    public void getInt64Array(long address, long[] array) {
        getInt64Array(address, array, 0, array.length);
    }
    public void setInt64Array(long address, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setInt64(address + (long) i << 3, array[index + i]);
        }
    }
    public void setInt64Array(long address, long[] array) {
        setInt64Array(address, array, 0, array.length);
    }
    public void getFloatArray(long address, float[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getFloat(address + (long) i << 2);
        }
    }
    public void getFloatArray(long address, float[] array) {
        getFloatArray(address, array, 0, array.length);
    }
    public void setFloatArray(long address, float[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setFloat(address + (long) i << 2, array[index + i]);
        }
    }
    public void setFloatArray(long address, float[] array) {
        setFloatArray(address, array, 0, array.length);
    }
    public void getDoubleArray(long address, double[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getDouble(address + (long) i << 3);
        }
    }
    public void getDoubleArray(long address, double[] array) {
        getDoubleArray(address, array, 0, array.length);
    }
    public void setDoubleArray(long address, double[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setDouble(address + (long) i << 3, array[index + i]);
        }
    }
    public void setDoubleArray(long address, double[] array) {
        setDoubleArray(address, array, 0, array.length);
    }
    public void getCharArray(long address, byte[] array, int index, int length) {
        getInt8Array(address, array, index, length);
    }
    public void getCharArray(long address, byte[] array) {
        getInt8Array(address, array);
    }
    public void setCharArray(long address, byte[] array, int index, int length) {
        setInt8Array(address, array, index, length);
    }
    public void setCharArray(long address, byte[] array) {
        setInt8Array(address, array);
    }
    public void getWCharArray(long address, int[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = Int32Adapter.WCHAR.get(this, address + (long) i * Foreign.wcharSize());
        }
    }
    public void getWCharArray(long address, int[] array) {
        getWCharArray(address, array, 0, array.length);
    }
    public void setWCharArray(long address, int[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int32Adapter.WCHAR.set(this, address + (long) i * Foreign.wcharSize(), array[index + i]);
        }
    }
    public void setWCharArray(long address, int[] array) {
        setWCharArray(address, array, 0, array.length);
    }
    public void getShortArray(long address, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = Int64Adapter.SHORT.get(this, address + (long) i * Foreign.shortSize());
        }
    }
    public void getShortArray(long address, long[] array) {
        getShortArray(address, array, 0, array.length);
    }
    public void setShortArray(long address, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int64Adapter.SHORT.set(this, address + (long) i * Foreign.shortSize(), array[index + i]);
        }
    }
    public void setShortArray(long address, long[] array) {
        setShortArray(address, array, 0, array.length);
    }
    public void getIntArray(long address, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = Int64Adapter.INT.get(this, address + (long) i * Foreign.intSize());
        }
    }
    public void getIntArray(long address, long[] array) {
        getIntArray(address, array, 0, array.length);
    }
    public void setIntArray(long address, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int64Adapter.INT.set(this, address + (long) i * Foreign.intSize(), array[index + i]);
        }
    }
    public void setIntArray(long address, long[] array) {
        setIntArray(address, array, 0, array.length);
    }
    public void getLongArray(long address, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int64Adapter.LONG.set(this, address + (long) i * Foreign.longSize(), array[index + i]);
        }
    }
    public void getLongArray(long address, long[] array) {
        getLongArray(address, array, 0, array.length);
    }
    public void setLongArray(long address, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = Int64Adapter.LONG.get(this, address + (long) i * Foreign.longSize());
        }
    }
    public void setLongArray(long address, long[] array) {
        setLongArray(address, array, 0, array.length);
    }
    public void getAddressArray(long address, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int64Adapter.ADDRESS.set(this, address + (long) i * Foreign.addressSize(), array[index + i]);
        }
    }
    public void getAddressArray(long address, long[] array) {
        getAddressArray(address, array, 0, array.length);
    }
    public void setAddressArray(long address, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int64Adapter.ADDRESS.set(this, address + (long) i * Foreign.addressSize(), array[index + i]);
        }
    }
    public void setAddressArray(long address, long[] array) {
        setAddressArray(address, array, 0, array.length);
    }
    public long getZeroTerminatedStringLength(long address) {
        return getZeroTerminatedStringLength(address, Limits.ADDRESS_MAX);
    }
    public long getZeroTerminatedStringLength(long address, long maxLength) {
        return getZeroTerminatedStringLength(address, maxLength, null);
    }
    public long getZeroTerminatedWStringLength(long address) {
        return getZeroTerminatedWStringLength(address, Limits.ADDRESS_MAX);
    }
    public long getZeroTerminatedWStringLength(long address, long maxLength) {
        return getZeroTerminatedStringLength(address, maxLength, Foreign.wideCharset());
    }
    public long getZeroTerminatedStringLength(long address, Charset charset) {
        return getZeroTerminatedStringLength(address, Limits.ADDRESS_MAX, charset);
    }
    public long getZeroTerminatedStringLength(long address, long maxLength, Charset charset) {
        byte[] terminator = "\0".getBytes(charset == null ? Foreign.ansiCharset() : charset);
        return search(address, terminator, 0, terminator.length, maxLength) - address;
    }
    public long getZeroTerminatedUTF16StringLength(long address) {
        return getZeroTerminatedStringLength(address, Limits.ADDRESS_MAX);
    }
    public long getZeroTerminatedUTF16StringLength(long address, long maxLength) {
        return getZeroTerminatedStringLength(address, maxLength, Foreign.utf16Charset());
    }
    public byte[] getZeroTerminatedCharArray(long address) {
        return getZeroTerminatedCharArray(address, Integer.MAX_VALUE - 8);
    }
    public byte[] getZeroTerminatedCharArray(long address, int maxLength) {
        return getZeroTerminatedCharArray(address, maxLength, null);
    }
    public byte[] getZeroTerminatedWCharArray(long address) {
        return getZeroTerminatedWCharArray(address, Integer.MAX_VALUE - 8);
    }
    public byte[] getZeroTerminatedWCharArray(long address, int maxLength) {
        return getZeroTerminatedCharArray(address, maxLength, Foreign.wideCharset());
    }
    public byte[] getZeroTerminatedCharArray(long address, Charset charset) {
        return getZeroTerminatedCharArray(address, Integer.MAX_VALUE - 8, charset);
    }
    public byte[] getZeroTerminatedCharArray(long address, int maxLength, Charset charset) {
        int length = (int) getZeroTerminatedStringLength(address, maxLength, charset);
        byte[] array = new byte[length];
        for (int i = 0; i < length; i ++) {
            array[i] = getChar(address + i);
        }
        return array;
    }
    public char[] getZeroTerminatedUTF16Array(long address) {
        return getZeroTerminatedUTF16Array(address, Integer.MAX_VALUE - 8);
    }
    public char[] getZeroTerminatedUTF16Array(long address, int maxLength) {
        int length = (int) getZeroTerminatedUTF16StringLength(address, maxLength) >>> 1;
        char[] array = new char[length];
        for (int i = 0; i < length; i ++) {
            array[i] = getUTF16(address + (long) i << 1);
        }
        return array;
    }
    public String getZeroTerminatedString(long address) {
        return getZeroTerminatedString(address, Integer.MAX_VALUE - 8);
    }
    public String getZeroTerminatedString(long address, int maxLength) {
        return new String(getZeroTerminatedCharArray(address, maxLength), Foreign.ansiCharset());
    }
    public String getZeroTerminatedWString(long address) {
        return getZeroTerminatedWString(address, Integer.MAX_VALUE - 8);
    }
    public String getZeroTerminatedWString(long address, int maxLength) {
        return new String(getZeroTerminatedWCharArray(address, maxLength), Foreign.wideCharset());
    }
    public String getZeroTerminatedString(long address, Charset charset) {
        return getZeroTerminatedString(address, Integer.MAX_VALUE - 8, charset);
    }
    public String getZeroTerminatedString(long address, int maxLength, Charset charset) {
        if (charset == null) charset = Foreign.ansiCharset();
        return new String(getZeroTerminatedCharArray(address, maxLength, charset), charset);
    }
    public String getZeroTerminatedUTF16String(long address) {
        return getZeroTerminatedUTF16String(address, Integer.MAX_VALUE - 8);
    }
    public String getZeroTerminatedUTF16String(long address, int maxLength) {
        return new String(getZeroTerminatedUTF16Array(address, maxLength));
    }
    public void setZeroTerminatedCharArray(long address, byte[] array) {
        setZeroTerminatedCharArray(address, array, 0, array.length);
    }
    public void setZeroTerminatedCharArray(long address, byte[] array, int index, int length) {
        setZeroTerminatedCharArray(address, array, index, length, null);
    }
    public void setZeroTerminatedWCharArray(long address, byte[] array) {
        setZeroTerminatedWCharArray(address, array, 0, array.length);
    }
    public void setZeroTerminatedWCharArray(long address, byte[] array, int index, int length) {
        setZeroTerminatedCharArray(address, array, index, length, Foreign.wideCharset());
    }
    public void setZeroTerminatedCharArray(long address, byte[] array, Charset charset) {
        setZeroTerminatedCharArray(address, array, 0, array.length, charset);
    }
    public void setZeroTerminatedCharArray(long address, byte[] array, int index, int length, Charset charset) {
        for (int i = 0; i < length; i ++) {
            setChar(address + i, array[index + i]);
        }
        setCharArray(address + length, "\0".getBytes(charset == null ? Foreign.ansiCharset() : charset));
    }
    public void setZeroTerminatedUTF16Array(long address, char[] array) {
        setZeroTerminatedUTF16Array(address, array, 0, array.length);
    }
    public void setZeroTerminatedUTF16Array(long address, char[] array, int index, int length) {
        for (int i = 0; i < length; i ++) {
            setUTF16(address + (long) i << 1, array[index + i]);
        }
        setUTF16(address + (long) length << 1, '\0');
    }
    public void setZeroTerminatedString(long address, String string) {
        setZeroTerminatedString(address, string, 0, string.length());
    }
    public void setZeroTerminatedString(long address, String string, int index, int length) {
        setZeroTerminatedCharArray(address, string.getBytes(Foreign.ansiCharset()), index, length);
    }
    public void setZeroTerminatedWString(long address, String string) {
        setZeroTerminatedWString(address, string, 0, string.length());
    }
    public void setZeroTerminatedWString(long address, String string, int index, int length) {
        setZeroTerminatedWCharArray(address, string.getBytes(Foreign.wideCharset()), index, length);
    }
    public void setZeroTerminatedString(long address, String string, Charset charset) {
        setZeroTerminatedString(address, string, 0, string.length(), charset);
    }
    public void setZeroTerminatedString(long address, String string, int index, int length, Charset charset) {
        setZeroTerminatedCharArray(address, string.getBytes(charset == null ? Foreign.ansiCharset() : charset), index, length, charset);
    }
    public void setZeroTerminatedUTF16String(long address, String string) {
        setZeroTerminatedUTF16String(address, string, 0, string.length());
    }
    public void setZeroTerminatedUTF16String(long address, String string, int index, int length) {
        for (int i = 0; i < length; i ++) {
            setUTF16(address + (long) i << 1, string.charAt(index + i));
        }
        setUTF16(address + (long) length << 1, '\0');
    }

    public abstract boolean getBoolean(Object array, long arrayOffset);
    public abstract byte getInt8(Object array, long arrayOffset);
    public abstract short getInt16(Object array, long arrayOffset);
    public abstract char getUTF16(Object array, long arrayOffset);
    public abstract int getInt32(Object array, long arrayOffset);
    public abstract long getInt64(Object array, long arrayOffset);
    public byte getChar(Object array, long arrayOffset) {
        return getInt8(array, arrayOffset);
    }
    public long getShort(Object array, long arrayOffset) {
        return Int64Adapter.SHORT.get(this, array, arrayOffset);
    }
    public long getInt(Object array, long arrayOffset) {
        return Int64Adapter.INT.get(this, array, arrayOffset);
    }
    public long getLong(Object array, long arrayOffset) {
        return Int64Adapter.LONG.get(this, array, arrayOffset);
    }
    public long getAddress(Object array, long arrayOffset) {
        return Int64Adapter.ADDRESS.get(this, array, arrayOffset);
    }
    public abstract float getFloat(Object array, long arrayOffset);
    public abstract double getDouble(Object array, long arrayOffset);
    public abstract void setBoolean(Object array, long arrayOffset, boolean value);
    public abstract void setInt8(Object array, long arrayOffset, byte value);
    public void setInt8(Object array, long arrayOffset, int value) {
        setInt8(array, arrayOffset, (byte) value);
    }
    public abstract void setInt16(Object array, long arrayOffset, short value);
    public void setInt16(Object array, long arrayOffset, int value) {
        setInt16(array, arrayOffset, (short) value);
    }
    public abstract void setUTF16(Object array, long arrayOffset, char value);
    public abstract void setInt32(Object array, long arrayOffset, int value);
    public abstract void setInt64(Object array, long arrayOffset, long value);
    public void setChar(Object array, long arrayOffset, byte value) {
        setInt8(array, arrayOffset, value);
    }
    public void setChar(Object array, long arrayOffset, int value) {
        setInt8(array, arrayOffset, value);
    }
    public void setShort(Object array, long arrayOffset, long value) {
        Int64Adapter.SHORT.set(this, array, arrayOffset, value);
    }
    public void setInt(Object array, long arrayOffset, long value) {
        Int64Adapter.INT.set(this, array, arrayOffset, value);
    }
    public void setLong(Object array, long arrayOffset, long value) {
        Int64Adapter.LONG.set(this, array, arrayOffset, value);
    }
    public void setAddress(Object array, long arrayOffset, long value) {
        Int64Adapter.ADDRESS.set(this, array, arrayOffset, value);
    }
    public abstract void setFloat(Object array, long arrayOffset, float value);
    public abstract void setDouble(Object array, long arrayOffset, double value);

    public void getBooleanArray(Object memoryArray, long memoryArrayOffset, boolean[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getBoolean(memoryArray, memoryArrayOffset + i);
        }
    }
    public void getBooleanArray(Object memoryArray, long memoryArrayOffset, boolean[] array) {
        getBooleanArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setBooleanArray(Object memoryArray, long memoryArrayOffset, boolean[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setBoolean(memoryArray, memoryArrayOffset + i, array[index + i]);
        }
    }
    public void setBooleanArray(Object memoryArray, long memoryArrayOffset, boolean[] array) {
        setBooleanArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getInt8Array(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt8(memoryArray, memoryArrayOffset + i);
        }
    }
    public void getInt8Array(Object memoryArray, long memoryArrayOffset, byte[] array) {
        getInt8Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setInt8Array(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setInt8(memoryArray, memoryArrayOffset + i, array[index + i]);
        }
    }
    public void setInt8Array(Object memoryArray, long memoryArrayOffset, byte[] array) {
        setInt8Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getInt16Array(Object memoryArray, long memoryArrayOffset, short[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt16(memoryArray, memoryArrayOffset + (long) i << 1);
        }
    }
    public void getInt16Array(Object memoryArray, long memoryArrayOffset, short[] array) {
        getInt16Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setInt16Array(Object memoryArray, long memoryArrayOffset, short[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setInt16(memoryArray, memoryArrayOffset + (long) i << 1, array[index + i]);
        }
    }
    public void setInt16Array(Object memoryArray, long memoryArrayOffset, short[] array) {
        setInt16Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getUTF16(memoryArray, memoryArrayOffset + (long) i << 1);
        }
    }
    public void getUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array) {
        getUTF16Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setUTF16(memoryArray, memoryArrayOffset + (long) i << 1, array[index + i]);
        }
    }
    public void setUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array) {
        setUTF16Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getInt32Array(Object memoryArray, long memoryArrayOffset, int[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt32(memoryArray, memoryArrayOffset + (long) i << 2);
        }
    }
    public void getInt32Array(Object memoryArray, long memoryArrayOffset, int[] array) {
        getInt32Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setInt32Array(Object memoryArray, long memoryArrayOffset, int[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setInt32(memoryArray, memoryArrayOffset + (long) i << 2, array[index + i]);
        }
    }
    public void setInt32Array(Object memoryArray, long memoryArrayOffset, int[] array) {
        setInt32Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getInt64Array(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt64(memoryArray, memoryArrayOffset + (long) i << 3);
        }
    }
    public void getInt64Array(Object memoryArray, long memoryArrayOffset, long[] array) {
        getInt64Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setInt64Array(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setInt64(memoryArray, memoryArrayOffset + (long) i << 3, array[index + i]);
        }
    }
    public void setInt64Array(Object memoryArray, long memoryArrayOffset, long[] array) {
        setInt64Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getFloatArray(Object memoryArray, long memoryArrayOffset, float[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getFloat(memoryArray, memoryArrayOffset + (long) i << 2);
        }
    }
    public void getFloatArray(Object memoryArray, long memoryArrayOffset, float[] array) {
        getFloatArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setFloatArray(Object memoryArray, long memoryArrayOffset, float[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setFloat(memoryArray, memoryArrayOffset + (long) i << 2, array[index + i]);
        }
    }
    public void setFloatArray(Object memoryArray, long memoryArrayOffset, float[] array) {
        setFloatArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getDoubleArray(Object memoryArray, long memoryArrayOffset, double[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = getDouble(memoryArray, memoryArrayOffset + (long) i << 3);
        }
    }
    public void getDoubleArray(Object memoryArray, long memoryArrayOffset, double[] array) {
        getDoubleArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setDoubleArray(Object memoryArray, long memoryArrayOffset, double[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            setDouble(memoryArray, memoryArrayOffset + (long) i << 3, array[index + i]);
        }
    }
    public void setDoubleArray(Object memoryArray, long memoryArrayOffset, double[] array) {
        setDoubleArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        getInt8Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public void getCharArray(Object memoryArray, long memoryArrayOffset, byte[] array) {
        getInt8Array(memoryArray, memoryArrayOffset, array);
    }
    public void setCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        setInt8Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public void setCharArray(Object memoryArray, long memoryArrayOffset, byte[] array) {
        setInt8Array(memoryArray, memoryArrayOffset, array);
    }
    public void getShortArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = Int64Adapter.SHORT.get(this, memoryArray, memoryArrayOffset + (long) i * Foreign.shortSize());
        }
    }
    public void getShortArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        getShortArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setShortArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int64Adapter.SHORT.set(this, memoryArray, memoryArrayOffset + (long) i * Foreign.shortSize(), array[index + i]);
        }
    }
    public void setShortArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        setShortArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getIntArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = Int64Adapter.INT.get(this, memoryArray, memoryArrayOffset + (long) i * Foreign.intSize());
        }
    }
    public void getIntArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        getIntArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setIntArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int64Adapter.INT.set(this, memoryArray, memoryArrayOffset + (long) i * Foreign.intSize(), array[index + i]);
        }
    }
    public void setIntArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        setIntArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getLongArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int64Adapter.LONG.set(this, memoryArray, memoryArrayOffset + (long) i * Foreign.longSize(), array[index + i]);
        }
    }
    public void getLongArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        getLongArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setLongArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            array[index + i] = Int64Adapter.LONG.get(this, memoryArray, memoryArrayOffset + (long) i * Foreign.longSize());
        }
    }
    public void setLongArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        setLongArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void getAddressArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int64Adapter.ADDRESS.set(this, memoryArray, memoryArrayOffset + (long) i * Foreign.addressSize(), array[index + i]);
        }
    }
    public void getAddressArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        getAddressArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setAddressArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = index + length;
        if (size < 0 || size > array.length) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Integer.toUnsignedString(size));
        for (int i = 0; i < length; i ++) {
            Int64Adapter.ADDRESS.set(this, memoryArray, memoryArrayOffset + (long) i * Foreign.addressSize(), array[index + i]);
        }
    }
    public void setAddressArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        setAddressArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public long getZeroTerminatedStringLength(Object memoryArray, long memoryArrayOffset) {
        return getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, Limits.ADDRESS_MAX);
    }
    public long getZeroTerminatedStringLength(Object memoryArray, long memoryArrayOffset, long maxLength) {
        return getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, maxLength, null);
    }
    public long getZeroTerminatedWStringLength(Object memoryArray, long memoryArrayOffset) {
        return getZeroTerminatedWStringLength(memoryArray, memoryArrayOffset, Limits.ADDRESS_MAX);
    }
    public long getZeroTerminatedWStringLength(Object memoryArray, long memoryArrayOffset, long maxLength) {
        return getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, maxLength, Foreign.wideCharset());
    }
    public long getZeroTerminatedStringLength(Object memoryArray, long memoryArrayOffset, Charset charset) {
        return getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, Limits.ADDRESS_MAX, charset);
    }
    public long getZeroTerminatedStringLength(Object memoryArray, long memoryArrayOffset, long maxLength, Charset charset) {
        byte[] terminator = "\0".getBytes(charset == null ? Foreign.ansiCharset() : charset);
        return search(memoryArray, memoryArrayOffset, terminator, 0, terminator.length, maxLength) - memoryArrayOffset;
    }
    public long getZeroTerminatedUTF16StringLength(Object memoryArray, long memoryArrayOffset) {
        return getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, Limits.ADDRESS_MAX);
    }
    public long getZeroTerminatedUTF16StringLength(Object memoryArray, long memoryArrayOffset, long maxLength) {
        return getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, maxLength, Foreign.utf16Charset()) >>> 1;
    }
    public byte[] getZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset) {
        return getZeroTerminatedCharArray(memoryArray, memoryArrayOffset, Integer.MAX_VALUE - 8);
    }
    public byte[] getZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, int maxLength) {
        int length = (int) getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, maxLength);
        byte[] array = new byte[length];
        for (int i = 0; i < length; i ++) {
            array[i] = getChar(memoryArray, memoryArrayOffset + i);
        }
        return array;
    }
    public byte[] getZeroTerminatedWCharArray(Object memoryArray, long memoryArrayOffset) {
        return getZeroTerminatedWCharArray(memoryArray, memoryArrayOffset, Integer.MAX_VALUE - 8);
    }
    public byte[] getZeroTerminatedWCharArray(Object memoryArray, long memoryArrayOffset, int maxLength) {
        int length = (int) getZeroTerminatedWStringLength(memoryArray, memoryArrayOffset, maxLength);
        byte[] array = new byte[length];
        for (int i = 0; i < length; i ++) {
            array[i] = getChar(memoryArray, memoryArrayOffset + i);
        }
        return array;
    }
    public byte[] getZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, Charset charset) {
        return getZeroTerminatedCharArray(memoryArray, memoryArrayOffset, Integer.MAX_VALUE - 8, charset);
    }
    public byte[] getZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, int maxLength, Charset charset) {
        int length = (int) getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, maxLength, charset);
        byte[] array = new byte[length];
        for (int i = 0; i < length; i ++) {
            array[i] = getChar(memoryArray, memoryArrayOffset + i);
        }
        return array;
    }
    public char[] getZeroTerminatedUTF16Array(Object memoryArray, long memoryArrayOffset) {
        return getZeroTerminatedUTF16Array(memoryArray, memoryArrayOffset, Integer.MAX_VALUE - 8);
    }
    public char[] getZeroTerminatedUTF16Array(Object memoryArray, long memoryArrayOffset, int maxLength) {
        int length = (int) getZeroTerminatedUTF16StringLength(memoryArray, memoryArrayOffset, maxLength);
        char[] array = new char[length];
        for (int i = 0; i < length; i ++) {
            array[i] = getUTF16(memoryArray, memoryArrayOffset + (long) i << 1);
        }
        return array;
    }
    public String getZeroTerminatedString(Object memoryArray, long memoryArrayOffset) {
        return getZeroTerminatedString(memoryArray, memoryArrayOffset, Integer.MAX_VALUE - 8);
    }
    public String getZeroTerminatedString(Object memoryArray, long memoryArrayOffset, int maxLength) {
        return new String(getZeroTerminatedCharArray(memoryArray, memoryArrayOffset, maxLength), Foreign.ansiCharset());
    }
    public String getZeroTerminatedWString(Object memoryArray, long memoryArrayOffset) {
        return getZeroTerminatedWString(memoryArray, memoryArrayOffset, Integer.MAX_VALUE - 8);
    }
    public String getZeroTerminatedWString(Object memoryArray, long memoryArrayOffset, int maxLength) {
        return new String(getZeroTerminatedWCharArray(memoryArray, memoryArrayOffset, maxLength), Foreign.wideCharset());
    }
    public String getZeroTerminatedString(Object memoryArray, long memoryArrayOffset, Charset charset) {
        return getZeroTerminatedString(memoryArray, memoryArrayOffset, Integer.MAX_VALUE - 8, charset);
    }
    public String getZeroTerminatedString(Object memoryArray, long memoryArrayOffset, int maxLength, Charset charset) {
        if (charset == null) charset = Foreign.ansiCharset();
        return new String(getZeroTerminatedCharArray(memoryArray, memoryArrayOffset, maxLength, charset), charset);
    }
    public String getZeroTerminatedUTF16String(Object memoryArray, long memoryArrayOffset) {
        return getZeroTerminatedUTF16String(memoryArray, memoryArrayOffset, Integer.MAX_VALUE - 8);
    }
    public String getZeroTerminatedUTF16String(Object memoryArray, long memoryArrayOffset, int maxLength) {
        return new String(getZeroTerminatedUTF16Array(memoryArray, memoryArrayOffset, maxLength));
    }
    public void setZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, byte[] array) {
        setZeroTerminatedCharArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        for (int i = 0; i < length; i ++) {
            setChar(memoryArray, memoryArrayOffset + i, array[index + i]);
        }
        setCharArray(memoryArray, memoryArrayOffset + length, "\0".getBytes(Foreign.ansiCharset()));
    }
    public void setZeroTerminatedWCharArray(Object memoryArray, long memoryArrayOffset, byte[] array) {
        setZeroTerminatedWCharArray(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setZeroTerminatedWCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        for (int i = 0; i < length; i ++) {
            setChar(memoryArray, memoryArrayOffset + i, array[index + i]);
        }
        setCharArray(memoryArray, memoryArrayOffset + length, "\0".getBytes(Foreign.wideCharset()));
    }
    public void setZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, Charset charset) {
        setZeroTerminatedCharArray(memoryArray, memoryArrayOffset, array, 0, array.length, charset);
    }
    public void setZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length, Charset charset) {
        for (int i = 0; i < length; i ++) {
            setChar(memoryArray, memoryArrayOffset + i, array[index + i]);
        }
        setCharArray(memoryArray, memoryArrayOffset + length, "\0".getBytes(charset == null ? Foreign.ansiCharset() : charset));
    }
    public void setZeroTerminatedUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array) {
        setZeroTerminatedUTF16Array(memoryArray, memoryArrayOffset, array, 0, array.length);
    }
    public void setZeroTerminatedUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array, int index, int length) {
        for (int i = 0; i < length; i ++) {
            setUTF16(memoryArray, memoryArrayOffset + (long) i << 1, array[index + i]);
        }
        setUTF16(memoryArray, memoryArrayOffset + (long) length << 1, '\0');
    }
    public void setZeroTerminatedString(Object memoryArray, long memoryArrayOffset, String string) {
        setZeroTerminatedString(memoryArray, memoryArrayOffset, string, 0, string.length());
    }
    public void setZeroTerminatedString(Object memoryArray, long memoryArrayOffset, String string, int index, int length) {
        setZeroTerminatedCharArray(memoryArray, memoryArrayOffset, string.getBytes(Foreign.ansiCharset()), index, length);
    }
    public void setZeroTerminatedWString(Object memoryArray, long memoryArrayOffset, String string) {
        setZeroTerminatedWString(memoryArray, memoryArrayOffset, string, 0, string.length());
    }
    public void setZeroTerminatedWString(Object memoryArray, long memoryArrayOffset, String string, int index, int length) {
        setZeroTerminatedWCharArray(memoryArray, memoryArrayOffset, string.getBytes(Foreign.wideCharset()), index, length);
    }
    public void setZeroTerminatedString(Object memoryArray, long memoryArrayOffset, String string, Charset charset) {
        setZeroTerminatedString(memoryArray, memoryArrayOffset, string, 0, string.length(), charset);
    }
    public void setZeroTerminatedString(Object memoryArray, long memoryArrayOffset, String string, int index, int length, Charset charset) {
        setZeroTerminatedCharArray(memoryArray, memoryArrayOffset, string.getBytes(charset == null ? Foreign.ansiCharset() : charset), index, length, charset);
    }
    public void setZeroTerminatedUTF16String(Object memoryArray, long memoryArrayOffset, String string) {
        setZeroTerminatedUTF16String(memoryArray, memoryArrayOffset, string, 0, string.length());
    }
    public void setZeroTerminatedUTF16String(Object memoryArray, long memoryArrayOffset, String string, int index, int length) {
        for (int i = 0; i < length; i ++) {
            setUTF16(memoryArray, memoryArrayOffset + (long) i << 1, string.charAt(index + i));
        }
        setUTF16(memoryArray, memoryArrayOffset + (long) length << 1, '\0');
    }

    private abstract static class Int64Adapter {
        
        public abstract long get(AllocatorProvider provider, Object array, long offset);
        public abstract void set(AllocatorProvider provider, Object array, long offset, long value);
        public abstract long get(AllocatorProvider provider, long address);
        public abstract void set(AllocatorProvider provider, long address, long value);

        public static final Int64Adapter SIZE64 = new Int64Adapter() {
            @Override
            public long get(AllocatorProvider provider, Object array, long offset) {
                return provider.getInt64(array, offset);
            }
            @Override
            public void set(AllocatorProvider provider, Object array, long offset, long value) {
                provider.setInt64(array, offset, value);
            }
            @Override
            public long get(AllocatorProvider provider, long address) {
                return provider.getInt64(address);
            }
            @Override
            public void set(AllocatorProvider provider, long address, long value) {
                provider.setInt64(address, value);
            }
        };

        public static final Int64Adapter SIZE32 = new Int64Adapter() {
            @Override
            public long get(AllocatorProvider provider, Object array, long offset) {
                return (long) provider.getInt32(array, offset) & 0xFFFFFFFFL;
            }
            @Override
            public void set(AllocatorProvider provider, Object array, long offset, long value) {
                if ((((value >> 32) + 1) & ~1) != 0) throw new ArithmeticException("integer overflow");
                provider.setInt32(array, offset, (int) value);
            }
            @Override
            public long get(AllocatorProvider provider, long address) {
                return (long) provider.getInt32(address) & 0xFFFFFFFFL;
            }
            @Override
            public void set(AllocatorProvider provider, long address, long value) {
                if ((((value >> 32) + 1) & ~1) != 0) throw new ArithmeticException("integer overflow");
                provider.setInt32(address, (int) value);
            }
        };

        public static final Int64Adapter SIZE16 = new Int64Adapter() {
            @Override
            public long get(AllocatorProvider provider, Object array, long offset) {
                return (long) provider.getInt16(array, offset) & 0xFFFFL;
            }
            @Override
            public void set(AllocatorProvider provider, Object array, long offset, long value) {
                if (value > 65535 || value < 0) throw new ArithmeticException("integer overflow");
                provider.setInt16(array, offset, (short) value);
            }
            @Override
            public long get(AllocatorProvider provider, long address) {
                return (long) provider.getInt16(address) & 0xFFFFL;
            }
            @Override
            public void set(AllocatorProvider provider, long address, long value) {
                if (value > 65535 || value < 0) throw new ArithmeticException("integer overflow");
                provider.setInt16(address, (short) value);
            }
        };

        public static final Int64Adapter SHORT = Foreign.shortSize() == 8 ? SIZE64 : SIZE16;
        public static final Int64Adapter INT = Foreign.intSize() == 8 ? SIZE64 : SIZE32;
        public static final Int64Adapter LONG = Foreign.longSize() == 8 ? SIZE64 : SIZE32;
        public static final Int64Adapter ADDRESS = Foreign.addressSize() == 8 ? SIZE64 : SIZE32;

    }

    private abstract static class Int32Adapter {

        public abstract int get(AllocatorProvider provider, Object array, long offset);
        public abstract void set(AllocatorProvider provider, Object array, long offset, int value);
        public abstract int get(AllocatorProvider provider, long address);
        public abstract void set(AllocatorProvider provider, long address, int value);

        public static final Int32Adapter SIZE32 = new Int32Adapter() {
            @Override
            public int get(AllocatorProvider provider, Object array, long offset) {
                return provider.getInt32(array, offset);
            }
            @Override
            public void set(AllocatorProvider provider, Object array, long offset, int value) {
                provider.setInt32(array, offset, value);
            }
            @Override
            public int get(AllocatorProvider provider, long address) {
                return provider.getInt32(address);
            }
            @Override
            public void set(AllocatorProvider provider, long address, int value) {
                provider.setInt32(address, value);
            }
        };

        public static final Int32Adapter SIZE16 = new Int32Adapter() {
            @Override
            public int get(AllocatorProvider provider, Object array, long offset) {
                return (int) provider.getInt16(array, offset) & 0xFFFF;
            }
            @Override
            public void set(AllocatorProvider provider, Object array, long offset, int value) {
                if (value > 65535 || value < 0) throw new ArithmeticException("integer overflow");
                provider.setInt16(array, offset, (short) value);
            }
            @Override
            public int get(AllocatorProvider provider, long address) {
                return (int) provider.getInt16(address) & 0xFFFF;
            }
            @Override
            public void set(AllocatorProvider provider, long address, int value) {
                if (value > 65535 || value < 0) throw new ArithmeticException("integer overflow");
                provider.setInt16(address, (short) value);
            }
        };

        public static final Int32Adapter WCHAR = Foreign.wcharSize() == 4 ? SIZE32 : SIZE16;

    }

}
