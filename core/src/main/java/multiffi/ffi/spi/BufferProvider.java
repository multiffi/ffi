package multiffi.ffi.spi;

import multiffi.ffi.Foreign;
import multiffi.ffi.MemoryHandle;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;
import java.util.ServiceLoader;

public abstract class BufferProvider {

    private static volatile BufferProvider IMPLEMENTATION;
    private static final Object IMPLEMENTATION_LOCK = new Object();
    public static BufferProvider getImplementation() {
        if (IMPLEMENTATION == null) synchronized (IMPLEMENTATION_LOCK) {
            if (IMPLEMENTATION == null) {
                try {
                    IMPLEMENTATION = (BufferProvider) Class
                            .forName(Objects.requireNonNull(System.getProperty("multiffi.buffer.provider")))
                            .getConstructor()
                            .newInstance();
                } catch (Throwable e) {
                    try {
                        for (BufferProvider provider : ServiceLoader.load(BufferProvider.class)) {
                            if (provider != null) {
                                IMPLEMENTATION = provider;
                                break;
                            }
                        }
                    }
                    catch (Throwable ex) {
                        ex.printStackTrace();
                        IMPLEMENTATION = null;
                    }
                }
                if (IMPLEMENTATION == null) throw new IllegalStateException("Failed to get any installed multiffi.ffi.spi.BufferProvider instance");
            }
        }
        return IMPLEMENTATION;
    }

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

    public abstract ByteBuffer wrapBytes(long address, int capacity);

    public abstract ByteBuffer wrapBytes(long address);

    public ByteBuffer wrapBytes(MemoryHandle memoryHandle) {
        long size = memoryHandle.size();
        if (memoryHandle.isDirect()) return wrapBytes(memoryHandle.address(), (size < 0 || size > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) size);
        else return wrapBytes((byte[]) memoryHandle.array(), (int) memoryHandle.arrayOffset(), (int) size);
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

    public int capacity(Buffer buffer) {
        return buffer.capacity();
    }

    public int position(Buffer buffer) {
        return buffer.position();
    }

    public int limit(Buffer buffer) {
        return buffer.limit();
    }

    public int remaining(Buffer buffer) {
        return buffer.remaining();
    }

    public boolean hasRemaining(Buffer buffer) {
        return buffer.hasRemaining();
    }

    public boolean isReadOnly(Buffer buffer) {
        return buffer.isReadOnly();
    }

    public boolean hasArray(Buffer buffer) {
        return buffer.hasArray();
    }

    public boolean isDirect(Buffer buffer) {
        return buffer.isDirect();
    }

    public boolean hasMemory(Buffer buffer) {
        return attachment(buffer) == null;
    }

    public boolean hasAttachment(Buffer buffer) {
        return attachment(buffer) != null;
    }

    public abstract Object array(Buffer buffer);

    public abstract int arrayOffset(Buffer buffer);

    public abstract long address(Buffer buffer);

    public abstract boolean isByteBuffer(Buffer buffer);

    public abstract ByteBuffer getByteBuffer(Buffer buffer);

    public abstract void clean(Buffer buffer);

    public abstract <T extends Buffer> T attachment(T buffer);

    public <T extends Buffer> T rewind(T buffer) {
        ((Buffer) buffer).rewind();
        return buffer;
    }

    public <T extends Buffer> T flip(T buffer) {
        ((Buffer) buffer).flip();
        return buffer;
    }

    public <T extends Buffer> T mark(T buffer) {
        ((Buffer) buffer).mark();
        return buffer;
    }

    public <T extends Buffer> T position(T buffer, int newPosition) {
        ((Buffer) buffer).position(newPosition);
        return buffer;
    }

    public <T extends Buffer> T limit(T buffer, int newLimit) {
        ((Buffer) buffer).limit(newLimit);
        return buffer;
    }

    public <T extends Buffer> T clear(T buffer) {
        ((Buffer) buffer).clear();
        return buffer;
    }

    public <T extends Buffer> T reset(T buffer) {
        ((Buffer) buffer).reset();
        return buffer;
    }

    public abstract <T extends Buffer> T slice(T buffer, int index, int length);

    public abstract <T extends Buffer> T slice(T buffer, int index);

    public abstract <T extends Buffer> T duplicate(T buffer);

}
