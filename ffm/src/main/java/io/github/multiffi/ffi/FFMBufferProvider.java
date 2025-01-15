package io.github.multiffi.ffi;

import multiffi.ffi.spi.BufferProvider;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;

@SuppressWarnings({"deprecation", "removal"})
public class FFMBufferProvider extends BufferProvider {

    @Override
    public ByteBuffer wrapBytes(long address, int capacity) {
        return MemorySegment.ofAddress(address).reinterpret(capacity).asByteBuffer();
    }

    @Override
    public ByteBuffer wrapBytes(long address) {
        return MemorySegment.ofAddress(address).reinterpret(Integer.MAX_VALUE).asByteBuffer();
    }

    public static final Field addressField;
    public static final Method baseMethod;
    static {
        try {
            addressField = Buffer.class.getDeclaredField("address");
            baseMethod = Buffer.class.getDeclaredMethod("base");
        }
        catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }

    @Override
    public long address(Buffer buffer) {
        if (buffer.isDirect()) return FFMUtil.UNSAFE.getLong(buffer, FFMUtil.UNSAFE.objectFieldOffset(addressField));
        else return 0;
    }

    @Override
    public boolean isByteBuffer(Buffer buffer) {
        return buffer instanceof ByteBuffer || (buffer != null && buffer.getClass().getSimpleName().startsWith("ByteBufferAs"));
    }

    @Override
    public ByteBuffer getByteBuffer(Buffer buffer) {
        if (buffer instanceof ByteBuffer) return (ByteBuffer) buffer;
        else if (buffer != null && buffer.getClass().getSimpleName().startsWith("ByteBufferAs")) {
            try {
                return (ByteBuffer) FFMUtil.UNSAFE.getObject(buffer,
                        FFMUtil.UNSAFE.objectFieldOffset(buffer.getClass().getDeclaredField("bb")));
            } catch (NoSuchFieldException | ClassCastException e) {
                throw new IllegalStateException("Unexpected exception", e);
            }
        }
        else return null;
    }

    @Override
    public void clean(Buffer buffer) {
        ByteBuffer byteBuffer = getByteBuffer(buffer);
        if (byteBuffer != null) FFMUtil.UNSAFE.invokeCleaner(byteBuffer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Buffer> T attachment(T buffer) {
        if (buffer == null || !buffer.isDirect()) return null;
        else {
            try {
                return (T) FFMUtil.IMPL_LOOKUP
                        .unreflect(buffer.getClass().getMethod("attachment")).bindTo(buffer).invokeWithArguments();
            } catch (NoSuchMethodException | ClassCastException | IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception", e);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public <T extends Buffer> T slice(T buffer, int index, int length) {
        buffer.slice(index, length);
        return buffer;
    }

    @Override
    public <T extends Buffer> T slice(T buffer, int index) {
        buffer.slice(index, buffer.capacity() - index);
        return buffer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Buffer> T duplicate(T buffer) {
        return (T) buffer.duplicate();
    }

    @Override
    public Object array(Buffer buffer) {
        if (buffer.isDirect()) return null;
        else if (buffer.isReadOnly()) {
            try {
                return FFMUtil.IMPL_LOOKUP.unreflect(baseMethod).bindTo(buffer).invokeWithArguments();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception", e);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
        else return buffer.array();
    }

    @Override
    public int arrayOffset(Buffer buffer) {
        if (buffer.isDirect()) return 0;
        else if (buffer.isReadOnly()) {
            try {
                return FFMUtil.UNSAFE.getInt(buffer,
                        FFMUtil.UNSAFE.objectFieldOffset(buffer.getClass().getDeclaredField("offset")));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Unexpected exception", e);
            }
        }
        else return buffer.arrayOffset() * FFMUtil.UNSAFE.arrayIndexScale(buffer.array().getClass());
    }

}
