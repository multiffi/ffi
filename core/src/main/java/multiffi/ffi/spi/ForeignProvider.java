package multiffi.ffi.spi;

import multiffi.ffi.MemoryHandle;
import multiffi.ffi.FunctionOptionVisitor;
import multiffi.ffi.CallOption;
import multiffi.ffi.ForeignType;
import multiffi.ffi.FunctionHandle;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
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
                            .getConstructor()
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
                if (IMPLEMENTATION == null) throw new IllegalStateException("Failed to get any installed multiffi.ffi.spi.ForeignProvider instance");
            }
        }
        return IMPLEMENTATION;
    }

    public abstract long addressSize();
    public abstract long diffSize();
    public abstract long shortSize();
    public abstract long intSize();
    public abstract long longSize();
    public abstract long wcharSize();
    public abstract long pageSize();
    public abstract long alignSize();

    public abstract Charset ansiCharset();
    public abstract Charset wideCharset();
    public abstract Charset utf16Charset();
    public abstract Charset utf32Charset();

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
    public String getStackTraceString(Throwable throwable) {
        if (throwable == null) return null;
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public abstract boolean isBigEndian();
    public abstract boolean isLittleEndian();
    public abstract ByteOrder endianness();

    public abstract void loadLibrary(String libraryName) throws UnsatisfiedLinkError;
    public abstract void loadLibrary(File libraryFile) throws UnsatisfiedLinkError;

    public abstract long getSymbolAddress(String symbolName) throws UnsatisfiedLinkError;
    public abstract String mapLibraryName(String libraryName);
    public abstract int getLastErrno();
    public abstract void setLastErrno(int errno);
    public abstract String getErrorString(int errno);
    public String getLastErrorString() {
        return getErrorString(getLastErrno());
    }

    public abstract FunctionHandle downcallHandle(long address, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options);
    public FunctionHandle downcallHandle(long address, int firstVarArgIndex, ForeignType returnType, ForeignType... parameterTypes) {
        return downcallHandle(address, firstVarArgIndex, returnType, parameterTypes, (CallOption[]) null);
    }
    public FunctionHandle downcallHandle(long address, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return downcallHandle(address, -1, returnType, parameterTypes, options);
    }
    public FunctionHandle downcallHandle(long address, ForeignType returnType, ForeignType... parameterTypes) {
        return downcallHandle(address, -1, returnType, parameterTypes);
    }
    public <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz,
                               long address, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        Objects.requireNonNull(clazz);
        boolean hasMethod = false;
        for (Method method : clazz.getMethods()) {
            if (!method.isDefault() && method.getDeclaringClass() != Object.class) {
                if (hasMethod) throw new IllegalArgumentException(clazz + " not a functional interface");
                else hasMethod = true;
            }
        }
        return downcallProxy(classLoader, clazz, new FunctionOptionVisitor() {
            @Override
            public long visitAddress(Method method) {
                return address;
            }
            @Override
            public int visitFirstVarArgIndex(Method method) {
                return firstVarArgIndex;
            }
            @Override
            public ForeignType visitReturnType(Method method) {
                return returnType;
            }
            @Override
            public ForeignType[] visitParameterTypes(Method method) {
                return parameterTypes;
            }
            @Override
            public CallOption[] visitCallOptions(Method method) {
                return options;
            }
        });
    }
    public <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz, long address, int firstVarArgIndex, ForeignType returnType, ForeignType... parameterTypes) {
        return downcallProxy(classLoader, clazz, address, firstVarArgIndex, returnType, parameterTypes, (CallOption[]) null);
    }
    public <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz,
                               long address, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return downcallProxy(classLoader, clazz, address, -1, returnType, parameterTypes, options);
    }
    public <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz, long address, ForeignType returnType, ForeignType... parameterTypes) {
        return downcallProxy(classLoader, clazz, address, -1, returnType, parameterTypes);
    }
    public <T> T downcallProxy(Class<T> clazz, long address, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return downcallProxy(null, clazz, address, firstVarArgIndex, returnType, parameterTypes, options);
    }
    public <T> T downcallProxy(Class<T> clazz, long address, int firstVarArgIndex, ForeignType returnType, ForeignType... parameterTypes) {
        return downcallProxy(null, clazz, address, firstVarArgIndex, returnType, parameterTypes);
    }
    public <T> T downcallProxy(Class<T> clazz, long address, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return downcallProxy(null, clazz, address, returnType, parameterTypes, options);
    }
    public <T> T downcallProxy(Class<T> clazz, long address, ForeignType returnType, ForeignType... parameterTypes) {
        return downcallProxy(null, clazz, address, returnType, parameterTypes);
    }
    public abstract Object downcallProxy(ClassLoader classLoader, Class<?>[] classes, FunctionOptionVisitor functionOptionVisitor);
    public Object downcallProxy(Class<?>[] classes, FunctionOptionVisitor functionOptionVisitor) {
        return downcallProxy(null, classes, functionOptionVisitor);
    }
    @SuppressWarnings("unchecked")
    public <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz, FunctionOptionVisitor functionOptionVisitor) {
        return (T) downcallProxy(classLoader, new Class<?>[] { clazz }, functionOptionVisitor);
    }
    public <T> T downcallProxy(Class<T> clazz, FunctionOptionVisitor functionOptionVisitor) {
        return downcallProxy(null, clazz, functionOptionVisitor);
    }
    public Object downcallProxy(ClassLoader classLoader, Class<?>... classes) {
        return downcallProxy(classLoader, classes, null);
    }
    public Object downcallProxy(Class<?>... classes) {
        return downcallProxy(classes, null);
    }
    public <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz) {
        return downcallProxy(classLoader, clazz, null);
    }
    public <T> T downcallProxy(Class<T> clazz) {
        return downcallProxy(clazz, null);
    }
    public abstract MemoryHandle upcallStub(Object object, Method method, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options);
    public MemoryHandle upcallStub(Object object, Method method, int firstVarArgIndex, ForeignType returnType, ForeignType... parameterTypes) {
        return upcallStub(object, method, firstVarArgIndex, returnType, parameterTypes, (CallOption[]) null);
    }
    public MemoryHandle upcallStub(Object object, Method method, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return upcallStub(object, method, -1, returnType, parameterTypes, options);
    }
    public MemoryHandle upcallStub(Object object, Method method, ForeignType returnType, ForeignType... parameterTypes) {
        return upcallStub(object, method, -1, returnType, parameterTypes);
    }

    public abstract Runnable registerCleaner(Object object, Runnable cleanup);

}
