package io.github.multiffi;

import com.sun.jna.CallbackProxy;
import com.sun.jna.CallbackReference;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.internal.Cleaner;
import multiffi.CallOption;
import multiffi.CallOptionVisitor;
import multiffi.ForeignType;
import multiffi.FunctionHandle;
import multiffi.MemoryHandle;
import multiffi.ScalarType;
import multiffi.StandardCallOption;
import multiffi.UnsatisfiedLinkException;
import multiffi.spi.ForeignProvider;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class JNAForeignProvider extends ForeignProvider {

    @Override
    public long addressSize() {
        return Native.POINTER_SIZE;
    }

    @Override
    public long shortSize() {
        return 2L;
    }

    @Override
    public long intSize() {
        return 4L;
    }

    @Override
    public long longSize() {
        return Native.LONG_SIZE;
    }

    @Override
    public long wcharSize() {
        return Native.WCHAR_SIZE;
    }

    @Override
    public long pageSize() {
        return JNAUtil.UnsafeHolder.UNSAFE.pageSize() & 0xFFFFFFFFL;
    }

    private static final class CharsetHolder {
        private CharsetHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Charset UTF16_CHARSET = JNAUtil.IS_BIG_ENDIAN ? Charset.forName("UTF-16BE") : Charset.forName("UTF-16LE");
        public static final Charset UTF32_CHARSET = JNAUtil.IS_BIG_ENDIAN ? Charset.forName("UTF-32BE") : Charset.forName("UTF-32LE");
        public static final Charset WIDE_CHARSET = Native.WCHAR_SIZE == 2 ? UTF16_CHARSET : UTF32_CHARSET;
        public static final Charset ANSI_CHARSET = Charset.forName(System.getProperty("jna.encoding", System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding", Charset.defaultCharset().name()))));
    }

    @Override
    public Charset ansiCharset() {
        return CharsetHolder.ANSI_CHARSET;
    }

    @Override
    public Charset wideCharset() {
        return CharsetHolder.WIDE_CHARSET;
    }

    @Override
    public Charset utf16Charset() {
        return CharsetHolder.UTF16_CHARSET;
    }

    @Override
    public Charset utf32Charset() {
        return CharsetHolder.UTF32_CHARSET;
    }

    @Override
    public void sneakyThrows(Throwable throwable) {
        if (throwable != null) JNAUtil.UnsafeHolder.UNSAFE.throwException(throwable);
    }

    private static final Runtime RUNTIME = Runtime.getRuntime();

    @Override
    public void exit(int status) {
        RUNTIME.exit(status);
    }

    @Override
    public void halt(int status) {
        RUNTIME.halt(status);
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long currentTimeMonotonic() {
        return System.nanoTime();
    }

    @Override
    public long currentTimeSeconds() {
        return JNACurrentTime.seconds();
    }

    @Override
    public int currentTimeNanos() {
        return JNACurrentTime.nanos();
    }

    @Override
    public Map<String, String> environ() {
        return System.getenv();
    }

    @Override
    public String getEnviron(String key) {
        return System.getenv(key);
    }

    @Override
    public String getEnviron(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null ? defaultValue : value;
    }

    @Override
    public String getStackTraceString(Throwable throwable) {
        if (throwable == null) return null;
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    @Override
    public boolean isBigEndian() {
        return JNAUtil.IS_BIG_ENDIAN;
    }

    @Override
    public boolean isLittleEndian() {
        return !JNAUtil.IS_BIG_ENDIAN;
    }

    @Override
    public ByteOrder endianness() {
        return ByteOrder.nativeOrder();
    }

    private static final class NativeLibraryHolder {
        private NativeLibraryHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Set<NativeLibrary> NATIVE_LIBRARIES = new HashSet<>();
        static {
            NATIVE_LIBRARIES.add(NativeLibrary.getProcess());
            if (!Platform.isLinux()) NATIVE_LIBRARIES.add(NativeLibrary.getInstance(Platform.C_LIBRARY_NAME));
            if (!Platform.isWindows() && !Platform.isLinux()) NATIVE_LIBRARIES.add(NativeLibrary.getInstance(Platform.MATH_LIBRARY_NAME));
        }
        public static final Method getSymbolAddressMethod;
        static {
            try {
                getSymbolAddressMethod = NativeLibrary.class.getDeclaredMethod("getSymbolAddress", String.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
    }

    @Override
    public void loadLibrary(String libraryName) throws IOException {
        Objects.requireNonNull(libraryName);
        try {
            NativeLibraryHolder.NATIVE_LIBRARIES.add(NativeLibrary.getInstance(libraryName));
        }
        catch (UnsatisfiedLinkError e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void loadLibrary(File libraryFile) throws IOException {
        Objects.requireNonNull(libraryFile);
        try {
            NativeLibraryHolder.NATIVE_LIBRARIES.add(NativeLibrary.getInstance(libraryFile.getAbsolutePath()));
        }
        catch (UnsatisfiedLinkError e) {
            throw new IOException(e.getMessage());
        }
    }

    private static final class FindNativeMethodHolder {
        private FindNativeMethodHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Method findNativeMethod;
        static {
            Method method;
            try {
                method = ClassLoader.class.getDeclaredMethod("findNative", ClassLoader.class, String.class);
            } catch (NoSuchMethodException e) {
                method = null;
            }
            findNativeMethod = method;
        }
    }

    @Override
    public long getSymbol(String symbolName) {
        Objects.requireNonNull(symbolName);
        if (FindNativeMethodHolder.findNativeMethod != null) {
            try {
                return (long) JNAUtil.invoke(Thread.currentThread().getContextClassLoader(), FindNativeMethodHolder.findNativeMethod, symbolName);
            } catch (Throwable ignored) {
            }
        }
        synchronized (NativeLibraryHolder.NATIVE_LIBRARIES) {
            for (NativeLibrary library : NativeLibraryHolder.NATIVE_LIBRARIES) {
                try {
                    return (long) JNAUtil.invoke(library, NativeLibraryHolder.getSymbolAddressMethod, symbolName);
                } catch (UnsatisfiedLinkError ignored) {
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new UnsatisfiedLinkException(String.format("Failed to get symbol: `%s`", symbolName));
    }

    @Override
    public String mapLibraryName(String libraryName) {
        return JNAUtil.mapLibraryName(libraryName);
    }

    @Override
    public int getLastErrno() {
        return JNAUtil.LastErrnoHolder.ERRNO_THREAD_LOCAL.get();
    }

    @Override
    public void setLastErrno(int errno) {
        JNAUtil.LastErrnoHolder.ERRNO_THREAD_LOCAL.set(errno);
    }

    private static final class ErrorStringMapperHolder {
        private ErrorStringMapperHolder() {
            throw new UnsupportedOperationException();
        }
        private static final class Kernel32 {
            private Kernel32() {
                throw new UnsupportedOperationException();
            }
            public static native int FormatMessageW(int dwFlags, Pointer lpSource, int dwMessageId, int dwLanguageId, Pointer lpBuffer, int nSize, Pointer arguments);
            public static native Pointer LocalFree(Pointer hMem);
            static {
                if (Platform.isWindows()) Native.register(Kernel32.class, "kernel32");
            }
            private static final ThreadLocal<Pointer> POINTER_THREAD_LOCAL = new ThreadLocal<Pointer>() {
                @Override
                protected Pointer initialValue() {
                    return new Memory(Native.POINTER_SIZE);
                }
            };
            public static String strerror(int errno) {
                Pointer lpBuffer = POINTER_THREAD_LOCAL.get();
                lpBuffer.setPointer(0, Pointer.NULL);
                try {
                    if (FormatMessageW(0x00001000 /* FORMAT_MESSAGE_FROM_SYSTEM */ | 0x00000100 /* FORMAT_MESSAGE_ALLOCATE_BUFFER */,
                            Pointer.NULL,
                            errno,
                            0,
                            lpBuffer,
                            0,
                            Pointer.NULL) == 0) return "FormatMessage failed with 0x" + Integer.toHexString(errno);
                    else return lpBuffer.getPointer(0).getString(0, CharsetHolder.WIDE_CHARSET.name());
                } finally {
                    Pointer hMem = lpBuffer.getPointer(0);
                    if (hMem != null) LocalFree(hMem);
                }
            }
        }
        private static final class CLibrary {
            private CLibrary() {
                throw new UnsupportedOperationException();
            }
            public static native Pointer strerror(int errno);
            static {
                if (!Platform.isWindows()) Native.register(CLibrary.class, Platform.C_LIBRARY_NAME);
            }
        }
    }

    @Override
    public String getErrorString(int errno) {
        if (Platform.isWindows()) return ErrorStringMapperHolder.Kernel32.strerror(errno);
        else {
            Pointer errorString = ErrorStringMapperHolder.CLibrary.strerror(errno);
            return errorString == null ? "strerror failed with 0x" + Integer.toHexString(errno) : errorString.getString(0, CharsetHolder.ANSI_CHARSET.name());
        }
    }

    @Override
    public FunctionHandle downcallHandle(long address, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return new JNAFunctionHandle(address, firstVararg, returnType, parameterTypes, options);
    }

    private static class DowncallHandler implements InvocationHandler {
        private final Map<Method, JNAFunctionHandle> functionHandleMap;
        private static void checkArgumentType(ForeignType type, Class<?> argumentType) {
            Class<?> expectedType;
            if (type == ScalarType.BOOLEAN) expectedType = boolean.class;
            else if (type == ScalarType.UTF16) expectedType = char.class;
            else if (type == ScalarType.INT8 || type == ScalarType.CHAR) expectedType = byte.class;
            else if (type == ScalarType.INT16) expectedType = short.class;
            else if (type == ScalarType.INT32 || type == ScalarType.WCHAR) expectedType = int.class;
            else if (type == ScalarType.INT64 || type == ScalarType.SHORT || type == ScalarType.INT
                    || type == ScalarType.LONG || type == ScalarType.SIZE || type == ScalarType.ADDRESS)
                expectedType = long.class;
            else if (type == ScalarType.FLOAT) expectedType = float.class;
            else if (type == ScalarType.DOUBLE) expectedType = double.class;
            else expectedType = MemoryHandle.class;
            if (argumentType != expectedType) throw new IllegalArgumentException("Illegal mapping type; expected " + expectedType);
        }
        public DowncallHandler(Class<?>[] classes, CallOptionVisitor callOptionVisitor) {
            Map<Method, JNAFunctionHandle> functionHandleMap = new HashMap<>();
            for (Class<?> clazz : classes) {
                for (Method method : clazz.getMethods()) {
                    String methodName = method.getName();
                    Class<?> returnType = method.getReturnType();
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (methodName.equals("hashCode") && returnType == int.class && parameterTypes.length == 0) continue;
                    else if (methodName.equals("equals") && returnType == boolean.class && parameterTypes.length == 1) continue;
                    else if (methodName.equals("toString") && returnType == String.class && parameterTypes.length == 0) continue;
                    else {
                        long address = callOptionVisitor.visitAddress(method);
                        int firstVarargIndex = callOptionVisitor.visitFirstVarArgIndex(method);
                        ForeignType returnForeignType = callOptionVisitor.visitReturnType(method);
                        ForeignType[] parameterForeignTypes = callOptionVisitor.visitParameterTypes(method);
                        CallOption[] callOptions = callOptionVisitor.visitCallOptions(method);
                        boolean addReturnMemoryParameter = returnForeignType != null && returnForeignType.isCompound();
                        for (int i = 0; i < parameterForeignTypes.length; i ++) {
                            checkArgumentType(parameterForeignTypes[i], parameterTypes[i + (addReturnMemoryParameter ? 1 : 0)]);
                        }
                        JNAFunctionHandle functionHandle = new JNAFunctionHandle(address, firstVarargIndex, returnForeignType, parameterForeignTypes, callOptions);
                        functionHandleMap.put(method, functionHandle);
                    }
                }
            }
            this.functionHandleMap = Collections.unmodifiableMap(functionHandleMap);
        }
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (methodName.equals("hashCode") && returnType == int.class && parameterTypes.length == 0)
                return hashCode();
            else if (methodName.equals("equals") && returnType == boolean.class && parameterTypes.length == 1) {
                Object other = args[0];
                if (other == null) return false;
                if (Proxy.isProxyClass(other.getClass())) other = Proxy.getInvocationHandler(other);
                else if (!(other instanceof InvocationHandler)) return false;
                return this == other;
            }
            else if (methodName.equals("toString") && returnType == String.class && parameterTypes.length == 0)
                return proxy.getClass().getName() + "@" + Integer.toHexString(hashCode());
            else return functionHandleMap.get(method).invoke(args);
        }
    }

    @Override
    public Object downcallProxy(ClassLoader classLoader, Class<?>[] classes, CallOptionVisitor callOptionVisitor) {
        if (classLoader == null) classLoader = Thread.currentThread().getContextClassLoader();
        return Proxy.newProxyInstance(classLoader, classes, new DowncallHandler(classes, callOptionVisitor));
    }

    private static final class CallbackHolder implements Runnable {
        private volatile CallbackProxy callback;
        public CallbackHolder(CallbackProxy callback) {
            this.callback = callback;
        }
        @Override
        public void run() {
            callback = null;
        }
    }

    private static Class<?> toClass(ForeignType type) {
        if (type == null) return void.class;
        else if (type.isCompound()) {
            long size = type.size();
            if (size < 0 || size > (Integer.MAX_VALUE - 8)) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(size));
            int length = (int) size;
            JNACompound.VariableLength.deque().addFirst(length);
            return JNACompound.VariableLength.class;
        }
        else return type.carrier();
    }

    @Override
    public long upcallStub(Object object, Method method, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        boolean stdcall = false;
        for (CallOption option : options) {
            if (option.equals(StandardCallOption.STDCALL)) {
                stdcall = true;
                continue;
            }
            throw new IllegalArgumentException(option + " not supported");
        }
        if (!JNAUtil.STDCALL_AVAILABLE) stdcall = false;
        Class<?>[] nativeParameterTypes = new Class[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i ++) {
            nativeParameterTypes[i] = toClass(parameterTypes[i]);
        }
        Class<?> nativeReturnType = toClass(returnType);
        CallbackProxy callbackHandler = new CallbackProxy() {
            @Override
            public Object callback(Object[] args) {
                try {
                    for (int i = 0; i < args.length; i ++) {
                        Object arg = args[i];
                        if (arg instanceof Pointer) args[i] = MemoryHandle.wrap(Pointer.nativeValue((Pointer) arg), parameterTypes[i].size());
                    }
                    Object result = JNAUtil.invoke(object, method, args);
                    if (result == null) throw new NullPointerException();
                    if (result instanceof MemoryHandle) {
                        MemoryHandle memoryHandle = (MemoryHandle) result;
                        if (memoryHandle.isNil()) throw new NullPointerException();
                        JNACompound compound = JNACompound.getInstance(memoryHandle);
                        compound.autoWrite();
                        result = compound;
                    }
                    return result;
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
            @Override
            public Class<?>[] getParameterTypes() {
                return nativeParameterTypes;
            }
            @Override
            public Class<?> getReturnType() {
                return nativeReturnType;
            }
        };
        CallbackProxy callback = stdcall ? new StdCallCallbackProxy() {
            @Override
            public Object callback(Object[] args) {
                return callbackHandler.callback(args);
            }
            @Override
            public Class<?>[] getParameterTypes() {
                return callbackHandler.getParameterTypes();
            }
            @Override
            public Class<?> getReturnType() {
                return callbackHandler.getReturnType();
            }
        } : callbackHandler;
        Cleaner.getCleaner().register(Modifier.isStatic(method.getModifiers()) ? method.getDeclaringClass() : object, new CallbackHolder(callback));
        return Pointer.nativeValue(CallbackReference.getFunctionPointer(callback));
    }

}
