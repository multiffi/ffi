package io.github.multiffi;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import multiffi.spi.BufferProvider;
import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public class JNABufferProvider extends BufferProvider {

    @Override
    public ByteBuffer wrapBytes(long address, int capacity) {
        return new Pointer(address).getByteBuffer(0, capacity);
    }

    @Override
    public ByteBuffer wrapBytes(long address) {
        return new Pointer(address).getByteBuffer(0, Integer.MAX_VALUE - 8);
    }

    private static final class BufferMethodHolder {
        private BufferMethodHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Method baseMethod;
        public static final Method sliceMethod;
        public static final Method duplicateMethod;
        static {
            Method method;
            try {
                method = Buffer.class.getDeclaredMethod("base");
            }
            catch (NoSuchMethodException e) {
                method = null;
            }
            baseMethod = method;
            try {
                method = Buffer.class.getDeclaredMethod("duplicate");
            }
            catch (NoSuchMethodException e) {
                method = null;
            }
            duplicateMethod = method;
            try {
                method = Buffer.class.getDeclaredMethod("slice", int.class, int.class);
            }
            catch (NoSuchMethodException e) {
                method = null;
            }
            sliceMethod = method;
        }
    }

    @Override
    public Object array(Buffer buffer) {
        if (buffer.isDirect()) return null;
        else if (buffer.isReadOnly()) {
            if (BufferMethodHolder.baseMethod == null) {
                if (isByteBuffer(buffer)) buffer = getByteBuffer(buffer);
                try {
                    return JNAUtil.UnsafeHolder.UNSAFE.getObject(buffer,
                            JNAUtil.UnsafeHolder.UNSAFE.objectFieldOffset(buffer.getClass().getField("hb")));
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
            else {
                try {
                    return JNAUtil.invoke(buffer, BufferMethodHolder.baseMethod);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        else return buffer.array();
    }

    @Override
    public int arrayOffset(Buffer buffer) {
        if (buffer.isDirect()) return 0;
        else if (buffer.isReadOnly()) {
            try {
                return JNAUtil.UnsafeHolder.UNSAFE.getInt(buffer,
                        JNAUtil.UnsafeHolder.UNSAFE.objectFieldOffset(buffer.getClass().getDeclaredField("offset")));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
        else return buffer.arrayOffset() * JNAUtil.UnsafeHolder.UNSAFE.arrayIndexScale(buffer.array().getClass());
    }

    @Override
    public long address(Buffer buffer) {
        return buffer.isDirect() ? Pointer.nativeValue(Native.getDirectBufferPointer(buffer)) : 0;
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
                return (ByteBuffer) JNAUtil.UnsafeHolder.UNSAFE.getObject(buffer,
                        JNAUtil.UnsafeHolder.UNSAFE.objectFieldOffset(buffer.getClass().getDeclaredField("bb")));
            } catch (NoSuchFieldException | ClassCastException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
        else return null;
    }

    private static final class InvokeCleanerMethodHolder {
        private InvokeCleanerMethodHolder() {
            throw new UnsupportedOperationException();
        }
        private static final Method invokeCleanerMethod;
        static {
            Method method;
            try {
                method = Unsafe.class.getDeclaredMethod("invokeCleaner", ByteBuffer.class);
            } catch (NoSuchMethodException e) {
                method = null;
            }
            invokeCleanerMethod = method;
        }
    }

    private static void invokeCleaner(ByteBuffer buffer) {
        if (InvokeCleanerMethodHolder.invokeCleanerMethod == null) {
            if (!buffer.isDirect()) throw new IllegalArgumentException("buffer is non-direct");
            if (attachmentOf(buffer) != null) throw new IllegalArgumentException("duplicate or slice");
            try {
                Object cleaner = JNAUtil.invoke(buffer, buffer.getClass().getMethod("cleaner"));
                cleaner.getClass().getMethod("clean").invoke(cleaner);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
        else {
            try {
                InvokeCleanerMethodHolder.invokeCleanerMethod.invoke(JNAUtil.UnsafeHolder.UNSAFE, buffer);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            } catch (InvocationTargetException e) {
                Throwable targetException = e.getTargetException();
                if (targetException instanceof Error) throw (Error) targetException;
                else if (targetException instanceof RuntimeException) throw (RuntimeException) targetException;
                else throw new IllegalStateException(targetException);
            }
        }
    }

    @Override
    public void clean(Buffer buffer) {
        ByteBuffer byteBuffer = getByteBuffer(buffer);
        if (byteBuffer != null) invokeCleaner(byteBuffer);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Buffer> T attachmentOf(T buffer) {
        if (buffer == null || !buffer.isDirect()) return null;
        else {
            try {
                return (T) JNAUtil.invoke(buffer, buffer.getClass().getMethod("attachment"));
            } catch (NoSuchMethodException | ClassCastException | IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public <T extends Buffer> T attachment(T buffer) {
        return attachmentOf(buffer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Buffer> T slice(T buffer, int index, int length) {
        if (BufferMethodHolder.sliceMethod == null) {
            buffer = duplicate(buffer);
            int limit = limit(buffer);
            if ((limit | index | length) < 0 || length > limit - index)
                throw new IndexOutOfBoundsException(String.format("Range [%s, %<s + %s) out of bounds for length %s", index, length, limit));
            limit(buffer, index + length).position(index);
            try {
                return (T) JNAUtil.invoke(buffer, buffer.getClass().getMethod("slice"));
            } catch (NoSuchMethodException | ClassCastException e) {
                throw new IllegalArgumentException("Unsupported buffer");
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
        else {
            try {
                return (T) JNAUtil.invoke(buffer, BufferMethodHolder.sliceMethod, index, length);
            } catch (ClassCastException | IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public <T extends Buffer> T slice(T buffer, int index) {
        return slice(buffer, index, buffer.capacity() - index);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Buffer> T duplicate(T buffer) {
        if (BufferMethodHolder.duplicateMethod == null) {
            if (buffer instanceof ByteBuffer) return (T) ((ByteBuffer) buffer).duplicate();
            else if (buffer instanceof CharBuffer) return (T) ((CharBuffer) buffer).duplicate();
            else if (buffer instanceof ShortBuffer) return (T) ((ShortBuffer) buffer).duplicate();
            else if (buffer instanceof IntBuffer) return (T) ((IntBuffer) buffer).duplicate();
            else if (buffer instanceof LongBuffer) return (T) ((LongBuffer) buffer).duplicate();
            else if (buffer instanceof FloatBuffer) return (T) ((FloatBuffer) buffer).duplicate();
            else if (buffer instanceof DoubleBuffer) return (T) ((DoubleBuffer) buffer).duplicate();
            else {
                try {
                    return (T) JNAUtil.invoke(buffer, buffer.getClass().getMethod("duplicate"));
                } catch (NoSuchMethodException | ClassCastException e) {
                    throw new IllegalArgumentException("Unsupported buffer");
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        else {
            try {
                return (T) JNAUtil.invoke(buffer, BufferMethodHolder.duplicateMethod);
            } catch (ClassCastException e) {
                throw new IllegalStateException("Unexpected exception");
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

}
