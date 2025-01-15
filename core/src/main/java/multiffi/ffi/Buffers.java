package multiffi.ffi;

import multiffi.ffi.spi.BufferProvider;

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
        throw new AssertionError("No multiffi.ffi.Buffers instances for you!");
    }

    private static final BufferProvider IMPLEMENTATION = BufferProvider.getImplementation();

    public static ByteBuffer allocateBytes(int capacity, boolean direct) {
        return IMPLEMENTATION.allocateBytes(capacity, direct);
    }

    public static CharBuffer allocateBytesAsChars(int capacity, boolean direct) {
        return IMPLEMENTATION.allocateBytesAsChars(capacity, direct);
    }

    public static ShortBuffer allocateBytesAsShorts(int capacity, boolean direct) {
        return IMPLEMENTATION.allocateBytesAsShorts(capacity, direct);
    }

    public static IntBuffer allocateBytesAsInts(int capacity, boolean direct) {
        return IMPLEMENTATION.allocateBytesAsInts(capacity, direct);
    }

    public static LongBuffer allocateBytesAsLongs(int capacity, boolean direct) {
        return IMPLEMENTATION.allocateBytesAsLongs(capacity, direct);
    }

    public static FloatBuffer allocateBytesAsFloats(int capacity, boolean direct) {
        return IMPLEMENTATION.allocateBytesAsFloats(capacity, direct);
    }

    public static DoubleBuffer allocateBytesAsDoubles(int capacity, boolean direct) {
        return IMPLEMENTATION.allocateBytesAsDoubles(capacity, direct);
    }

    public static ByteBuffer wrapBytes(byte[] array, int offset, int length) {
        return IMPLEMENTATION.wrapBytes(array, offset, length);
    }

    public static ByteBuffer wrapBytes(byte[] array) {
        return IMPLEMENTATION.wrapBytes(array);
    }

    public static ByteBuffer wrapBytes(long address, int capacity) {
        return IMPLEMENTATION.wrapBytes(address, capacity);
    }

    public static ByteBuffer wrapBytes(long address) {
        return IMPLEMENTATION.wrapBytes(address);
    }

    public static ByteBuffer wrapBytes(MemoryHandle memoryHandle) {
        return IMPLEMENTATION.wrapBytes(memoryHandle);
    }

    public static CharBuffer allocateChars(int capacity) {
        return IMPLEMENTATION.allocateChars(capacity);
    }

    public static CharBuffer wrapChars(char[] array, int offset, int length) {
        return IMPLEMENTATION.wrapChars(array, offset, length);
    }

    public static CharBuffer wrapChars(char[] array) {
        return IMPLEMENTATION.wrapChars(array);
    }

    public static CharBuffer wrapChars(CharSequence csq, int start, int end) {
        return IMPLEMENTATION.wrapChars(csq, start, end);
    }

    public static CharBuffer wrapChars(CharSequence csq) {
        return IMPLEMENTATION.wrapChars(csq);
    }

    public static ShortBuffer allocateShorts(int capacity) {
        return IMPLEMENTATION.allocateShorts(capacity);
    }

    public static ShortBuffer wrapShorts(short[] array, int offset, int length) {
        return IMPLEMENTATION.wrapShorts(array, offset, length);
    }

    public static ShortBuffer wrapShorts(short[] array) {
        return IMPLEMENTATION.wrapShorts(array);
    }

    public static IntBuffer allocateInts(int capacity) {
        return IMPLEMENTATION.allocateInts(capacity);
    }

    public static IntBuffer wrapInts(int[] array, int offset, int length) {
        return IMPLEMENTATION.wrapInts(array, offset, length);
    }

    public static IntBuffer wrapInts(int[] array) {
        return IMPLEMENTATION.wrapInts(array);
    }

    public static LongBuffer allocateLongs(int capacity) {
        return IMPLEMENTATION.allocateLongs(capacity);
    }

    public static LongBuffer wrapLongs(long[] array, int offset, int length) {
        return IMPLEMENTATION.wrapLongs(array, offset, length);
    }

    public static LongBuffer wrapLongs(long[] array) {
        return IMPLEMENTATION.wrapLongs(array);
    }

    public static FloatBuffer allocateFloats(int capacity) {
        return IMPLEMENTATION.allocateFloats(capacity);
    }

    public static FloatBuffer wrapFloats(float[] array, int offset, int length) {
        return IMPLEMENTATION.wrapFloats(array, offset, length);
    }

    public static FloatBuffer wrapFloats(float[] array) {
        return IMPLEMENTATION.wrapFloats(array);
    }

    public static DoubleBuffer allocateDoubles(int capacity) {
        return IMPLEMENTATION.allocateDoubles(capacity);
    }

    public static DoubleBuffer wrapDoubles(double[] array, int offset, int length) {
        return IMPLEMENTATION.wrapDoubles(array, offset, length);
    }

    public static DoubleBuffer wrapDoubles(double[] array) {
        return IMPLEMENTATION.wrapDoubles(array);
    }

    public static int capacity(Buffer buffer) {
        return IMPLEMENTATION.capacity(buffer);
    }

    public static int position(Buffer buffer) {
        return IMPLEMENTATION.position(buffer);
    }

    public static int limit(Buffer buffer) {
        return IMPLEMENTATION.limit(buffer);
    }

    public static int remaining(Buffer buffer) {
        return IMPLEMENTATION.remaining(buffer);
    }

    public static boolean hasRemaining(Buffer buffer) {
        return IMPLEMENTATION.hasRemaining(buffer);
    }

    public static boolean isReadOnly(Buffer buffer) {
        return IMPLEMENTATION.isReadOnly(buffer);
    }

    public static boolean hasArray(Buffer buffer) {
        return IMPLEMENTATION.hasArray(buffer);
    }

    public static boolean isDirect(Buffer buffer) {
        return IMPLEMENTATION.isDirect(buffer);
    }

    public static boolean hasMemory(Buffer buffer) {
        return IMPLEMENTATION.hasMemory(buffer);
    }

    public static boolean hasAttachment(Buffer buffer) {
        return IMPLEMENTATION.hasAttachment(buffer);
    }

    public static Object array(Buffer buffer) {
        return IMPLEMENTATION.array(buffer);
    }

    public static int arrayOffset(Buffer buffer) {
        return IMPLEMENTATION.arrayOffset(buffer);
    }

    public static long address(Buffer buffer) {
        return IMPLEMENTATION.address(buffer);
    }

    public static boolean isByteBuffer(Buffer buffer) {
        return IMPLEMENTATION.isByteBuffer(buffer);
    }

    public static ByteBuffer getByteBuffer(Buffer buffer) {
        return IMPLEMENTATION.getByteBuffer(buffer);
    }

    public static void clean(Buffer buffer) {
        IMPLEMENTATION.clean(buffer);
    }

    public static <T extends Buffer> T attachment(T buffer) {
        return IMPLEMENTATION.attachment(buffer);
    }
    
    public static <T extends Buffer> T rewind(T buffer) {
        return IMPLEMENTATION.rewind(buffer);
    }

    public static <T extends Buffer> T flip(T buffer) {
        return IMPLEMENTATION.flip(buffer);
    }

    public static <T extends Buffer> T mark(T buffer) {
        return IMPLEMENTATION.mark(buffer);
    }

    public static <T extends Buffer> T position(T buffer, int newPosition) {
        return IMPLEMENTATION.position(buffer, newPosition);
    }

    public static <T extends Buffer> T limit(T buffer, int newLimit) {
        return IMPLEMENTATION.limit(buffer, newLimit);
    }

    public static <T extends Buffer> T clear(T buffer) {
        return IMPLEMENTATION.clear(buffer);
    }

    public static <T extends Buffer> T reset(T buffer) {
        return IMPLEMENTATION.reset(buffer);
    }

    public static <T extends Buffer> T slice(T buffer, int index, int length) {
        return IMPLEMENTATION.slice(buffer, index, length);
    }

    public static <T extends Buffer> T slice(T buffer, int index) {
        return IMPLEMENTATION.slice(buffer, index);
    }

    public static <T extends Buffer> T duplicate(T buffer) {
        return IMPLEMENTATION.duplicate(buffer);
    }
    
}
