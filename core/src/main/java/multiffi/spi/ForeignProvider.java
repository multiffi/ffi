package multiffi.spi;

import multiffi.CallOption;
import multiffi.ForeignType;
import multiffi.FunctionPointer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

public abstract class ForeignProvider {

    private static volatile ForeignProvider IMPLEMENTATION;
    private static final Object IMPLEMENTATION_LOCK = new Object();
    public static ForeignProvider getImplementation() {
        if (IMPLEMENTATION == null) synchronized (IMPLEMENTATION_LOCK) {
            if (IMPLEMENTATION == null) {
                try {
                    IMPLEMENTATION = (ForeignProvider) Class
                            .forName(Objects.requireNonNull(System.getProperty("multiffi.foreign.provider")))
                            .getDeclaredConstructor()
                            .newInstance();
                } catch (Throwable e) {
                    try {
                        for (ForeignProvider provider : ServiceLoader.load(ForeignProvider.class)) {
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
                if (IMPLEMENTATION == null) throw new IllegalStateException("Failed to get any installed multiffi.spi.ForeignProvider instance");
            }
        }
        return IMPLEMENTATION;
    }

    public abstract long addressSize();
    public abstract long shortSize();
    public abstract long intSize();
    public abstract long longSize();
    public abstract long wideCharSize();
    public abstract long pageSize();

    public abstract Charset ansiCharset();
    public abstract Charset wideCharset();
    public abstract Charset utf16Charset();
    public abstract Charset utf32Charset();
    public abstract Charset consoleCharset();

    public abstract ByteBuffer wrapDirectBuffer(long address, int capacity);
    public abstract ByteBuffer wrapDirectBuffer(long address);
    public abstract Object getHeapBufferArray(Buffer buffer);
    public abstract int getHeapBufferArrayOffset(Buffer buffer);
    public abstract long getDirectBufferAddress(Buffer buffer);
    public abstract boolean isByteBuffer(Buffer buffer);
    public abstract ByteBuffer getByteBuffer(Buffer buffer);
    public abstract void cleanBuffer(Buffer buffer);
    public abstract <T extends Buffer> T getBufferAttachment(T buffer);
    public abstract <T extends Buffer> T sliceBuffer(T buffer, int index, int length);
    public abstract <T extends Buffer> T sliceBuffer(T buffer, int index);
    public abstract <T extends Buffer> T duplicateBuffer(T buffer);

    public abstract void sneakyThrows(Throwable throwable);

    public abstract void exit(int status);
    public abstract void halt(int status);

    public abstract long currentTimeMillis();
    public abstract long currentTimeMonotonic();
    public abstract long currentTimeSeconds();
    public abstract int currentTimeNanos();

    public abstract Map<String, String> environ();
    public abstract String getEnviron(String key);
    public abstract String getEnviron(String key, String defaultValue);
    public abstract String getStackTraceString(Throwable throwable);

    public abstract boolean isBigEndian();
    public abstract boolean isLittleEndian();
    public abstract ByteOrder endianness();

    public abstract void loadLibrary(String libraryName) throws IOException;
    public abstract void loadLibrary(File libraryFile) throws IOException;

    public abstract long getSymbol(String symbolName);
    public abstract String mapLibraryName(String libraryName);
    public abstract int getLastErrno();
    public abstract void setLastErrno(int errno);
    public abstract String getErrorString(int errno);
    public String getLastErrorString() {
        return getErrorString(getLastErrno());
    }

    private static final CallOption[] EMPTY_CALL_OPTION_ARRAY = new CallOption[0];
    public abstract FunctionPointer downcallHandle(long address, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options);
    public FunctionPointer downcallHandle(long address, int firstVararg, ForeignType returnType, ForeignType... parameterTypes) {
        return downcallHandle(address, firstVararg, returnType, parameterTypes, EMPTY_CALL_OPTION_ARRAY);
    }
    public FunctionPointer downcallHandle(long address, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return downcallHandle(address, -1, returnType, parameterTypes, options);
    }
    public FunctionPointer downcallHandle(long address, ForeignType returnType, ForeignType... parameterTypes) {
        return downcallHandle(address, -1, returnType, parameterTypes);
    }
    public abstract Object downcallProxy(ClassLoader classLoader, Class<?>... classes);
    public Object downcallProxy(Class<?>... classes) {
        return downcallProxy(null, classes);
    }
    @SuppressWarnings("unchecked")
    public <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz) {
        return (T) downcallProxy(classLoader, new Class<?>[] { clazz });
    }
    public <T> T downcallProxy(Class<T> clazz) {
        return downcallProxy(null, clazz);
    }
    public abstract long upcallStub(Object object, Method method, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options);
    public long upcallStub(Object object, Method method, int firstVararg, ForeignType returnType, ForeignType... parameterTypes) {
        return upcallStub(object, method, firstVararg, returnType, parameterTypes, EMPTY_CALL_OPTION_ARRAY);
    }
    public long upcallStub(Object object, Method method, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return upcallStub(object, method, -1, returnType, parameterTypes, options);
    }
    public long upcallStub(Object object, Method method, ForeignType returnType, ForeignType... parameterTypes) {
        return upcallStub(object, method, -1, returnType, parameterTypes);
    }

}
