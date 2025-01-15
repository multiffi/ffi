package io.github.multiffi.ffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Closure;
import com.kenai.jffi.ClosureManager;
import com.kenai.jffi.Type;
import com.kenai.jffi.internal.Cleaner;
import multiffi.ffi.CallOption;
import multiffi.ffi.FunctionOptionVisitor;
import multiffi.ffi.Foreign;
import multiffi.ffi.ForeignType;
import multiffi.ffi.FunctionHandle;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
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

public class JNRForeignProvider extends ForeignProvider {

    public JNRForeignProvider() {
        this(true);
    }

    private final boolean proxyIntrinsics;
    public JNRForeignProvider(boolean proxyIntrinsics) {
        this.proxyIntrinsics = proxyIntrinsics;
    }

    @Override
    public long addressSize() {
        return JNRUtil.ADDRESS_SIZE;
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
        return JNRUtil.LONG_SIZE;
    }

    @Override
    public long wcharSize() {
        return JNRUtil.WCHAR_SIZE;
    }

    @Override
    public long pageSize() {
        return JNRUtil.PAGE_SIZE;
    }

    @Override
    public long alignmentSize() {
        return JNRUtil.ALIGNMENT_SIZE;
    }

    @Override
    public Charset ansiCharset() {
        return JNRUtil.ANSI_CHARSET;
    }

    @Override
    public Charset wideCharset() {
        return JNRUtil.WIDE_CHARSET;
    }

    @Override
    public Charset utf16Charset() {
        return JNRUtil.UTF16_CHARSET;
    }

    @Override
    public Charset utf32Charset() {
        return JNRUtil.UTF32_CHARSET;
    }

    @Override
    public void sneakyThrows(Throwable throwable) {
        if (throwable != null) JNRUtil.UNSAFE.throwException(throwable);
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
        return JNRCurrentTime.seconds();
    }

    @Override
    public int currentTimeNanos() {
        return JNRCurrentTime.nanos();
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
        return JNRUtil.IS_BIG_ENDIAN;
    }

    @Override
    public boolean isLittleEndian() {
        return !JNRUtil.IS_BIG_ENDIAN;
    }

    @Override
    public ByteOrder endianness() {
        return JNRUtil.NATIVE_ORDER;
    }

    @Override
    public void loadLibrary(String libraryName) throws UnsatisfiedLinkError {
        JNRLibraryLookup.loadLibrary(libraryName);
    }

    @Override
    public void loadLibrary(File libraryFile) throws UnsatisfiedLinkError {
        JNRLibraryLookup.loadLibrary(libraryFile);
    }

    @Override
    public long getSymbolAddress(String symbolName) {
        return JNRLibraryLookup.getSymbolAddress(symbolName);
    }

    @Override
    public String mapLibraryName(String libraryName) {
        return JNRUtil.mapLibraryName(libraryName);
    }

    @Override
    public int getLastErrno() {
        return JNRLastErrno.get();
    }

    @Override
    public void setLastErrno(int errno) {
        JNRLastErrno.set(errno);
    }

    @Override
    public String getErrorString(int errno) {
        return JNRUtil.getErrorString(errno);
    }

    @Override
    public FunctionHandle downcallHandle(long address, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return new JNRFunctionHandle(address, firstVarArgIndex, returnType, parameterTypes, options);
    }

    @Override
    public Object downcallProxy(ClassLoader classLoader, Class<?>[] classes, FunctionOptionVisitor functionOptionVisitor) {
        if (functionOptionVisitor == null) functionOptionVisitor = Util.DEFAULT_SIGNATURE_VISITOR;
        if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
        return (proxyIntrinsics && JNRUtil.PROXY_INTRINSICS) ? JNRASMRuntime.generateProxy(classLoader, classes, functionOptionVisitor) :
                classes == null || classes.length == 0 ? null : Proxy.newProxyInstance(classLoader, classes, new ForeignInvocationHandler(this, classes, functionOptionVisitor));
    }

    @Override
    public MemoryHandle upcallStub(Object object, Method method, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        if (!isStatic) Objects.requireNonNull(object);
        boolean stdcall = false;
        if (options != null) {
            for (CallOption option : options) {
                if (option.equals(StandardCallOption.STDCALL)) {
                    stdcall = true;
                    continue;
                }
                throw new IllegalArgumentException(option + " not supported");
            }
            if (!JNRUtil.STDCALL_SUPPORTED) stdcall = false;
        }
        Type[] parameterFFITypes;
        if (parameterTypes == null || parameterTypes.length == 1 && parameterTypes[0] == null)
            parameterFFITypes = JNRUtil.EMPTY_TYPE_ARRAY;
        else {
            Class<?>[] methodParameterTypes = method.getParameterTypes();
            parameterFFITypes = new Type[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i ++) {
                parameterFFITypes[i] = JNRUtil.toFFIType(Objects.requireNonNull(parameterTypes[i]));
                Util.checkType(parameterTypes[i], methodParameterTypes[i]);
            }
        }
        Type returnFFIType = JNRUtil.toFFIType(returnType);
        Util.checkType(returnType, method.getReturnType());
        Closure closure = buffer -> {
            Object[] args = new Object[parameterFFITypes.length];
            int index = 0;
            for (int i = 0; i < parameterFFITypes.length; i ++) {
                ForeignType parameterType = parameterTypes[i];
                if (parameterType == ScalarType.BOOLEAN) args[i] = buffer.getByte(index) != 0;
                else if (parameterType == ScalarType.INT8 || parameterType == ScalarType.CHAR) args[i] = buffer.getByte(index);
                else if (parameterType == ScalarType.UTF16 || (parameterType == ScalarType.WCHAR && Foreign.wcharSize() == 2L))
                    args[i] = (char) buffer.getShort(index);
                else if (parameterType == ScalarType.INT16 || parameterType == ScalarType.SHORT)
                    args[i] = buffer.getShort(index);
                else if (parameterType == ScalarType.INT32 || parameterType == ScalarType.INT
                        || (parameterType == ScalarType.LONG && Foreign.longSize() == 4L)
                        || (parameterType == ScalarType.SIZE && Foreign.addressSize() == 4L)
                        || (parameterType == ScalarType.WCHAR && Foreign.wcharSize() == 4L))
                    args[i] = buffer.getInt(index);
                else if (parameterType == ScalarType.INT64
                        || (parameterType == ScalarType.LONG && Foreign.longSize() == 8L)
                        || (parameterType == ScalarType.SIZE && Foreign.addressSize() == 8L)) {
                    args[i] = buffer.getLong(index);
                    if (Foreign.addressSize() == 4L) index ++;
                }
                else if (parameterType == ScalarType.FLOAT) args[i] = buffer.getFloat(index);
                else if (parameterType == ScalarType.DOUBLE) {
                    args[i] = buffer.getDouble(index);
                    if (Foreign.addressSize() == 4L) index ++;
                }
                else if (parameterType == ScalarType.ADDRESS) args[i] = buffer.getAddress(index);
                else args[i] = MemoryHandle.wrap(buffer.getStruct(index), parameterFFITypes[i].size());
            }
            Object result;
            try {
                result = JNRUtil.invoke(isStatic ? null : object, method, args);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
            if (returnType != null && result == null) throw new NullPointerException();
            if (returnType == ScalarType.BOOLEAN) buffer.setByteReturn((byte) (((boolean) result) ? 1 : 0));
            else if (returnType == ScalarType.INT8 || returnType == ScalarType.CHAR) buffer.setByteReturn((byte) result);
            else if (returnType == ScalarType.UTF16 || (returnType == ScalarType.WCHAR && Foreign.wcharSize() == 2L))
                buffer.setShortReturn((short) (char) result);
            else if (returnType == ScalarType.INT16 || returnType == ScalarType.SHORT)
                buffer.setShortReturn((short) result);
            else if (returnType == ScalarType.INT32 || returnType == ScalarType.INT
                    || (returnType == ScalarType.LONG && Foreign.longSize() == 4L)
                    || (returnType == ScalarType.SIZE && Foreign.addressSize() == 4L)
                    || (returnType == ScalarType.WCHAR && Foreign.wcharSize() == 4L))
                buffer.setIntReturn((int) result);
            else if (returnType == ScalarType.INT64
                    || (returnType == ScalarType.LONG && Foreign.longSize() == 8L)
                    || (returnType == ScalarType.SIZE && Foreign.addressSize() == 8L))
                buffer.setLongReturn((long) result);
            else if (returnType == ScalarType.FLOAT) buffer.setFloatReturn((float) result);
            else if (returnType == ScalarType.DOUBLE) buffer.setDoubleReturn((double) result);
            else if (returnType == ScalarType.ADDRESS) buffer.setAddressReturn((long) result);
            else if (returnType != null) {
                MemoryHandle memoryHandle = (MemoryHandle) result;
                if (memoryHandle.isNil()) throw new NullPointerException();
                if (memoryHandle.isDirect()) buffer.setStructReturn(memoryHandle.address());
                else if (memoryHandle.array() instanceof byte[] && memoryHandle.arrayOffset() >= 0 && memoryHandle.arrayOffset() < (Integer.MAX_VALUE - 8))
                    buffer.setStructReturn((byte[]) memoryHandle.array(), (int) memoryHandle.arrayOffset());
                else {
                    byte[] array = new byte[(int) returnType.size()];
                    memoryHandle.getInt8Array(0, array);
                    buffer.setStructReturn(array, 0);
                }
            }
        };
        Closure.Handle handle = ClosureManager.getInstance().newClosure(closure,
                returnFFIType, parameterFFITypes, stdcall ? CallingConvention.STDCALL : CallingConvention.DEFAULT);
        handle.setAutoRelease(false);
        Runnable cleanup = Cleaner.register(isStatic ? method.getDeclaringClass() : object, handle::dispose);
        long address = handle.getAddress();
        return new DirectWrapperMemoryHandle(address, 0) {
            @Override
            protected void free(long address) {
                cleanup.run();
            }
        };
    }

    @Override
    public Runnable registerCleaner(Object object, Runnable cleanup) {
        return Cleaner.register(object, cleanup);
    }

}
