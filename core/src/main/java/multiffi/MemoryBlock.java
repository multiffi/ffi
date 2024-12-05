package multiffi;

import io.github.multiffi.DirectMemoryBlock;
import io.github.multiffi.HeapMemoryBlock;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;

/**
 * A wrapper class which represents a block of memory.
 * 
 * <p>This class provides operations on a block of memory.
 * {@code MemoryBlock} instances will either represent direct
 * memory (that is, a fixed address in the process address space,
 * directly accessible by native code), or backed by at least one
 * Java <code>byte</code> array.
 * See {@link #isDirect()}, {@link #hasArray()} for more information.
 */
public abstract class MemoryBlock implements Comparable<MemoryBlock>, AutoCloseable {

    public static final MemoryBlock NULL = MemoryBlock.wrap(0);

    /**
     * Wraps a Java {@code byte} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(byte[] array) {
        return wrap(array, 0, array.length);
    }

    /**
     * Wraps a Java {@code byte} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     * @param offset the offset of the array.
     * @param length the region length of the array.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(byte[] array, int offset, int length) {
        return new HeapMemoryBlock(array, offset, length);
    }

    /**
     * Wraps a Java {@code char} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(char[] array) {
        return wrap(array, 0, array.length);
    }

    /**
     * Wraps a Java {@code char} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     * @param offset the offset of the array.
     * @param length the region length of the array.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(char[] array, int offset, int length) {
        return new HeapMemoryBlock(array, (long) offset << 1, (long) length << 1);
    }

    /**
     * Wraps a Java {@code short} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(short[] array) {
        return wrap(array, 0, array.length);
    }

    /**
     * Wraps a Java {@code short} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     * @param offset the offset of the array.
     * @param length the region length of the array.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(short[] array, int offset, int length) {
        return new HeapMemoryBlock(array, (long) offset << 1, (long) length << 1);
    }

    /**
     * Wraps a Java {@code int} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(int[] array) {
        return wrap(array, 0, array.length);
    }

    /**
     * Wraps a Java {@code int} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     * @param offset the offset of the array.
     * @param length the region length of the array.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(int[] array, int offset, int length) {
        return new HeapMemoryBlock(array, (long) offset << 2, (long) length << 2);
    }

    /**
     * Wraps a Java {@code long} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(long[] array) {
        return wrap(array, 0, array.length);
    }

    /**
     * Wraps a Java {@code long} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     * @param offset the offset of the array.
     * @param length the region length of the array.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(long[] array, int offset, int length) {
        return new HeapMemoryBlock(array, (long) offset << 3, (long) length << 3);
    }

    /**
     * Wraps a Java {@code float} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(float[] array) {
        return wrap(array, 0, array.length);
    }

    /**
     * Wraps a Java {@code float} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     * @param offset the offset of the array.
     * @param length the region length of the array.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(float[] array, int offset, int length) {
        return new HeapMemoryBlock(array, (long) offset << 2, (long) length << 2);
    }

    /**
     * Wraps a Java {@code double} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(double[] array) {
        return wrap(array, 0, array.length);
    }

    /**
     * Wraps a Java {@code double} array in a {@link MemoryBlock} instance.
     *
     * @param array the {@code array} to wrap in a {@code MemoryBlock} instance.
     * @param offset the offset of the array.
     * @param length the region length of the array.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(double[] array, int offset, int length) {
        return new HeapMemoryBlock(array, (long) offset << 3, (long) length << 3);
    }

    /**
     * Wraps a native address in a {@link MemoryBlock} instance.
     *
     * @param address the {@code address} to wrap in a {@code MemoryBlock} instance.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(long address) {
        return wrap(address, -1);
    }

    /**
     * Wraps a native address in a {@link MemoryBlock} instance.
     *
     * @param address the {@code address} to wrap in a MemoryBlock instance.
     * @param size the size of the native memory region.
     *
     * @return a {@code MemoryBlock} instance.
     */
    public static MemoryBlock wrap(long address, long size) {
        return new DirectMemoryBlock(address, size);
    }

    /**
     * Wraps an existing ByteBuffer in a {@link MemoryBlock} implementation so it can
     * be used as a parameter to native functions.
     *
     * <p>Wrapping a ByteBuffer is only necessary if the native function parameter
     * was declared as a {@code MemoryBlock}.  The if the method will always be used
     * with {@code ByteBuffer} parameters, then the parameter type can just be declared
     * as {@code ByteBuffer} and the conversion will be performed automatically.
     *
     * @param buffer the {@code ByteBuffer} to wrap.
     *
     * @return a {@code MemoryBlock} instance that will proxy all accesses to the ByteBuffer contents.
     */
    public static MemoryBlock wrap(ByteBuffer buffer) {
        if (buffer.isDirect()) return wrap(Buffers.address(buffer), buffer.capacity());
        else return wrap((byte[]) Buffers.array(buffer), Buffers.arrayOffset(buffer), buffer.capacity());
    }

    /**
     * Wraps an existing CharBuffer in a {@link MemoryBlock} implementation so it can
     * be used as a parameter to native functions.
     *
     * <p>Wrapping a CharBuffer is only necessary if the native function parameter
     * was declared as a {@code MemoryBlock}.  The if the method will always be used
     * with {@code CharBuffer} parameters, then the parameter type can just be declared
     * as {@code CharBuffer} and the conversion will be performed automatically.
     *
     * @param buffer the {@code CharBuffer} to wrap.
     *
     * @return a {@code MemoryBlock} instance that will proxy all accesses to the CharBuffer contents.
     */
    public static MemoryBlock wrap(CharBuffer buffer) {
        if (buffer.isDirect()) return wrap(Buffers.address(buffer), buffer.capacity());
        else return wrap((char[]) Buffers.array(buffer), Buffers.arrayOffset(buffer), buffer.capacity());
    }

    public static MemoryBlock wrap(ShortBuffer buffer) {
        if (buffer.isDirect()) return wrap(Buffers.address(buffer), buffer.capacity());
        else return wrap((short[]) Buffers.array(buffer), Buffers.arrayOffset(buffer), buffer.capacity());
    }

    public static MemoryBlock wrap(IntBuffer buffer) {
        if (buffer.isDirect()) return wrap(Buffers.address(buffer), buffer.capacity());
        else return wrap((int[]) Buffers.array(buffer), Buffers.arrayOffset(buffer), buffer.capacity());
    }

    public static MemoryBlock wrap(LongBuffer buffer) {
        if (buffer.isDirect()) return wrap(Buffers.address(buffer), buffer.capacity());
        else return wrap((long[]) Buffers.array(buffer), Buffers.arrayOffset(buffer), buffer.capacity());
    }

    public static MemoryBlock wrap(FloatBuffer buffer) {
        if (buffer.isDirect()) return wrap(Buffers.address(buffer), buffer.capacity());
        else return wrap((float[]) Buffers.array(buffer), Buffers.arrayOffset(buffer), buffer.capacity());
    }

    public static MemoryBlock wrap(DoubleBuffer buffer) {
        if (buffer.isDirect()) return wrap(Buffers.address(buffer), buffer.capacity());
        else return wrap((double[]) Buffers.array(buffer), Buffers.arrayOffset(buffer), buffer.capacity());
    }

    public static MemoryBlock allocate(long size) {
        if (size < Integer.MIN_VALUE || size > (Integer.MAX_VALUE - 8)) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(size));
        return wrap(new byte[(int) size]);
    }

    public static MemoryBlock allocate(CharSequence string) {
        return allocate(string, null);
    }

    public static MemoryBlock allocate(CharSequence string, Charset charset) {
        if (charset == null) charset = Foreign.ansiCharset();
        byte[] bytes;
        if (string instanceof String) bytes = ((String) string).getBytes(charset);
        else bytes = charset.encode(CharBuffer.wrap(string)).array();
        byte[] terminator = "\0".getBytes(charset);
        MemoryBlock memoryBlock = allocate(bytes.length + terminator.length);
        memoryBlock.setInt8Array(bytes.length, terminator);
        memoryBlock.setInt8Array(0, bytes);
        return memoryBlock;
    }

    public static MemoryBlock allocateDirect(long size) {
        return wrap(Allocator.allocate(size), size);
    }

    public static MemoryBlock allocateDirect(CharSequence string) {
        return allocateDirect(string, null);
    }

    public static MemoryBlock allocateDirect(CharSequence string, Charset charset) {
        if (charset == null) charset = Foreign.ansiCharset();
        byte[] bytes;
        if (string instanceof String) bytes = ((String) string).getBytes(charset);
        else bytes = charset.encode(CharBuffer.wrap(string)).array();
        byte[] terminator = "\0".getBytes(charset);
        MemoryBlock memoryBlock = allocateDirect(bytes.length + terminator.length);
        memoryBlock.setInt8Array(bytes.length, terminator);
        memoryBlock.setInt8Array(0, bytes);
        return memoryBlock;
    }

    public static MemoryBlock allocate(ForeignType type) {
        return allocate(type.size());
    }

    public static MemoryBlock allocateDirect(ForeignType type) {
        return allocateDirect(type.size());
    }

    /**
     * Indicates whether this memory object represents a native memory address.
     *
     * <p>Memory objects can be either direct (representing native memory), or
     * non-direct (representing java heap memory).
     *
     * @return true if, and only if, this memory object represents a native address.
     */
    public abstract boolean isDirect();

    /**
     * Gets the native address of this memory object (optional operation).
     *
     * @return the native address of this memory object.  If this object is not
     * a native memory address, an address of zero is returned.
     */
    public abstract long address();

    @Override
    public String toString() {
        if (isDirect()) {
            long size = size();
            if (size == -1) {
                return getClass().getName()
                        + '{' +
                        "address=" + address() +
                        ", hasMemory=" + hasMemory() +
                        '}';
            }
            else {
                return getClass().getName()
                        + '{' +
                        "address=" + address() +
                        ", size=" + Long.toUnsignedString(size) +
                        ", hasMemory=" + hasMemory() +
                        '}';
            }
        }
        else {
            long size = size();
            if (size == -1) {
                return getClass().getName()
                        + '{' +
                        "array=" + array() +
                        ", arrayOffset=" + arrayOffset() +
                        ", arrayLength=" + arrayLength() +
                        ", hasMemory=" + hasMemory() +
                        '}';
            }
            else {
                return getClass().getName()
                        + '{' +
                        "array=" + array() +
                        ", arrayOffset=" + arrayOffset() +
                        ", arrayLength=" + arrayLength() +
                        ", size=" + size +
                        ", hasMemory=" + hasMemory() +
                        '}';
            }
        }
    }

    /**
     * Gets the size of this memory object in bytes (optional operation).
     *
     * @return the size of the memory area this {@code MemoryBlock} points to.  If
     * the size is unknown, return -1.
     */
    public abstract long size();

    /**
     * Indicates whether the memory region size of this <code>MemoryBlock</code> instance is known.
     *
     * @return true if, and only if, the memory region size of this object is known.
     */
    public abstract boolean isBounded();

    /**
     * Indicates whether this <code>MemoryBlock</code> instance is backed by array.
     *
     * @return true if, and only if, this memory object is backed by an array
     */
    public abstract boolean hasArray();

    /**
     * Returns the array that back this pointer.
     *
     * @return The array that back this pointer.
     * @throws UnsupportedOperationException if this pointer does not have a backing array.
     */
    public abstract Object array();

    /**
     * Returns the offset in bytes within this pointer's backing array of the first element.
     *
     * @throws UnsupportedOperationException if this pointer does not have a backing array
     * @return The offset of the first element on the backing array
     */
    public abstract long arrayOffset();

    /**
     * Returns the length in bytes of this pointer's backing array that is used by this pointer.
     *
     * @throws UnsupportedOperationException if this pointer does not have a backing array
     * @return The length of the backing array used
     */
    public abstract long arrayLength();

    /**
     * Reads an {@code boolean} (8 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code boolean} value at the offset.
     */
    public abstract boolean getBoolean(long offset);

    /**
     * Reads an {@code byte} (8 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code byte} value at the offset.
     */
    public abstract byte getInt8(long offset);

    /**
     * Reads a {@code short} (16 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code short} value at the offset.
     */
    public abstract short getInt16(long offset);

    /**
     * Reads a {@code char} (16 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code char} value at the offset.
     */
    public abstract char getUTF16(long offset);

    /**
     * Reads an {@code int} (32 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code int} value contained in the memory at the offset.
     */
    public abstract int getInt32(long offset);

    /**
     * Reads a {@code long} (64 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code long} value at the offset.
     */
    public abstract long getInt64(long offset);

    /**
     * Reads a native {@code char} (8-bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the native {@code char} value at the offset.
     */
    public byte getChar(long offset) {
        return getInt8(offset);
    }

    /**
     * Reads a native {@code short} (16-bit or 64-bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the native {@code short} value at the offset.
     */
    public long getShort(long offset) {
        return ValueAdapter.SHORT.get(this, offset);
    }

    /**
     * Reads a native {@code int} (32-bit or 64-bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the native {@code int} value at the offset.
     */
    public long getInt(long offset) {
        return ValueAdapter.INT.get(this, offset);
    }

    /**
     * Reads a native {@code long} (32-bit or 64-bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the native {@code long} value at the offset.
     */
    public long getLong(long offset) {
        return ValueAdapter.LONG.get(this, offset);
    }

    /**
     * Reads a native memory address value at the given offset.
     * <p>A native address can be either 32 or 64 bits in size, depending
     * on the cpu architecture.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the native address value contained in the memory at the offset
     */
    public long getAddress(long offset) {
        return ValueAdapter.ADDRESS.get(this, offset);
    }

    /**
     * Reads a {@code float} (32 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code float} value at the offset.
     */
    public abstract float getFloat(long offset);

    /**
     * Reads a {@code double} (64 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code double} value at the offset.
     */
    public abstract double getDouble(long offset);

    /**
     * Writes a {@code boolean} (8 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the {@code boolean} value to be written.
     */
    public abstract void setBoolean(long offset, boolean value);

    /**
     * Writes a {@code byte} (8 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the {@code byte} value to be written.
     */
    public abstract void setInt8(long offset, byte value);

    /**
     * Writes a {@code byte} (8 bit) value at the given offset.
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the {@code byte} value to be written.
     */
    public void setInt8(long offset, int value) {
        setInt8(offset, (byte) value);
    }

    /**
     * Writes a {@code short} (16 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the {@code short} value to be written.
     */
    public abstract void setInt16(long offset, short value);

    /**
     * Writes a {@code short} (16 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the {@code short} value to be written.
     */
    public void setInt16(long offset, int value) {
        setInt16(offset, (short) value);
    }

    /**
     * Writes a {@code char} (16 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the {@code char} value to be written.
     */
    public abstract void setUTF16(long offset, char value);

    /**
     * Writes an {@code int} (32 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the {@code int} value to be written.
     */
    public abstract void setInt32(long offset, int value);

    /**
     * Writes a {@code long} value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the {@code long} value to be written.
     */
    public abstract void setInt64(long offset, long value);

    /**
     * Writes a native {@code char} (8 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the native {@code char} value to be written.
     */
    public void setChar(long offset, byte value) {
        setInt8(offset, value);
    }

    /**
     * Writes a native {@code char} (8 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the native {@code char} value to be written.
     */
    public void setChar(long offset, int value) {
        setInt8(offset, value);
    }

    /**
     * Writes a native {@code short} value at the given offset.
     *
     * <p>A native {@code short} can be either 16 or 64 bits in size, depending
     * on the cpu architecture, and the C ABI in use.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the native {@code short} value to be written.
     */
    public void setShort(long offset, long value) {
        ValueAdapter.SHORT.set(this, offset, value);
    }

    /**
     * Writes a native {@code int} value at the given offset.
     *
     * <p>A native {@code int} can be either 32 or 64 bits in size, depending
     * on the cpu architecture, and the C ABI in use.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the native {@code int} value to be written.
     */
    public void setInt(long offset, long value) {
        ValueAdapter.INT.set(this, offset, value);
    }

    /**
     * Writes a native {@code long} value at the given offset.
     *
     * <p>A native {@code long} can be either 32 or 64 bits in size, depending
     * on the cpu architecture, and the C ABI in use.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the native {@code long} value to be written.
     */
    public void setLong(long offset, long value) {
        ValueAdapter.LONG.set(this, offset, value);
    }

    /**
     * Writes a native memory address value at the given offset.
     * <p>A native address can be either 32 or 64 bits in size, depending
     * on the cpu architecture.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value The native address value to be written.
     */
    public void setAddress(long offset, long value) {
        ValueAdapter.ADDRESS.set(this, offset, value);
    }

    /**
     * Writes a {@code float} (32 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the {@code float} value to be written.
     */
    public abstract void setFloat(long offset, float value);

    /**
     * Writes a {@code double} (64 bit) value at the given offset.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param value the {@code double} value to be written.
     */
    public abstract void setDouble(long offset, double value);

    /**
     * Bulk get method for multiple {@code boolean} values.
     *
     * <p>This method reads multiple {@code boolean} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array the array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getBooleanArray(long offset, boolean[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, length);
        for (int i = 0; i < length; i ++) {
            array[index + i] = getBoolean(offset + i);
        }
    }

    /**
     * Bulk get method for multiple {@code boolean} values.
     *
     * <p>This method reads multiple {@code boolean} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array the array into which values are to be stored.
     */
    public void getBooleanArray(long offset, boolean[] array) {
        getBooleanArray(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple {@code boolean} values.
     *
     * <p>This method writes multiple {@code boolean} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setBooleanArray(long offset, boolean[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, length);
        for (int i = 0; i < length; i ++) {
            setBoolean(offset + i, array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple {@code boolean} values.
     *
     * <p>This method writes multiple {@code boolean} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setBooleanArray(long offset, boolean[] array) {
        setBooleanArray(offset, array, 0, array.length);
    }

    /**
     * Bulk get method for multiple {@code byte} values.
     *
     * <p>This method reads multiple {@code byte} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array the array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getInt8Array(long offset, byte[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, length);
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt8(offset + i);
        }
    }

    /**
     * Bulk get method for multiple {@code byte} values.
     *
     * <p>This method reads multiple {@code byte} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array the array into which values are to be stored.
     */
    public void getInt8Array(long offset, byte[] array) {
        getInt8Array(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple {@code byte} values.
     *
     * <p>This method writes multiple {@code byte} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setInt8Array(long offset, byte[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, length);
        for (int i = 0; i < length; i ++) {
            setInt8(offset + i, array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple {@code byte} values.
     *
     * <p>This method writes multiple {@code byte} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setInt8Array(long offset, byte[] array) {
        setInt8Array(offset, array, 0, array.length);
    }

    /**
     * Bulk get method for multiple {@code short} values.
     *
     * <p>This method reads multiple {@code short} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getInt16Array(long offset, short[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 1);
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt16(offset + (long) i << 1);
        }
    }

    /**
     * Bulk get method for multiple {@code short} values.
     *
     * <p>This method reads multiple {@code short} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getInt16Array(long offset, short[] array) {
        getInt16Array(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple {@code short} values.
     *
     * <p>This method writes multiple {@code short} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setInt16Array(long offset, short[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 1);
        for (int i = 0; i < length; i ++) {
            setInt16(offset + (long) i << 1, array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple {@code short} values.
     *
     * <p>This method writes multiple {@code short} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setInt16Array(long offset, short[] array) {
        setInt16Array(offset, array, 0, array.length);
    }

    /**
     * Bulk get method for multiple {@code char} values.
     *
     * <p>This method reads multiple {@code char} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getUTF16Array(long offset, char[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 1);
        for (int i = 0; i < length; i ++) {
            array[index + i] = getUTF16(offset + (long) i << 1);
        }
    }

    /**
     * Bulk get method for multiple {@code char} values.
     *
     * <p>This method reads multiple {@code char} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getUTF16Array(long offset, char[] array) {
        getUTF16Array(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple {@code char} values.
     *
     * <p>This method writes multiple {@code char} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setUTF16Array(long offset, char[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 1);
        for (int i = 0; i < length; i ++) {
            setUTF16(offset + (long) i << 1, array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple {@code char} values.
     *
     * <p>This method writes multiple {@code char} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setUTF16Array(long offset, char[] array) {
        setUTF16Array(offset, array, 0, array.length);
    }

    /**
     * Bulk get method for multiple {@code int} values.
     *
     * <p>This method reads multiple {@code int} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getInt32Array(long offset, int[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 2);
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt32(offset + (long) i << 2);
        }
    }

    /**
     * Bulk get method for multiple {@code int} values.
     *
     * <p>This method reads multiple {@code int} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getInt32Array(long offset, int[] array) {
        getInt32Array(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple {@code int} values.
     *
     * <p>This method writes multiple {@code int} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setInt32Array(long offset, int[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 2);
        for (int i = 0; i < length; i ++) {
            setInt32(offset + (long) i << 2, array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple {@code int} values.
     *
     * <p>This method writes multiple {@code int} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setInt32Array(long offset, int[] array) {
        setInt32Array(offset, array, 0, array.length);
    }

    /**
     * Bulk get method for multiple {@code long} values.
     *
     * <p>This method reads multiple {@code long} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getInt64Array(long offset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 3);
        for (int i = 0; i < length; i ++) {
            array[index + i] = getInt64(offset + (long) i << 3);
        }
    }

    /**
     * Bulk get method for multiple {@code long} values.
     *
     * <p>This method reads multiple {@code long} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getInt64Array(long offset, long[] array) {
        getInt64Array(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple {@code long} values.
     *
     * <p>This method writes multiple {@code long} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setInt64Array(long offset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 3);
        for (int i = 0; i < length; i ++) {
            setInt64(offset + (long) i << 3, array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple {@code long} values.
     *
     * <p>This method writes multiple {@code long} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setInt64Array(long offset, long[] array) {
        setInt64Array(offset, array, 0, array.length);
    }

    /**
     * Bulk get method for multiple {@code float} values.
     *
     * <p>This method reads multiple {@code float} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getFloatArray(long offset, float[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 2);
        for (int i = 0; i < length; i ++) {
            array[index + i] = getFloat(offset + (long) i << 2);
        }
    }

    /**
     * Bulk get method for multiple {@code float} values.
     *
     * <p>This method reads multiple {@code float} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getFloatArray(long offset, float[] array) {
        getFloatArray(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple {@code float} values.
     *
     * <p>This method writes multiple {@code float} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setFloatArray(long offset, float[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 2);
        for (int i = 0; i < length; i ++) {
            setFloat(offset + (long) i << 2, array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple {@code float} values.
     *
     * <p>This method writes multiple {@code float} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setFloatArray(long offset, float[] array) {
        setFloatArray(offset, array, 0, array.length);
    }

    /**
     * Bulk get method for multiple {@code double} values.
     *
     * <p>This method reads multiple {@code double} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getDoubleArray(long offset, double[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 3);
        for (int i = 0; i < length; i ++) {
            array[index + i] = getDouble(offset + (long) i << 3);
        }
    }

    /**
     * Bulk get method for multiple {@code double} values.
     *
     * <p>This method reads multiple {@code double} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getDoubleArray(long offset, double[] array) {
        getDoubleArray(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple {@code double} values.
     *
     * <p>This method writes multiple {@code double} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setDoubleArray(long offset, double[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length << 3);
        for (int i = 0; i < length; i ++) {
            setDouble(offset + (long) i << 3, array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple {@code double} values.
     *
     * <p>This method writes multiple {@code double} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setDoubleArray(long offset, double[] array) {
        setDoubleArray(offset, array, 0, array.length);
    }
    
    /**
     * Bulk get method for multiple native {@code char} values.
     *
     * <p>This method reads multiple native {@code char} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getCharArray(long offset, byte[] array, int index, int length) {
        getInt8Array(offset, array, index, length);
    }

    /**
     * Bulk get method for multiple native {@code char} values.
     *
     * <p>This method reads multiple native {@code char} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getCharArray(long offset, byte[] array) {
        getInt8Array(offset, array);
    }
    
    /**
     * Bulk set method for multiple native {@code char} values.
     *
     * <p>This method writes multiple native {@code char} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setCharArray(long offset, byte[] array, int index, int length) {
        setInt8Array(offset, array, index, length);
    }

    /**
     * Bulk set method for multiple native {@code char} values.
     *
     * <p>This method writes multiple native {@code char} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setCharArray(long offset, byte[] array) {
        setInt8Array(offset, array);
    }

    /**
     * Bulk get method for multiple native {@code short} values.
     *
     * <p>This method reads multiple native {@code short} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getShortArray(long offset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length * Foreign.shortSize());
        for (int i = 0; i < length; i ++) {
            array[index + i] = ValueAdapter.SHORT.get(this, offset + (long) i * Foreign.shortSize());
        }
    }

    /**
     * Bulk get method for multiple native {@code short} values.
     *
     * <p>This method reads multiple native {@code short} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getShortArray(long offset, long[] array) {
        getShortArray(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple native {@code short} values.
     *
     * <p>This method writes multiple native {@code short} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setShortArray(long offset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length * Foreign.shortSize());
        for (int i = 0; i < length; i ++) {
            ValueAdapter.SHORT.set(this, offset + (long) i * Foreign.shortSize(), array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple native {@code short} values.
     *
     * <p>This method writes multiple native {@code short} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setShortArray(long offset, long[] array) {
        setShortArray(offset, array, 0, array.length);
    }

    /**
     * Bulk get method for multiple native {@code int} values.
     *
     * <p>This method reads multiple native {@code int} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getIntArray(long offset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length * Foreign.intSize());
        for (int i = 0; i < length; i ++) {
            array[index + i] = ValueAdapter.INT.get(this, offset + (long) i * Foreign.intSize());
        }
    }

    /**
     * Bulk get method for multiple native {@code int} values.
     *
     * <p>This method reads multiple native {@code int} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getIntArray(long offset, long[] array) {
        getIntArray(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple native {@code int} values.
     *
     * <p>This method writes multiple native {@code int} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setIntArray(long offset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length * Foreign.intSize());
        for (int i = 0; i < length; i ++) {
            ValueAdapter.INT.set(this, offset + (long) i * Foreign.intSize(), array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple native {@code int} values.
     *
     * <p>This method writes multiple native {@code int} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setIntArray(long offset, long[] array) {
        setIntArray(offset, array, 0, array.length);
    }

    /**
     * Bulk get method for multiple native {@code long} values.
     *
     * <p>This method reads multiple {@code long} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getLongArray(long offset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length * Foreign.longSize());
        for (int i = 0; i < length; i ++) {
            array[index + i] = ValueAdapter.LONG.get(this, offset + (long) i * Foreign.longSize());
        }
    }

    /**
     * Bulk get method for multiple native {@code long} values.
     *
     * <p>This method reads multiple {@code long} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getLongArray(long offset, long[] array) {
        getLongArray(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple native {@code long} values.
     *
     * <p>This method writes multiple native {@code long} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setLongArray(long offset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length * Foreign.longSize());
        for (int i = 0; i < length; i ++) {
            ValueAdapter.LONG.set(this, offset + (long) i * Foreign.longSize(), array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple native {@code long} values.
     *
     * <p>This method writes multiple native {@code long} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setLongArray(long offset, long[] array) {
        setLongArray(offset, array, 0, array.length);
    }

    /**
     * Bulk get method for multiple {@code address} values.
     *
     * <p>This method reads multiple {@code address} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     * @param index the start index in the {@code array} array to begin storing the values.
     * @param length the number of values to be read.
     */
    public void getAddressArray(long offset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length * Foreign.addressSize());
        for (int i = 0; i < length; i ++) {
            array[index + i] = ValueAdapter.ADDRESS.get(this, offset + (long) i * Foreign.addressSize());
        }
    }

    /**
     * Bulk get method for multiple {@code address} values.
     *
     * <p>This method reads multiple {@code address} values from consecutive addresses,
     * beginning at the given offset, and stores them in an array.
     *
     * @param offset The offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be read.
     * @param array The array into which values are to be stored.
     */
    public void getAddressArray(long offset, long[] array) {
        getAddressArray(offset, array, 0, array.length);
    }

    /**
     * Bulk set method for multiple {@code address} values.
     *
     * <p>This method writes multiple {@code address} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     * @param index the start index in the {@code array} array to begin reading values.
     * @param length the number of values to be written.
     */
    public void setAddressArray(long offset, long[] array, int index, int length) {
        if (index < 0) throw new ArrayIndexOutOfBoundsException(index);
        else if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
        int size = Math.addExact(index, length);
        if (size > array.length) throw new ArrayIndexOutOfBoundsException(size);
        checkBounds(offset, (long) length * Foreign.addressSize());
        for (int i = 0; i < length; i ++) {
            ValueAdapter.ADDRESS.set(this, offset + (long) i * Foreign.addressSize(), array[index + i]);
        }
    }

    /**
     * Bulk set method for multiple {@code address} values.
     *
     * <p>This method writes multiple {@code address} values to consecutive addresses,
     * beginning at the given offset, from an array.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the first value will be written.
     * @param array the array to get values from.
     */
    public void setAddressArray(long offset, long[] array) {
        setAddressArray(offset, array, 0, array.length);
    }

    public long getZeroTerminatedStringLength(long offset) {
        return getZeroTerminatedStringLength(offset, Limits.ADDRESS_MAX);
    }

    public long getZeroTerminatedStringLength(long offset, long maxLength) {
        return getZeroTerminatedStringLength(offset, maxLength, null);
    }

    public long getZeroTerminatedWideStringLength(long offset) {
        return getZeroTerminatedWideStringLength(offset, Limits.ADDRESS_MAX);
    }

    public long getZeroTerminatedWideStringLength(long offset, long maxLength) {
        return getZeroTerminatedStringLength(offset, maxLength, Foreign.wideCharset());
    }

    public long getZeroTerminatedStringLength(long offset, Charset charset) {
        return getZeroTerminatedStringLength(offset, Limits.ADDRESS_MAX, charset);
    }

    public long getZeroTerminatedStringLength(long offset, long maxLength, Charset charset) {
        byte[] terminator = "\0".getBytes(charset == null ? Foreign.ansiCharset() : charset);
        return indexOf(offset, terminator, 0, terminator.length, maxLength) - offset;
    }

    public long getZeroTerminatedUTF16StringLength(long offset) {
        return getZeroTerminatedUTF16StringLength(offset, Limits.ADDRESS_MAX);
    }

    public long getZeroTerminatedUTF16StringLength(long offset, long maxLength) {
        return getZeroTerminatedStringLength(offset, maxLength, Foreign.utf16Charset());
    }

    public byte[] getZeroTerminatedCharArray(long offset) {
        return getZeroTerminatedCharArray(offset, Integer.MAX_VALUE - 8);
    }

    public byte[] getZeroTerminatedCharArray(long offset, int maxLength) {
        return getZeroTerminatedCharArray(offset, maxLength, null);
    }

    public byte[] getZeroTerminatedWideCharArray(long offset) {
        return getZeroTerminatedWideCharArray(offset, Integer.MAX_VALUE - 8);
    }

    public byte[] getZeroTerminatedWideCharArray(long offset, int maxLength) {
        return getZeroTerminatedCharArray(offset, maxLength, Foreign.wideCharset());
    }

    public byte[] getZeroTerminatedCharArray(long offset, Charset charset) {
        return getZeroTerminatedCharArray(offset, Integer.MAX_VALUE - 8, charset);
    }

    public byte[] getZeroTerminatedCharArray(long offset, int maxLength, Charset charset) {
        int length = (int) getZeroTerminatedStringLength(offset, maxLength, charset);
        byte[] array = new byte[length];
        for (int i = 0; i < length; i ++) {
            array[i] = getChar(offset + i);
        }
        return array;
    }
    
    public char[] getZeroTerminatedUTF16Array(long offset) {
        return getZeroTerminatedUTF16Array(offset, Integer.MAX_VALUE - 8);
    }

    public char[] getZeroTerminatedUTF16Array(long offset, int maxLength) {
        int length = (int) getZeroTerminatedUTF16StringLength(offset, maxLength) >>> 1;
        char[] array = new char[length];
        for (int i = 0; i < length; i ++) {
            array[i] = getUTF16(offset + (long) i << 1);
        }
        return array;
    }

    /**
     * Reads an {@code String} value at the given offset.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code String} value read from memory.
     */
    public String getZeroTerminatedString(long offset) {
        return getZeroTerminatedString(offset, Integer.MAX_VALUE - 8);
    }

    /**
     * Reads a {@code String} value at the given offset, using a specific {@code Charset}
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @param maxLength the maximum size of memory to search for a '\0' character.
     * @return the {@code String} value read from memory.
     */
    public String getZeroTerminatedString(long offset, int maxLength) {
        return getZeroTerminatedString(offset, maxLength, null);
    }

    /**
     * Reads an {@code String} value at the given offset.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code String} value read from memory.
     */
    public String getZeroTerminatedWideString(long offset) {
        return getZeroTerminatedWideString(offset, Integer.MAX_VALUE - 8);
    }

    /**
     * Reads a {@code String} value at the given offset, using a specific {@code Charset}
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @param maxLength the maximum size of memory to search for a '\0' character.
     * @return the {@code String} value read from memory.
     */
    public String getZeroTerminatedWideString(long offset, int maxLength) {
        return getZeroTerminatedString(offset, maxLength, Foreign.wideCharset());
    }

    /**
     * Reads an {@code String} value at the given offset.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @return the {@code String} value read from memory.
     */
    public String getZeroTerminatedString(long offset, Charset charset) {
        return getZeroTerminatedString(offset, Integer.MAX_VALUE - 8, charset);
    }

    /**
     * Reads a {@code String} value at the given offset, using a specific {@code Charset}
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the value will be read.
     * @param maxLength the maximum size of memory to search for a '\0' character.
     * @param charset the {@code Charset} to use to decode the string.
     * @return the {@code String} value read from memory.
     */
    public String getZeroTerminatedString(long offset, int maxLength, Charset charset) {
        return new String(getZeroTerminatedCharArray(offset, maxLength, charset), charset == null ? Foreign.ansiCharset() : charset);
    }

    public String getZeroTerminatedUTF16String(long offset) {
        return getZeroTerminatedUTF16String(offset, Integer.MAX_VALUE - 8);
    }

    public String getZeroTerminatedUTF16String(long offset, int maxLength) {
        return getZeroTerminatedString(offset, maxLength, Foreign.utf16Charset());
    }

    public void setZeroTerminatedCharArray(long offset, byte[] array) {
        setZeroTerminatedCharArray(offset, array, 0, array.length);
    }

    public void setZeroTerminatedCharArray(long offset, byte[] array, int index, int length) {
        setZeroTerminatedCharArray(offset, array, index, length, null);
    }

    public void setZeroTerminatedWideCharArray(long offset, byte[] array) {
        setZeroTerminatedWideCharArray(offset, array, 0, array.length);
    }

    public void setZeroTerminatedWideCharArray(long offset, byte[] array, int index, int length) {
        setZeroTerminatedCharArray(offset, array, index, length, Foreign.wideCharset());
    }

    public void setZeroTerminatedCharArray(long offset, byte[] array, Charset charset) {
        setZeroTerminatedCharArray(offset, array, 0, array.length, charset);
    }

    public void setZeroTerminatedCharArray(long offset, byte[] array, int index, int length, Charset charset) {
        for (int i = 0; i < length; i ++) {
            setChar(offset + i, array[index + i]);
        }
        setCharArray(offset + length, "\0".getBytes(charset == null ? Foreign.ansiCharset() : charset));
    }
    
    public void setZeroTerminatedUTF16Array(long offset, char[] array) {
        setZeroTerminatedUTF16Array(offset, array, 0, array.length);
    }

    public void setZeroTerminatedUTF16Array(long offset, char[] array, int index, int length) {
        for (int i = 0; i < length; i ++) {
            setUTF16(offset + (long) i << 1, array[index + i]);
        }
        setUTF16(offset + (long) length << 1, '\0');
    }

    /**
     * Writes a {@code CharSequence} value at the given offset, using the default {@code Charset}
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param string the string to be written.
     */
    public void setZeroTerminatedString(long offset, CharSequence string) {
        setZeroTerminatedString(offset, string, 0, string.length());
    }

    public void setZeroTerminatedString(long offset, CharSequence string, int index, int length) {
        setZeroTerminatedString(offset, string, index, length, null);
    }

    /**
     * Writes a {@code CharSequence} value at the given offset, using the wide {@code Charset}
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param string the string to be written.
     */
    public void setZeroTerminatedWideString(long offset, CharSequence string) {
        setZeroTerminatedWideString(offset, string, 0, string.length());
    }

    public void setZeroTerminatedWideString(long offset, CharSequence string, int index, int length) {
        setZeroTerminatedString(offset, string, index, length, Foreign.wideCharset());
    }

    /**
     * Writes a {@code CharSequence} value at the given offset, using a specific {@code Charset}
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock} represents at which the value will be written.
     * @param string the string to be written.
     * @param charset the {@code Charset} to use to decode the string.
     */
    public void setZeroTerminatedString(long offset, CharSequence string, Charset charset) {
        setZeroTerminatedString(offset, string, 0, string.length(), charset);
    }

    public void setZeroTerminatedString(long offset, CharSequence string, int index, int length, Charset charset) {
        if (charset == null) charset = Foreign.ansiCharset();
        byte[] bytes;
        if (string instanceof String) bytes = ((String) string).getBytes(charset);
        else bytes = charset.encode(CharBuffer.wrap(string)).array();
        setZeroTerminatedCharArray(offset, bytes, index, length, charset);
    }

    public void setZeroTerminatedUTF16String(long offset, CharSequence string) {
        setZeroTerminatedUTF16String(offset, string, 0, string.length());
    }

    public void setZeroTerminatedUTF16String(long offset, CharSequence string, int index, int length) {
        for (int i = 0; i < length; i ++) {
            setUTF16(offset + (long) i << 1, string.charAt(index + i));
        }
        setUTF16(offset + (long) length << 1, '\0');
    }

    /**
     * Creates a new unbounded {@code MemoryBlock} representing a sub-region of the memory
     * referred to by this {@code MemoryBlock}.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock}
     * represents at which the new {@code MemoryBlock} will start.
     * @return a {@code MemoryBlock} instance representing the new sub-region.
     */
    public abstract MemoryBlock slice(long offset);

    /**
     * Creates a new bounded {@code MemoryBlock} representing a sub-region of the memory
     * referred to by this {@code MemoryBlock}.
     *
     * @param offset the offset from the start of the memory this {@code MemoryBlock}
     * represents at which the new {@code MemoryBlock} will start.
     * @param size the maximum size of the memory sub-region.
     *
     * @return a {@code MemoryBlock} instance representing the new sub-region.
     */
    public abstract MemoryBlock slice(long offset, long size);

    public abstract MemoryBlock duplicate();

    public abstract MemoryBlock attachment();

    public boolean hasAttachment() {
        return attachment() != null;
    }

    /**
     * Bulk data transfer from one memory location to another.
     *
     * @param srcOffset the offset from the start of the memory location this {@code MemoryBlock} represents to begin copying from.
     * @param destMemoryBlock the destination memory location to transfer data to.
     * @param destOffset the offset from the start of the memory location the destination {@code MemoryBlock} represents to begin copying to.
     * @param size the number of bytes to transfer.
     */
    public abstract void transferTo(long srcOffset, MemoryBlock destMemoryBlock, long destOffset, long size);

    /**
     * Bulk data transfer from one memory location to another.
     *
     * @param destMemoryBlock the destination memory location to transfer data to.
     * @param size the number of bytes to transfer.
     */
    public abstract void transferTo(MemoryBlock destMemoryBlock, long size);

    /**
     * Bulk data transfer from one memory location to another.
     *
     * @param destOffset the offset from the start of the memory location this {@code MemoryBlock} represents to begin copying to.
     * @param srcMemoryBlock the destination memory location to transfer data from.
     * @param srcOffset the offset from the start of the memory location the destination {@code MemoryBlock} represents to begin copying from.
     * @param size the number of bytes to transfer.
     */
    public abstract void transferFrom(long destOffset, MemoryBlock srcMemoryBlock, long srcOffset, long size);

    /**
     * Bulk data transfer from one memory location to another.
     *
     * @param srcMemoryBlock the destination memory location to transfer data from.
     * @param size the number of bytes to transfer.
     */
    public abstract void transferFrom(MemoryBlock srcMemoryBlock, long size);

    /**
     * Checks that the memory region is within the bounds of this memory object
     *
     * @param offset the starting point within this memory region.
     * @throws IndexOutOfBoundsException if the memory region is not within the bounds.
     */
    public abstract void checkBounds(long offset) throws IndexOutOfBoundsException;

    /**
     * Checks that the memory region is within the bounds of this memory object
     *
     * @param offset the starting point within this memory region.
     * @param size the length of the memory region in bytes
     * @throws IndexOutOfBoundsException if the memory region is not within the bounds.
     */
    public abstract void checkBounds(long offset, long size) throws IndexOutOfBoundsException;

    /**
     * Checks that the memory region is within the bounds of this memory object
     *
     * @param offset the starting point within this memory region.
     * @throws IndexOutOfBoundsException if the memory region is not within the bounds.
     */
    public abstract boolean inBounds(long offset);

    /**
     * Checks that the memory region is within the bounds of this memory object
     *
     * @param offset the starting point within this memory region.
     * @param size the length of the memory region in bytes
     * @throws IndexOutOfBoundsException if the memory region is not within the bounds.
     */
    public abstract boolean inBounds(long offset, long size);

    /**
     * Sets the value of each byte in the memory area represented by this {@code MemoryBlock}.
     * to a specified value.
     *
     * @param offset the offset from the start of the memory location this {@code MemoryBlock} represents to begin writing to.
     * @param size the number of bytes to set to the value.
     * @param value the value to set each byte to.
     */
    public abstract void fill(long offset, byte value, long size);

    /**
     * Sets the value of each byte in the memory area represented by this {@code MemoryBlock}.
     * to a specified value.
     *
     * @param offset the offset from the start of the memory location this {@code MemoryBlock} represents to begin writing to.
     * @param size the number of bytes to set to the value.
     * @param value the value to set each byte to.
     */
    public abstract void fill(long offset, int value, long size);

    /**
     * Returns the location of a byte value within the memory area represented by this {@code MemoryBlock}.
     *
     * @param offset the offset from the start of the memory location this {@code MemoryBlock} represents to begin searching.
     * @param value the {@code byte} value to locate.
     * @return the offset from the start of the search area (i.e. relative to the offset parameter), or -1 if not found.
     */
    public abstract long indexOf(long offset, byte value);

    /**
     * Returns the location of a byte value within the memory area represented by this {@code MemoryBlock}.
     *
     * @param offset the offset from the start of the memory location this {@code MemoryBlock} represents to begin searching.
     * @param value the {@code byte} value to locate.
     * @return the offset from the start of the search area (i.e. relative to the offset parameter), or -1 if not found.
     */
    public abstract long indexOf(long offset, int value);

    /**
     * Returns the location of a byte value within the memory area represented by this {@code MemoryBlock}.
     *
     * @param offset the offset from the start of the memory location this {@code MemoryBlock} represents to begin searching.
     * @param value the {@code byte} value to locate.
     * @param maxLength the maximum number of bytes to search for the desired value.
     * @return the offset from the start of the search area (i.e. relative to the offset parameter), or -1 if not found.
     */
    public abstract long indexOf(long offset, byte value, long maxLength);

    /**
     * Returns the location of a byte value within the memory area represented by this {@code MemoryBlock}.
     *
     * @param offset the offset from the start of the memory location this {@code MemoryBlock} represents to begin searching.
     * @param value the {@code byte} value to locate.
     * @param maxLength the maximum number of bytes to search for the desired value.
     * @return the offset from the start of the search area (i.e. relative to the offset parameter), or -1 if not found.
     */
    public abstract long indexOf(long offset, int value, long maxLength);

    public abstract long indexOf(long offset, Object valueArray, int valueArrayOffset, int valueSize);

    public abstract long indexOf(long offset, Object valueArray, int valueArrayOffset, int valueSize, long maxLength);

    public boolean hasMemory() {
        return attachment() == null;
    }

    public abstract boolean isNullPointer();

    @Override
    public int compareTo(MemoryBlock other) {
        if (isNullPointer()) return other.isNullPointer() ? 0 : -1;
        else if (other.isNullPointer()) return isNullPointer() ? 0 : 1;
        else if (isBounded() && other.isBounded()) {
            long size = size();
            long otherSize = other.size();
            long length;
            if (size > 0) length = otherSize < 0 ? size : Math.min(size, otherSize);
            else length = otherSize >= 0 ? otherSize : Math.min(size, otherSize);

            if (length >= 0) {
                for (long i = 0; i < length; i ++) {
                    byte oa = getInt8(i);
                    byte ob = other.getInt8(i);
                    if (oa != ob) return Byte.toUnsignedInt(oa) - Byte.toUnsignedInt(ob);
                }
            }
            else {
                byte oa, ob;
                for (long i = 0; i < Long.MAX_VALUE; i ++) {
                    oa = getInt8(i);
                    ob = other.getInt8(i);
                    if (oa != ob) return Byte.toUnsignedInt(oa) - Byte.toUnsignedInt(ob);
                }
                oa = getInt8(Long.MAX_VALUE);
                ob = other.getInt8(Long.MAX_VALUE);
                if (oa != ob) return Byte.toUnsignedInt(oa) - Byte.toUnsignedInt(ob);
                for (long i = Long.MIN_VALUE; i < length; i ++) {
                    oa = getInt8(i);
                    ob = other.getInt8(i);
                    if (oa != ob) return Byte.toUnsignedInt(oa) - Byte.toUnsignedInt(ob);
                }
            }
            return Long.compareUnsigned(size, otherSize);
        }
        else if (isBounded()) return compare(other, 0, size());
        else return compare(other, 0, other.size());
    }

    private int compare(MemoryBlock other, long offset, long size) {
        // FIXME: performance optimization
        if (size > 0) {
            for (long i = 0; i < size; i ++) {
                byte oa = getInt8(offset + i);
                byte ob = other.getInt8(offset + i);
                if (oa != ob) return Byte.toUnsignedInt(oa) - Byte.toUnsignedInt(ob);
            }
            return 0;
        }
        else {
            byte oa, ob;
            for (long i = 0; i < Long.MAX_VALUE; i ++) {
                oa = getInt8(offset + i);
                ob = other.getInt8(offset + i);
                if (oa != ob) return Byte.toUnsignedInt(oa) - Byte.toUnsignedInt(ob);
            }
            oa = getInt8(offset + Long.MAX_VALUE);
            ob = other.getInt8(offset + Long.MAX_VALUE);
            if (oa != ob) return Byte.toUnsignedInt(oa) - Byte.toUnsignedInt(ob);
            for (long i = Long.MIN_VALUE; i < size; i ++) {
                oa = getInt8(offset + i);
                ob = other.getInt8(offset + i);
                if (oa != ob) return Byte.toUnsignedInt(oa) - Byte.toUnsignedInt(ob);
            }
            return 0;
        }
    }

    public int compareTo(MemoryBlock other, long offset, long size) {
        if (size == 0) return 0;
        else if (isNullPointer() && other.isNullPointer()) return 0;
        else if (isDirect() && other.isDirect()) return Allocator.compare(address(), other.address(), size);
        else return compare(other, offset, size);
    }

    @Override
    public abstract void close();

    private abstract static class ValueAdapter {

        public abstract long get(MemoryBlock memoryBlock, long offset);
        public abstract void set(MemoryBlock memoryBlock, long offset, long value);

        public static final ValueAdapter SIZE64 = new ValueAdapter() {
            @Override
            public long get(MemoryBlock memoryBlock, long offset) {
                return memoryBlock.getInt64(offset);
            }
            @Override
            public void set(MemoryBlock memoryBlock, long offset, long value) {
                memoryBlock.setInt64(offset, value);
            }
        };

        public static final ValueAdapter SIZE32 = new ValueAdapter() {
            @Override
            public long get(MemoryBlock memoryBlock, long offset) {
                return (long) memoryBlock.getInt32(offset) & 0xFFFFFFFFL;
            }
            @Override
            public void set(MemoryBlock memoryBlock, long offset, long value) {
                memoryBlock.setInt32(offset, (int) value);
            }
        };

        public static final ValueAdapter SIZE16 = new ValueAdapter() {
            @Override
            public long get(MemoryBlock memoryBlock, long offset) {
                return (long) memoryBlock.getInt16(offset) & 0xFFFFL;
            }
            @Override
            public void set(MemoryBlock memoryBlock, long offset, long value) {
                memoryBlock.setInt16(offset, (short) value);
            }
        };

        public static final ValueAdapter SHORT = Foreign.shortSize() == 8 ? SIZE64 : SIZE16;
        public static final ValueAdapter INT = Foreign.intSize() == 8 ? SIZE64 : SIZE32;
        public static final ValueAdapter LONG = Foreign.longSize() == 8 ? SIZE64 : SIZE32;
        public static final ValueAdapter ADDRESS = Foreign.addressSize() == 8 ? SIZE64 : SIZE32;

    }

}
