package multiffi;

import multiffi.spi.ForeignProvider;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public final class Buffers {

    private Buffers() {
        throw new AssertionError("No multiffi.Buffers instances for you!");
    }

    private static final ForeignProvider IMPLEMENTATION = ForeignProvider.getImplementation();

    public ByteBuffer allocateBytes(int capacity, boolean direct) {
        return (direct ? ByteBuffer.allocateDirect(capacity).order(Foreign.endianness()) : ByteBuffer.allocate(capacity));
    }

    public CharBuffer allocateBytesAsChars(int capacity, boolean direct) {
        return (direct ? ByteBuffer.allocateDirect(capacity << 1).order(Foreign.endianness()) : ByteBuffer.allocate(capacity << 1)).asCharBuffer();
    }

    public ShortBuffer allocateBytesAsShorts(int capacity, boolean direct) {
        return (direct ? ByteBuffer.allocateDirect(capacity << 1).order(Foreign.endianness()) : ByteBuffer.allocate(capacity << 1)).asShortBuffer();
    }

    public IntBuffer allocateBytesAsInts(int capacity, boolean direct) {
        return (direct ? ByteBuffer.allocateDirect(capacity << 2).order(Foreign.endianness()) : ByteBuffer.allocate(capacity << 2)).asIntBuffer();
    }

    public LongBuffer allocateBytesAsLongs(int capacity, boolean direct) {
        return (direct ? ByteBuffer.allocateDirect(capacity << 3).order(Foreign.endianness()) : ByteBuffer.allocate(capacity << 3)).asLongBuffer();
    }

    public FloatBuffer allocateBytesAsFloats(int capacity, boolean direct) {
        return (direct ? ByteBuffer.allocateDirect(capacity << 2).order(Foreign.endianness()) : ByteBuffer.allocate(capacity << 2)).asFloatBuffer();
    }

    public DoubleBuffer allocateBytesAsDoubles(int capacity, boolean direct) {
        return (direct ? ByteBuffer.allocateDirect(capacity << 3).order(Foreign.endianness()) : ByteBuffer.allocate(capacity << 3)).asDoubleBuffer();
    }

    public ByteBuffer wrapBytes(byte[] array, int offset, int length) {
        return ByteBuffer.wrap(array, offset, length);
    }

    public ByteBuffer wrapBytes(byte[] array) {
        return ByteBuffer.wrap(array);
    }

    public ByteBuffer wrapBytes(long address, int capacity) {
        return IMPLEMENTATION.wrapDirectBuffer(address, capacity);
    }

    public static ByteBuffer wrapBytes(long address) {
        return IMPLEMENTATION.wrapDirectBuffer(address);
    }

    public CharBuffer allocateChars(int capacity) {
        return CharBuffer.allocate(capacity);
    }

    public CharBuffer wrapChars(char[] array, int offset, int length) {
        return CharBuffer.wrap(array, offset, length);
    }

    public CharBuffer wrapChars(char[] array) {
        return CharBuffer.wrap(array);
    }

    public CharBuffer wrapChars(CharSequence csq, int start, int end) {
        return CharBuffer.wrap(csq, start, end);
    }

    public CharBuffer wrapChars(CharSequence csq) {
        return CharBuffer.wrap(csq);
    }

    public ShortBuffer allocateShorts(int capacity) {
        return ShortBuffer.allocate(capacity);
    }

    public ShortBuffer wrapShorts(short[] array, int offset, int length) {
        return ShortBuffer.wrap(array, offset, length);
    }

    public ShortBuffer wrapShorts(short[] array) {
        return ShortBuffer.wrap(array);
    }

    public IntBuffer allocateInts(int capacity) {
        return IntBuffer.allocate(capacity);
    }

    public IntBuffer wrapInts(int[] array, int offset, int length) {
        return IntBuffer.wrap(array, offset, length);
    }

    public IntBuffer wrapInts(int[] array) {
        return IntBuffer.wrap(array);
    }

    public LongBuffer allocateLongs(int capacity) {
        return LongBuffer.allocate(capacity);
    }

    public LongBuffer wrapLongs(long[] array, int offset, int length) {
        return LongBuffer.wrap(array, offset, length);
    }

    public LongBuffer wrapLongs(long[] array) {
        return LongBuffer.wrap(array);
    }

    public FloatBuffer allocateFloats(int capacity) {
        return FloatBuffer.allocate(capacity);
    }

    public FloatBuffer wrapFloats(float[] array, int offset, int length) {
        return FloatBuffer.wrap(array, offset, length);
    }

    public FloatBuffer wrapFloats(float[] array) {
        return FloatBuffer.wrap(array);
    }

    public DoubleBuffer allocateDoubles(int capacity) {
        return DoubleBuffer.allocate(capacity);
    }

    public DoubleBuffer wrapDoubles(double[] array, int offset, int length) {
        return DoubleBuffer.wrap(array, offset, length);
    }

    public DoubleBuffer wrapDoubles(double[] array) {
        return DoubleBuffer.wrap(array);
    }

    public static int capacity(Buffer buffer) {
        return buffer.capacity();
    }

    public static int position(Buffer buffer) {
        return buffer.position();
    }

    public static int limit(Buffer buffer) {
        return buffer.limit();
    }

    public static int remaining(Buffer buffer) {
        return buffer.remaining();
    }

    public static boolean hasRemaining(Buffer buffer) {
        return buffer.hasRemaining();
    }

    public static boolean isReadOnly(Buffer buffer) {
        return buffer.isReadOnly();
    }

    public static boolean hasArray(Buffer buffer) {
        return buffer.hasArray();
    }

    public static boolean isDirect(Buffer buffer) {
        return buffer.isDirect();
    }

    public static boolean hasMemory(Buffer buffer) {
        return attachment(buffer) == null;
    }

    public static boolean hasAttachment(Buffer buffer) {
        return attachment(buffer) != null;
    }

    public static Object array(Buffer buffer) {
        return IMPLEMENTATION.getHeapBufferArray(buffer);
    }

    public static int arrayOffset(Buffer buffer) {
        return IMPLEMENTATION.getHeapBufferArrayOffset(buffer);
    }

    public static long address(Buffer buffer) {
        return IMPLEMENTATION.getDirectBufferAddress(buffer);
    }

    public static boolean isByteBuffer(Buffer buffer) {
        return IMPLEMENTATION.isByteBuffer(buffer);
    }

    public static ByteBuffer getByteBuffer(Buffer buffer) {
        return IMPLEMENTATION.getByteBuffer(buffer);
    }

    public static void clean(Buffer buffer) {
        IMPLEMENTATION.cleanBuffer(buffer);
    }

    public static <T extends Buffer> T attachment(T buffer) {
        return IMPLEMENTATION.getBufferAttachment(buffer);
    }
    
    public static <T extends Buffer> T rewind(T buffer) {
        ((Buffer) buffer).rewind();
        return buffer;
    }

    public static <T extends Buffer> T flip(T buffer) {
        ((Buffer) buffer).flip();
        return buffer;
    }

    public static <T extends Buffer> T mark(T buffer) {
        ((Buffer) buffer).mark();
        return buffer;
    }

    public static <T extends Buffer> T position(T buffer, int newPosition) {
        ((Buffer) buffer).position(newPosition);
        return buffer;
    }

    public static <T extends Buffer> T limit(T buffer, int newLimit) {
        ((Buffer) buffer).limit(newLimit);
        return buffer;
    }

    public static <T extends Buffer> T clear(T buffer) {
        ((Buffer) buffer).clear();
        return buffer;
    }

    public static <T extends Buffer> T reset(T buffer) {
        ((Buffer) buffer).reset();
        return buffer;
    }

    public static <T extends Buffer> T slice(T buffer, int index, int length) {
        return IMPLEMENTATION.sliceBuffer(buffer, index, length);
    }

    public static <T extends Buffer> T slice(T buffer, int index) {
        return IMPLEMENTATION.sliceBuffer(buffer, index);
    }

    public static <T extends Buffer> T duplicate(T buffer) {
        return IMPLEMENTATION.duplicateBuffer(buffer);
    }
    
}
