package io.github.multiffi.ffi;

import com.sun.jna.CallbackProxy;
import com.sun.jna.CallbackReference;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.internal.Cleaner;
import com.sun.jna.win32.StdCallLibrary;
import multiffi.ffi.CallOption;
import multiffi.ffi.FunctionOptionVisitor;
import multiffi.ffi.ForeignType;
import multiffi.ffi.FunctionHandle;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.StandardCallOption;
import multiffi.ffi.spi.ForeignProvider;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

public class JNAForeignProvider extends ForeignProvider {

    public JNAForeignProvider() {
        this(true);
    }

    private final boolean proxyIntrinsics;
    public JNAForeignProvider(boolean proxyIntrinsics) {
        this.proxyIntrinsics = proxyIntrinsics;
    }

    @Override
    public long addressSize() {
        return Native.POINTER_SIZE;
    }

    @Override
    public long diffSize() {
        return Native.SIZE_T_SIZE;
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
        return JNAUtil.PAGE_SIZE;
    }

    @Override
    public long alignSize() {
        return JNAUtil.ALIGN_SIZE;
    }

    @Override
    public Charset ansiCharset() {
        return JNAUtil.ANSI_CHARSET;
    }

    @Override
    public Charset wideCharset() {
        return JNAUtil.WIDE_CHARSET;
    }

    @Override
    public Charset utf16Charset() {
        return JNAUtil.UTF16_CHARSET;
    }

    @Override
    public Charset utf32Charset() {
        return JNAUtil.UTF32_CHARSET;
    }

    @Override
    public void sneakyThrows(Throwable throwable) {
        if (throwable != null) JNAUtil.UNSAFE.throwException(throwable);
    }

    @Override
    public void exit(int status) {
        Runtime.getRuntime().exit(status);
    }

    @Override
    public void halt(int status) {
        Runtime.getRuntime().halt(status);
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
    public boolean isBigEndian() {
        return JNAUtil.IS_BIG_ENDIAN;
    }

    @Override
    public boolean isLittleEndian() {
        return !JNAUtil.IS_BIG_ENDIAN;
    }

    @Override
    public ByteOrder endianness() {
        return JNAUtil.NATIVE_ORDER;
    }

    @Override
    public void loadLibrary(String libraryName) throws UnsatisfiedLinkError {
        JNASymbolLookup.loadLibrary(libraryName);
    }

    @Override
    public void loadLibrary(File libraryFile) throws UnsatisfiedLinkError {
        JNASymbolLookup.loadLibrary(libraryFile);
    }

    @Override
    public long getSymbolAddress(String symbolName) {
        return JNASymbolLookup.getSymbolAddress(symbolName);
    }

    @Override
    public String mapLibraryName(String libraryName) {
        return JNAUtil.mapLibraryName(libraryName);
    }

    @Override
    public int getLastErrno() {
        return JNALastErrno.get();
    }

    @Override
    public void setLastErrno(int errno) {
        JNALastErrno.set(errno);
    }

    @Override
    public String getErrorString(int errno) {
        return JNAUtil.getErrorString(errno);
    }

    @Override
    public FunctionHandle downcallHandle(long address, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return new JNAFunctionHandle(address, firstVarArgIndex, returnType, parameterTypes, options);
    }

    @Override
    public Object downcallProxy(ClassLoader classLoader, Class<?>[] classes, FunctionOptionVisitor functionOptionVisitor) {
        if (functionOptionVisitor == null) functionOptionVisitor = Util.DEFAULT_SIGNATURE_VISITOR;
        if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
        return (proxyIntrinsics && JNAUtil.PROXY_INTRINSICS) ? JNAASMRuntime.generateProxy(classLoader, classes, functionOptionVisitor) :
                classes == null || classes.length == 0 ? null : Proxy.newProxyInstance(classLoader, classes, new ForeignInvocationHandler(this, classes, functionOptionVisitor));
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

    private interface StdCallCallbackProxy extends StdCallLibrary.StdCallCallback, CallbackProxy {}

    @Override
    public MemoryHandle upcallStub(Object object, Method method, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        boolean stdcall = false;
        if (options != null) {
            for (CallOption option : options) {
                if (option.equals(StandardCallOption.STDCALL)) {
                    stdcall = true;
                    continue;
                }
                throw new IllegalArgumentException(option + " not supported");
            }
            if (!JNAUtil.STDCALL_SUPPORTED) stdcall = false;
        }
        Class<?>[] parameterNativeTypes;
        if (parameterTypes == null || parameterTypes.length == 1 && parameterTypes[0] == null)
            parameterNativeTypes = JNAUtil.EMPTY_CLASS_ARRAY;
        else {
            Class<?>[] methodParameterTypes = method.getParameterTypes();
            parameterNativeTypes = new Class<?>[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i ++) {
                parameterNativeTypes[i] = JNAUtil.toNativeType(Objects.requireNonNull(parameterTypes[i]));
                Util.checkType(parameterTypes[i], methodParameterTypes[i]);
            }
        }
        Class<?> returnNativeType = JNAUtil.toNativeType(returnType);
        Util.checkType(returnType, method.getReturnType());
        CallbackProxy callbackHandler = new CallbackProxy() {
            @Override
            public Object callback(Object[] args) {
                try {
                    for (int i = 0; i < args.length; i ++) {
                        Object arg = args[i];
                        if (arg instanceof Pointer) args[i] = MemoryHandle.wrap(Pointer.nativeValue((Pointer) arg), Native.getNativeSize(parameterNativeTypes[i]));
                    }
                    Object result = JNAUtil.invoke(object, method, args);
                    if (returnNativeType != void.class && result == null) throw new NullPointerException();
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
                return parameterNativeTypes;
            }
            @Override
            public Class<?> getReturnType() {
                return returnNativeType;
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
        Cleaner.Cleanable cleanup = Cleaner.getCleaner().register(Modifier.isStatic(method.getModifiers()) ? method.getDeclaringClass() : Objects.requireNonNull(object),
                new CallbackHolder(callback));
        long address = Pointer.nativeValue(CallbackReference.getFunctionPointer(callback));
        return new DirectWrapperMemoryHandle(address, 0) {
            @Override
            protected void free(long address) {
                cleanup.clean();
            }
        };
    }

    @Override
    public Runnable registerCleaner(Object object, Runnable cleanup) {
        return Cleaner.getCleaner().register(Objects.requireNonNull(object), Objects.requireNonNull(cleanup))::clean;
    }

}
