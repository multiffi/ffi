package multiffi;

import multiffi.spi.AllocatorProvider;

import java.nio.charset.Charset;

public final class Allocator {

    private Allocator() {
        throw new AssertionError("No multiffi.Allocator instances for you!");
    }

    private static final AllocatorProvider IMPLEMENTATION = AllocatorProvider.getImplementation();

    public static long allocate(long size) {
        return IMPLEMENTATION.allocate(size);
    }
    public static long allocateInitialized(long count, long size) {
        return IMPLEMENTATION.allocateInitialized(count, size);
    }
    public static long reallocate(long address, long size) {
        return IMPLEMENTATION.reallocate(address, size);
    }
    public static void free(long address) {
        IMPLEMENTATION.free(address);
    }

    public static long search(long address, byte value, long maxLength) {
        return IMPLEMENTATION.search(address, value, maxLength);
    }
    public static long search(long address, byte value) {
        return IMPLEMENTATION.search(address, value);
    }
    public static long search(long address, int value, long maxLength) {
        return IMPLEMENTATION.search(address, value, maxLength);
    }
    public static long search(long address, int value) {
        return IMPLEMENTATION.search(address, value);
    }
    public static long search(Object array, long arrayOffset, byte value, long maxLength) {
        return IMPLEMENTATION.search(array, arrayOffset, value, maxLength);
    }
    public static long search(Object array, long arrayOffset, byte value) {
        return IMPLEMENTATION.search(array, arrayOffset, value);
    }
    public static long search(Object array, long arrayOffset, int value, long maxLength) {
        return IMPLEMENTATION.search(array, arrayOffset, value, maxLength);
    }
    public static long search(Object array, long arrayOffset, int value) {
        return IMPLEMENTATION.search(array, arrayOffset, value);
    }
    public static long search(long address, long valueAddress, long valueSize, long maxLength) {
        return IMPLEMENTATION.search(address, valueAddress, valueSize, maxLength);
    }
    public static long search(long address, long valueAddress, long valueSize) {
        return IMPLEMENTATION.search(address, valueAddress, valueSize);
    }
    public static long search(long address, Object valueArray, int valueArrayOffset, int valueSize, long maxLength) {
        return IMPLEMENTATION.search(address, valueArray, valueArrayOffset, valueSize, maxLength);
    }
    public static long search(long address, Object valueArray, int valueArrayOffset, int valueSize) {
        return IMPLEMENTATION.search(address, valueArray, valueArrayOffset, valueSize);
    }
    public static long search(Object array, long arrayOffset, long valueAddress, int valueSize, long maxLength) {
        return IMPLEMENTATION.search(array, arrayOffset, valueAddress, valueSize, maxLength);
    }
    public static long search(Object array, long arrayOffset, long valueAddress, int valueSize) {
        return IMPLEMENTATION.search(array, arrayOffset, valueAddress, valueSize);
    }
    public static long search(Object array, long arrayOffset, Object valueArray, int valueArrayOffset, int valueSize, long maxLength) {
        return IMPLEMENTATION.search(array, arrayOffset, valueArray, valueArrayOffset, valueSize, maxLength);
    }
    public static long search(Object array, long arrayOffset, Object valueArray, int valueArrayOffset, int valueSize) {
        return IMPLEMENTATION.search(array, arrayOffset, valueArray, valueArrayOffset, valueSize);
    }
    public static int compare(long aAddress, long bAddress, long size) {
        return IMPLEMENTATION.compare(aAddress, bAddress, size);
    }
    public static int compare(long address, Object array, long arrayOffset, long size) {
        return IMPLEMENTATION.compare(address, array, arrayOffset, size);
    }
    public static int compare(Object array, long arrayOffset, long address, long size) {
        return IMPLEMENTATION.compare(array, arrayOffset, address, size);
    }
    public static int compare(Object aArray, long aArrayOffset, Object bArray, long bArrayOffset, long size) {
        return IMPLEMENTATION.compare(aArray, aArrayOffset, bArray, bArrayOffset, size);
    }
    public static long fill(long address, byte value, long size) {
        return IMPLEMENTATION.fill(address, value, size);
    }
    public static long fill(long address, int value, long size) {
        return IMPLEMENTATION.fill(address, value, size);
    }
    public static Object fill(Object array, long arrayOffset, byte value, long size) {
        return IMPLEMENTATION.fill(array, arrayOffset, value, size);
    }
    public static Object fill(Object array, long arrayOffset, int value, long size) {
        return IMPLEMENTATION.fill(array, arrayOffset, value, size);
    }
    public static long copy(long destAddress, long srcAddress, long size) {
        return IMPLEMENTATION.copy(destAddress, srcAddress, size);
    }
    public static long copy(long destAddress, Object srcArray, long srcArrayOffset, long size) {
        return IMPLEMENTATION.copy(destAddress, srcArray, srcArrayOffset, size);
    }
    public static Object copy(Object destArray, long destArrayOffset, long srcAddress, long size) {
        return IMPLEMENTATION.copy(destArray, destArrayOffset, srcAddress, size);
    }
    public static Object copy(Object destArray, long destArrayOffset, Object srcArray, long srcArrayOffset, long size) {
        return IMPLEMENTATION.copy(destArray, destArrayOffset, srcArray, srcArrayOffset, size);
    }

    public static boolean getBoolean(long address) {
        return IMPLEMENTATION.getBoolean(address);
    }
    public static byte getInt8(long address) {
        return IMPLEMENTATION.getInt8(address);
    }
    public static short getInt16(long address) {
        return IMPLEMENTATION.getInt16(address);
    }
    public static char getUTF16(long address) {
        return IMPLEMENTATION.getUTF16(address);
    }
    public static int getInt32(long address) {
        return IMPLEMENTATION.getInt32(address);
    }
    public static long getInt64(long address) {
        return IMPLEMENTATION.getInt64(address);
    }
    public static byte getChar(long address) {
        return IMPLEMENTATION.getChar(address);
    }
    public static long getShort(long address) {
        return IMPLEMENTATION.getShort(address);
    }
    public static long getInt(long address) {
        return IMPLEMENTATION.getInt(address);
    }
    public static long getLong(long address) {
        return IMPLEMENTATION.getLong(address);
    }
    public static long getAddress(long address) {
        return IMPLEMENTATION.getAddress(address);
    }
    public static float getFloat(long address) {
        return IMPLEMENTATION.getFloat(address);
    }
    public static double getDouble(long address) {
        return IMPLEMENTATION.getDouble(address);
    }
    public static void setBoolean(long address, boolean value) {
        IMPLEMENTATION.setBoolean(address, value);
    }
    public static void setInt8(long address, byte value) {
        IMPLEMENTATION.setInt8(address, value);
    }
    public static void setInt8(long address, int value) {
        IMPLEMENTATION.setInt8(address, value);
    }
    public static void setInt16(long address, short value) {
        IMPLEMENTATION.setInt16(address, value);
    }
    public static void setInt16(long address, int value) {
        IMPLEMENTATION.setInt16(address, value);
    }
    public static void setUTF16(long address, char value) {
        IMPLEMENTATION.setUTF16(address, value);
    }
    public static void setInt32(long address, int value) {
        IMPLEMENTATION.setInt32(address, value);
    }
    public static void setInt64(long address, long value) {
        IMPLEMENTATION.setInt64(address, value);
    }
    public static void setChar(long address, byte value) {
        IMPLEMENTATION.setChar(address, value);
    }
    public static void setChar(long address, int value) {
        IMPLEMENTATION.setChar(address, value);
    }
    public static void setShort(long address, long value) {
        IMPLEMENTATION.setShort(address, value);
    }
    public static void setInt(long address, long value) {
        IMPLEMENTATION.setInt(address, value);
    }
    public static void setLong(long address, long value) {
        IMPLEMENTATION.setLong(address, value);
    }
    public static void setAddress(long address, long value) {
        IMPLEMENTATION.setAddress(address, value);
    }
    public static void setFloat(long address, float value) {
        IMPLEMENTATION.setFloat(address, value);
    }
    public static void setDouble(long address, double value) {
        IMPLEMENTATION.setDouble(address, value);
    }

    public static void getBooleanArray(long address, boolean[] array, int index, int length) {
        IMPLEMENTATION.getBooleanArray(address, array, index, length);
    }
    public static void getBooleanArray(long address, boolean[] array) {
        IMPLEMENTATION.getBooleanArray(address, array);
    }
    public static void setBooleanArray(long address, boolean[] array, int index, int length) {
        IMPLEMENTATION.setBooleanArray(address, array, index, length);
    }
    public static void setBooleanArray(long address, boolean[] array) {
        IMPLEMENTATION.setBooleanArray(address, array);
    }
    public static void getInt8Array(long address, byte[] array, int index, int length) {
        IMPLEMENTATION.getInt8Array(address, array, index, length);
    }
    public static void getInt8Array(long address, byte[] array) {
        IMPLEMENTATION.getInt8Array(address, array);
    }
    public static void setInt8Array(long address, byte[] array, int index, int length) {
        IMPLEMENTATION.setInt8Array(address, array, index, length);
    }
    public static void setInt8Array(long address, byte[] array) {
        IMPLEMENTATION.setInt8Array(address, array);
    }
    public static void getInt16Array(long address, short[] array, int index, int length) {
        IMPLEMENTATION.getInt16Array(address, array, index, length);
    }
    public static void getInt16Array(long address, short[] array) {
        IMPLEMENTATION.getInt16Array(address, array);
    }
    public static void setInt16Array(long address, short[] array, int index, int length) {
        IMPLEMENTATION.setInt16Array(address, array, index, length);
    }
    public static void setInt16Array(long address, short[] array) {
        IMPLEMENTATION.setInt16Array(address, array);
    }
    public static void getUTF16Array(long address, char[] array, int index, int length) {
        IMPLEMENTATION.getUTF16Array(address, array, index, length);
    }
    public static void getUTF16Array(long address, char[] array) {
        IMPLEMENTATION.getUTF16Array(address, array);
    }
    public static void setUTF16Array(long address, char[] array, int index, int length) {
        IMPLEMENTATION.setUTF16Array(address, array, index, length);
    }
    public static void setUTF16Array(long address, char[] array) {
        IMPLEMENTATION.setUTF16Array(address, array);
    }
    public static void getInt32Array(long address, int[] array, int index, int length) {
        IMPLEMENTATION.getInt32Array(address, array, index, length);
    }
    public static void getInt32Array(long address, int[] array) {
        IMPLEMENTATION.getInt32Array(address, array);
    }
    public static void setInt32Array(long address, int[] array, int index, int length) {
        IMPLEMENTATION.setInt32Array(address, array, index, length);
    }
    public static void setInt32Array(long address, int[] array) {
        IMPLEMENTATION.setInt32Array(address, array);
    }
    public static void getInt64Array(long address, long[] array, int index, int length) {
        IMPLEMENTATION.getInt64Array(address, array, index, length);
    }
    public static void getInt64Array(long address, long[] array) {
        IMPLEMENTATION.getInt64Array(address, array);
    }
    public static void setInt64Array(long address, long[] array, int index, int length) {
        IMPLEMENTATION.setInt64Array(address, array, index, length);
    }
    public static void setInt64Array(long address, long[] array) {
        IMPLEMENTATION.setInt64Array(address, array);
    }
    public static void getFloatArray(long address, float[] array, int index, int length) {
        IMPLEMENTATION.getFloatArray(address, array, index, length);
    }
    public static void getFloatArray(long address, float[] array) {
        IMPLEMENTATION.getFloatArray(address, array);
    }
    public static void setFloatArray(long address, float[] array, int index, int length) {
        IMPLEMENTATION.setFloatArray(address, array, index, length);
    }
    public static void setFloatArray(long address, float[] array) {
        IMPLEMENTATION.setFloatArray(address, array);
    }
    public static void getDoubleArray(long address, double[] array, int index, int length) {
        IMPLEMENTATION.getDoubleArray(address, array, index, length);
    }
    public static void getDoubleArray(long address, double[] array) {
        IMPLEMENTATION.getDoubleArray(address, array);
    }
    public static void setDoubleArray(long address, double[] array, int index, int length) {
        IMPLEMENTATION.setDoubleArray(address, array, index, length);
    }
    public static void setDoubleArray(long address, double[] array) {
        IMPLEMENTATION.setDoubleArray(address, array);
    }
    public static void getCharArray(long address, byte[] array, int index, int length) {
        IMPLEMENTATION.getCharArray(address, array, index, length);
    }
    public static void getCharArray(long address, byte[] array) {
        IMPLEMENTATION.getCharArray(address, array);
    }
    public static void setCharArray(long address, byte[] array, int index, int length) {
        IMPLEMENTATION.setCharArray(address, array, index, length);
    }
    public static void setCharArray(long address, byte[] array) {
        IMPLEMENTATION.setCharArray(address, array);
    }
    public static void getShortArray(long address, long[] array, int index, int length) {
        IMPLEMENTATION.getShortArray(address, array, index, length);
    }
    public static void getShortArray(long address, long[] array) {
        IMPLEMENTATION.getShortArray(address, array);
    }
    public static void setShortArray(long address, long[] array, int index, int length) {
        IMPLEMENTATION.setShortArray(address, array, index, length);
    }
    public static void setShortArray(long address, long[] array) {
        IMPLEMENTATION.setShortArray(address, array);
    }
    public static void getIntArray(long address, long[] array, int index, int length) {
        IMPLEMENTATION.getIntArray(address, array, index, length);
    }
    public static void getIntArray(long address, long[] array) {
        IMPLEMENTATION.getIntArray(address, array);
    }
    public static void setIntArray(long address, long[] array, int index, int length) {
        IMPLEMENTATION.setIntArray(address, array, index, length);
    }
    public static void setIntArray(long address, long[] array) {
        IMPLEMENTATION.setIntArray(address, array);
    }
    public static void getLongArray(long address, long[] array, int index, int length) {
        IMPLEMENTATION.getLongArray(address, array, index, length);
    }
    public static void getLongArray(long address, long[] array) {
        IMPLEMENTATION.getLongArray(address, array);
    }
    public static void setLongArray(long address, long[] array, int index, int length) {
        IMPLEMENTATION.setLongArray(address, array, index, length);
    }
    public static void setLongArray(long address, long[] array) {
        IMPLEMENTATION.setLongArray(address, array);
    }
    public static void getAddressArray(long address, long[] array, int index, int length) {
        IMPLEMENTATION.getAddressArray(address, array, index, length);
    }
    public static void getAddressArray(long address, long[] array) {
        IMPLEMENTATION.getAddressArray(address, array);
    }
    public static void setAddressArray(long address, long[] array, int index, int length) {
        IMPLEMENTATION.setAddressArray(address, array, index, length);
    }
    public static void setAddressArray(long address, long[] array) {
        IMPLEMENTATION.setAddressArray(address, array);
    }
    public static long getZeroTerminatedStringLength(long address) {
        return IMPLEMENTATION.getZeroTerminatedStringLength(address);
    }
    public static long getZeroTerminatedStringLength(long address, long maxLength) {
        return IMPLEMENTATION.getZeroTerminatedStringLength(address, maxLength);
    }
    public static long getZeroTerminatedWideStringLength(long address) {
        return IMPLEMENTATION.getZeroTerminatedWideStringLength(address);
    }
    public static long getZeroTerminatedWideStringLength(long address, long maxLength) {
        return IMPLEMENTATION.getZeroTerminatedWideStringLength(address, maxLength);
    }
    public static long getZeroTerminatedStringLength(long address, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedStringLength(address, charset);
    }
    public static long getZeroTerminatedStringLength(long address, long maxLength, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedStringLength(address, maxLength, charset);
    }
    public static long getZeroTerminatedUTF16StringLength(long address) {
        return IMPLEMENTATION.getZeroTerminatedUTF16StringLength(address);
    }
    public static long getZeroTerminatedUTF16StringLength(long address, long maxLength) {
        return IMPLEMENTATION.getZeroTerminatedUTF16StringLength(address, maxLength);
    }
    public static byte[] getZeroTerminatedCharArray(long address) {
        return IMPLEMENTATION.getZeroTerminatedCharArray(address);
    }
    public static byte[] getZeroTerminatedCharArray(long address, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedCharArray(address, maxLength);
    }
    public static byte[] getZeroTerminatedWideCharArray(long address) {
        return IMPLEMENTATION.getZeroTerminatedWideCharArray(address);
    }
    public static byte[] getZeroTerminatedWideCharArray(long address, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedWideCharArray(address, maxLength);
    }
    public static byte[] getZeroTerminatedCharArray(long address, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedCharArray(address, charset);
    }
    public static byte[] getZeroTerminatedCharArray(long address, int maxLength, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedCharArray(address, maxLength, charset);
    }
    public static char[] getZeroTerminatedUTF16Array(long address) {
        return IMPLEMENTATION.getZeroTerminatedUTF16Array(address);
    }
    public static char[] getZeroTerminatedUTF16Array(long address, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedUTF16Array(address, maxLength);
    }
    public static String getZeroTerminatedString(long address) {
        return IMPLEMENTATION.getZeroTerminatedString(address);
    }
    public static String getZeroTerminatedString(long address, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedString(address, maxLength);
    }
    public static String getZeroTerminatedWideString(long address) {
        return IMPLEMENTATION.getZeroTerminatedWideString(address);
    }
    public static String getZeroTerminatedWideString(long address, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedWideString(address, maxLength);
    }
    public static String getZeroTerminatedString(long address, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedString(address, charset);
    }
    public static String getZeroTerminatedString(long address, int maxLength, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedString(address, maxLength, charset);
    }
    public static String getZeroTerminatedUTF16String(long address) {
        return IMPLEMENTATION.getZeroTerminatedUTF16String(address);
    }
    public static String getZeroTerminatedUTF16String(long address, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedUTF16String(address, maxLength);
    }
    public static void setZeroTerminatedCharArray(long address, byte[] array) {
        IMPLEMENTATION.setZeroTerminatedCharArray(address, array);
    }
    public static void setZeroTerminatedCharArray(long address, byte[] array, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedCharArray(address, array, index, length);
    }
    public static void setZeroTerminatedWideCharArray(long address, byte[] array) {
        IMPLEMENTATION.setZeroTerminatedWideCharArray(address, array);
    }
    public static void setZeroTerminatedWideCharArray(long address, byte[] array, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedWideCharArray(address, array, index, length);
    }
    public static void setZeroTerminatedCharArray(long address, byte[] array, Charset charset) {
        IMPLEMENTATION.setZeroTerminatedCharArray(address, array, charset);
    }
    public static void setZeroTerminatedCharArray(long address, byte[] array, int index, int length, Charset charset) {
        IMPLEMENTATION.setZeroTerminatedCharArray(address, array, index, length, charset);
    }
    public static void setZeroTerminatedUTF16Array(long address, char[] array) {
        IMPLEMENTATION.setZeroTerminatedUTF16Array(address, array);
    }
    public static void setZeroTerminatedUTF16Array(long address, char[] array, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedUTF16Array(address, array, index, length);
    }
    public static void setZeroTerminatedString(long address, String string) {
        IMPLEMENTATION.setZeroTerminatedString(address, string);
    }
    public static void setZeroTerminatedString(long address, String string, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedString(address, string, index, length);
    }
    public static void setZeroTerminatedWideString(long address, String string) {
        IMPLEMENTATION.setZeroTerminatedWideString(address, string);
    }
    public static void setZeroTerminatedWideString(long address, String string, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedWideString(address, string, index, length);
    }
    public static void setZeroTerminatedString(long address, String string, Charset charset) {
        IMPLEMENTATION.setZeroTerminatedString(address, string, charset);
    }
    public static void setZeroTerminatedString(long address, String string, int index, int length, Charset charset) {
        IMPLEMENTATION.setZeroTerminatedString(address, string, index, length, charset);
    }
    public static void setZeroTerminatedUTF16String(long address, String string) {
        IMPLEMENTATION.setZeroTerminatedUTF16String(address, string);
    }
    public static void setZeroTerminatedUTF16String(long address, String string, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedUTF16String(address, string, index, length);
    }

    public static boolean getBoolean(Object array, long arrayOffset) {
        return IMPLEMENTATION.getBoolean(array, arrayOffset);
    }
    public static byte getInt8(Object array, long arrayOffset) {
        return IMPLEMENTATION.getInt8(array, arrayOffset);
    }
    public static short getInt16(Object array, long arrayOffset) {
        return IMPLEMENTATION.getInt16(array, arrayOffset);
    }
    public static char getUTF16(Object array, long arrayOffset) {
        return IMPLEMENTATION.getUTF16(array, arrayOffset);
    }
    public static int getInt32(Object array, long arrayOffset) {
        return IMPLEMENTATION.getInt32(array, arrayOffset);
    }
    public static long getInt64(Object array, long arrayOffset) {
        return IMPLEMENTATION.getInt64(array, arrayOffset);
    }
    public static byte getChar(Object array, long arrayOffset) {
        return IMPLEMENTATION.getChar(array, arrayOffset);
    }
    public static long getShort(Object array, long arrayOffset) {
        return IMPLEMENTATION.getShort(array, arrayOffset);
    }
    public static long getInt(Object array, long arrayOffset) {
        return IMPLEMENTATION.getInt(array, arrayOffset);
    }
    public static long getLong(Object array, long arrayOffset) {
        return IMPLEMENTATION.getLong(array, arrayOffset);
    }
    public static long getAddress(Object array, long arrayOffset) {
        return IMPLEMENTATION.getAddress(array, arrayOffset);
    }
    public static float getFloat(Object array, long arrayOffset) {
        return IMPLEMENTATION.getFloat(array, arrayOffset);
    }
    public static double getDouble(Object array, long arrayOffset) {
        return IMPLEMENTATION.getDouble(array, arrayOffset);
    }
    public static void setBoolean(Object array, long arrayOffset, boolean value) {
        IMPLEMENTATION.setBoolean(array, arrayOffset, value);
    }
    public static void setInt8(Object array, long arrayOffset, byte value) {
        IMPLEMENTATION.setInt8(array, arrayOffset, value);
    }
    public static void setInt8(Object array, long arrayOffset, int value) {
        IMPLEMENTATION.setInt8(array, arrayOffset, value);
    }
    public static void setInt16(Object array, long arrayOffset, short value) {
        IMPLEMENTATION.setInt16(array, arrayOffset, value);
    }
    public static void setInt16(Object array, long arrayOffset, int value) {
        IMPLEMENTATION.setInt16(array, arrayOffset, value);
    }
    public static void setUTF16(Object array, long arrayOffset, char value) {
        IMPLEMENTATION.setUTF16(array, arrayOffset, value);
    }
    public static void setInt32(Object array, long arrayOffset, int value) {
        IMPLEMENTATION.setInt32(array, arrayOffset, value);
    }
    public static void setInt64(Object array, long arrayOffset, long value) {
        IMPLEMENTATION.setInt64(array, arrayOffset, value);
    }
    public static void setChar(Object array, long arrayOffset, byte value) {
        IMPLEMENTATION.setChar(array, arrayOffset, value);
    }
    public static void setChar(Object array, long arrayOffset, int value) {
        IMPLEMENTATION.setChar(array, arrayOffset, value);
    }
    public static void setShort(Object array, long arrayOffset, long value) {
        IMPLEMENTATION.setShort(array, arrayOffset, value);
    }
    public static void setInt(Object array, long arrayOffset, long value) {
        IMPLEMENTATION.setInt(array, arrayOffset, value);
    }
    public void setLong(Object array, long arrayOffset, long value) {
        IMPLEMENTATION.setLong(array, arrayOffset, value);
    }
    public static void setAddress(Object array, long arrayOffset, long value) {
        IMPLEMENTATION.setAddress(array, arrayOffset, value);
    }
    public static void setFloat(Object array, long arrayOffset, float value) {
        IMPLEMENTATION.setFloat(array, arrayOffset, value);
    }
    public static void setDouble(Object array, long arrayOffset, double value) {
        IMPLEMENTATION.setDouble(array, arrayOffset, value);
    }

    public static void getBooleanArray(Object memoryArray, long memoryArrayOffset, boolean[] array, int index, int length) {
        IMPLEMENTATION.getBooleanArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getBooleanArray(Object memoryArray, long memoryArrayOffset, boolean[] array) {
        IMPLEMENTATION.getBooleanArray(memoryArray, memoryArrayOffset, array);
    }
    public static void setBooleanArray(Object memoryArray, long memoryArrayOffset, boolean[] array, int index, int length) {
        IMPLEMENTATION.setBooleanArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setBooleanArray(Object memoryArray, long memoryArrayOffset, boolean[] array) {
        IMPLEMENTATION.setBooleanArray(memoryArray, memoryArrayOffset, array);
    }
    public static void getInt8Array(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        IMPLEMENTATION.getInt8Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getInt8Array(Object memoryArray, long memoryArrayOffset, byte[] array) {
        IMPLEMENTATION.getInt8Array(memoryArray, memoryArrayOffset, array);
    }
    public static void setInt8Array(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        IMPLEMENTATION.setInt8Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setInt8Array(Object memoryArray, long memoryArrayOffset, byte[] array) {
        IMPLEMENTATION.setInt8Array(memoryArray, memoryArrayOffset, array);
    }
    public static void getInt16Array(Object memoryArray, long memoryArrayOffset, short[] array, int index, int length) {
        IMPLEMENTATION.getInt16Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getInt16Array(Object memoryArray, long memoryArrayOffset, short[] array) {
        IMPLEMENTATION.getInt16Array(memoryArray, memoryArrayOffset, array);
    }
    public static void setInt16Array(Object memoryArray, long memoryArrayOffset, short[] array, int index, int length) {
        IMPLEMENTATION.setInt16Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setInt16Array(Object memoryArray, long memoryArrayOffset, short[] array) {
        IMPLEMENTATION.setInt16Array(memoryArray, memoryArrayOffset, array);
    }
    public static void getUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array, int index, int length) {
        IMPLEMENTATION.getUTF16Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array) {
        IMPLEMENTATION.getUTF16Array(memoryArray, memoryArrayOffset, array);
    }
    public static void setUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array, int index, int length) {
        IMPLEMENTATION.setUTF16Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array) {
        IMPLEMENTATION.setUTF16Array(memoryArray, memoryArrayOffset, array);
    }
    public static void getInt32Array(Object memoryArray, long memoryArrayOffset, int[] array, int index, int length) {
        IMPLEMENTATION.getInt32Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getInt32Array(Object memoryArray, long memoryArrayOffset, int[] array) {
        IMPLEMENTATION.getInt32Array(memoryArray, memoryArrayOffset, array);
    }
    public static void setInt32Array(Object memoryArray, long memoryArrayOffset, int[] array, int index, int length) {
        IMPLEMENTATION.setInt32Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setInt32Array(Object memoryArray, long memoryArrayOffset, int[] array) {
        IMPLEMENTATION.setInt32Array(memoryArray, memoryArrayOffset, array);
    }
    public static void getInt64Array(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        IMPLEMENTATION.getInt64Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getInt64Array(Object memoryArray, long memoryArrayOffset, long[] array) {
        IMPLEMENTATION.getInt64Array(memoryArray, memoryArrayOffset, array);
    }
    public static void setInt64Array(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        IMPLEMENTATION.setInt64Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setInt64Array(Object memoryArray, long memoryArrayOffset, long[] array) {
        IMPLEMENTATION.setInt64Array(memoryArray, memoryArrayOffset, array);
    }
    public static void getFloatArray(Object memoryArray, long memoryArrayOffset, float[] array, int index, int length) {
        IMPLEMENTATION.getFloatArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getFloatArray(Object memoryArray, long memoryArrayOffset, float[] array) {
        IMPLEMENTATION.getFloatArray(memoryArray, memoryArrayOffset, array);
    }
    public static void setFloatArray(Object memoryArray, long memoryArrayOffset, float[] array, int index, int length) {
        IMPLEMENTATION.setFloatArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setFloatArray(Object memoryArray, long memoryArrayOffset, float[] array) {
        IMPLEMENTATION.setFloatArray(memoryArray, memoryArrayOffset, array);
    }
    public static void getDoubleArray(Object memoryArray, long memoryArrayOffset, double[] array, int index, int length) {
        IMPLEMENTATION.getDoubleArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getDoubleArray(Object memoryArray, long memoryArrayOffset, double[] array) {
        IMPLEMENTATION.getDoubleArray(memoryArray, memoryArrayOffset, array);
    }
    public static void setDoubleArray(Object memoryArray, long memoryArrayOffset, double[] array, int index, int length) {
        IMPLEMENTATION.setDoubleArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setDoubleArray(Object memoryArray, long memoryArrayOffset, double[] array) {
        IMPLEMENTATION.setDoubleArray(memoryArray, memoryArrayOffset, array);
    }
    public static void getCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        IMPLEMENTATION.getCharArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getCharArray(Object memoryArray, long memoryArrayOffset, byte[] array) {
        IMPLEMENTATION.getCharArray(memoryArray, memoryArrayOffset, array);
    }
    public static void setCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        IMPLEMENTATION.setCharArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setCharArray(Object memoryArray, long memoryArrayOffset, byte[] array) {
        IMPLEMENTATION.setCharArray(memoryArray, memoryArrayOffset, array);
    }
    public static void getShortArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        IMPLEMENTATION.getShortArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getShortArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        IMPLEMENTATION.getShortArray(memoryArray, memoryArrayOffset, array);
    }
    public static void setShortArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        IMPLEMENTATION.setShortArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setShortArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        IMPLEMENTATION.setShortArray(memoryArray, memoryArrayOffset, array);
    }
    public static void getIntArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        IMPLEMENTATION.getIntArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getIntArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        IMPLEMENTATION.getIntArray(memoryArray, memoryArrayOffset, array);
    }
    public static void setIntArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        IMPLEMENTATION.setIntArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setIntArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        IMPLEMENTATION.setIntArray(memoryArray, memoryArrayOffset, array);
    }
    public static void getLongArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        IMPLEMENTATION.getLongArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getLongArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        IMPLEMENTATION.getLongArray(memoryArray, memoryArrayOffset, array);
    }
    public static void setLongArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        IMPLEMENTATION.setLongArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setLongArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        IMPLEMENTATION.setLongArray(memoryArray, memoryArrayOffset, array);
    }
    public static void getAddressArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        IMPLEMENTATION.getAddressArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void getAddressArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        IMPLEMENTATION.getAddressArray(memoryArray, memoryArrayOffset, array);
    }
    public static void setAddressArray(Object memoryArray, long memoryArrayOffset, long[] array, int index, int length) {
        IMPLEMENTATION.setAddressArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setAddressArray(Object memoryArray, long memoryArrayOffset, long[] array) {
        IMPLEMENTATION.setAddressArray(memoryArray, memoryArrayOffset, array);
    }
    public static long getZeroTerminatedStringLength(Object memoryArray, long memoryArrayOffset) {
        return IMPLEMENTATION.getZeroTerminatedStringLength(memoryArray, memoryArrayOffset);
    }
    public static long getZeroTerminatedStringLength(Object memoryArray, long memoryArrayOffset, long maxLength) {
        return IMPLEMENTATION.getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, maxLength);
    }
    public static long getZeroTerminatedWideStringLength(Object memoryArray, long memoryArrayOffset) {
        return IMPLEMENTATION.getZeroTerminatedWideStringLength(memoryArray, memoryArrayOffset);
    }
    public static long getZeroTerminatedWideStringLength(Object memoryArray, long memoryArrayOffset, long maxLength) {
        return IMPLEMENTATION.getZeroTerminatedWideStringLength(memoryArray, memoryArrayOffset, maxLength);
    }
    public static long getZeroTerminatedStringLength(Object memoryArray, long memoryArrayOffset, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, charset);
    }
    public static long getZeroTerminatedStringLength(Object memoryArray, long memoryArrayOffset, long maxLength, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedStringLength(memoryArray, memoryArrayOffset, maxLength, charset);
    }
    public static long getZeroTerminatedUTF16StringLength(Object memoryArray, long memoryArrayOffset) {
        return IMPLEMENTATION.getZeroTerminatedUTF16StringLength(memoryArray, memoryArrayOffset);
    }
    public static long getZeroTerminatedUTF16StringLength(Object memoryArray, long memoryArrayOffset, long maxLength) {
        return IMPLEMENTATION.getZeroTerminatedUTF16StringLength(memoryArray, memoryArrayOffset, maxLength);
    }
    public static byte[] getZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset) {
        return IMPLEMENTATION.getZeroTerminatedCharArray(memoryArray, memoryArrayOffset);
    }
    public static byte[] getZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedCharArray(memoryArray, memoryArrayOffset, maxLength);
    }
    public static byte[] getZeroTerminatedWideCharArray(Object memoryArray, long memoryArrayOffset) {
        return IMPLEMENTATION.getZeroTerminatedWideCharArray(memoryArray, memoryArrayOffset);
    }
    public static byte[] getZeroTerminatedWideCharArray(Object memoryArray, long memoryArrayOffset, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedWideCharArray(memoryArray, memoryArrayOffset, maxLength);
    }
    public static byte[] getZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedCharArray(memoryArray, memoryArrayOffset, charset);
    }
    public static byte[] getZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, int maxLength, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedCharArray(memoryArray, memoryArrayOffset, maxLength, charset);
    }
    public static char[] getZeroTerminatedUTF16Array(Object memoryArray, long memoryArrayOffset) {
        return IMPLEMENTATION.getZeroTerminatedUTF16Array(memoryArray, memoryArrayOffset);
    }
    public static char[] getZeroTerminatedUTF16Array(Object memoryArray, long memoryArrayOffset, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedUTF16Array(memoryArray, memoryArrayOffset, maxLength);
    }
    public static String getZeroTerminatedString(Object memoryArray, long memoryArrayOffset) {
        return IMPLEMENTATION.getZeroTerminatedString(memoryArray, memoryArrayOffset);
    }
    public static String getZeroTerminatedString(Object memoryArray, long memoryArrayOffset, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedString(memoryArray, memoryArrayOffset, maxLength);
    }
    public static String getZeroTerminatedWideString(Object memoryArray, long memoryArrayOffset) {
        return IMPLEMENTATION.getZeroTerminatedWideString(memoryArray, memoryArrayOffset);
    }
    public static String getZeroTerminatedWideString(Object memoryArray, long memoryArrayOffset, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedWideString(memoryArray, memoryArrayOffset, maxLength);
    }
    public static String getZeroTerminatedString(Object memoryArray, long memoryArrayOffset, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedString(memoryArray, memoryArrayOffset, charset);
    }
    public static String getZeroTerminatedString(Object memoryArray, long memoryArrayOffset, int maxLength, Charset charset) {
        return IMPLEMENTATION.getZeroTerminatedString(memoryArray, memoryArrayOffset, maxLength, charset);
    }
    public static String getZeroTerminatedUTF16String(Object memoryArray, long memoryArrayOffset) {
        return IMPLEMENTATION.getZeroTerminatedUTF16String(memoryArray, memoryArrayOffset);
    }
    public static String getZeroTerminatedUTF16String(Object memoryArray, long memoryArrayOffset, int maxLength) {
        return IMPLEMENTATION.getZeroTerminatedUTF16String(memoryArray, memoryArrayOffset, maxLength);
    }
    public static void setZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, byte[] array) {
        IMPLEMENTATION.setZeroTerminatedCharArray(memoryArray, memoryArrayOffset, array);
    }
    public static void setZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedCharArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setZeroTerminatedWideCharArray(Object memoryArray, long memoryArrayOffset, byte[] array) {
        IMPLEMENTATION.setZeroTerminatedWideCharArray(memoryArray, memoryArrayOffset, array);
    }
    public static void setZeroTerminatedWideCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedWideCharArray(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, Charset charset) {
        IMPLEMENTATION.setZeroTerminatedCharArray(memoryArray, memoryArrayOffset, array, charset);
    }
    public static void setZeroTerminatedCharArray(Object memoryArray, long memoryArrayOffset, byte[] array, int index, int length, Charset charset) {
        IMPLEMENTATION.setZeroTerminatedCharArray(memoryArray, memoryArrayOffset, array, index, length, charset);
    }
    public static void setZeroTerminatedUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array) {
        IMPLEMENTATION.setZeroTerminatedUTF16Array(memoryArray, memoryArrayOffset, array);
    }
    public static void setZeroTerminatedUTF16Array(Object memoryArray, long memoryArrayOffset, char[] array, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedUTF16Array(memoryArray, memoryArrayOffset, array, index, length);
    }
    public static void setZeroTerminatedString(Object memoryArray, long memoryArrayOffset, String string) {
        IMPLEMENTATION.setZeroTerminatedString(memoryArray, memoryArrayOffset, string);
    }
    public static void setZeroTerminatedString(Object memoryArray, long memoryArrayOffset, String string, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedString(memoryArray, memoryArrayOffset, string, index, length);
    }
    public static void setZeroTerminatedWideString(Object memoryArray, long memoryArrayOffset, String string) {
        IMPLEMENTATION.setZeroTerminatedWideString(memoryArray, memoryArrayOffset, string);
    }
    public static void setZeroTerminatedWideString(Object memoryArray, long memoryArrayOffset, String string, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedWideString(memoryArray, memoryArrayOffset, string, index, length);
    }
    public static void setZeroTerminatedString(Object memoryArray, long memoryArrayOffset, String string, Charset charset) {
        IMPLEMENTATION.setZeroTerminatedString(memoryArray, memoryArrayOffset, string, charset);
    }
    public static void setZeroTerminatedString(Object memoryArray, long memoryArrayOffset, String string, int index, int length, Charset charset) {
        IMPLEMENTATION.setZeroTerminatedString(memoryArray, memoryArrayOffset, string, index, length);
    }
    public static void setZeroTerminatedUTF16String(Object memoryArray, long memoryArrayOffset, String string) {
        IMPLEMENTATION.setZeroTerminatedUTF16String(memoryArray, memoryArrayOffset, string);
    }
    public static void setZeroTerminatedUTF16String(Object memoryArray, long memoryArrayOffset, String string, int index, int length) {
        IMPLEMENTATION.setZeroTerminatedUTF16String(memoryArray, memoryArrayOffset, string, index, length);
    }
    
}
