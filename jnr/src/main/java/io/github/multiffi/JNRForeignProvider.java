package io.github.multiffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Closure;
import com.kenai.jffi.ClosureManager;
import com.kenai.jffi.Type;
import com.kenai.jffi.internal.Cleaner;
import jnr.ffi.JNRAccessor;
import jnr.ffi.Memory;
import jnr.ffi.NativeType;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import multiffi.CallOption;
import multiffi.CallOptionVisitor;
import multiffi.Foreign;
import multiffi.ForeignType;
import multiffi.FunctionHandle;
import multiffi.MemoryHandle;
import multiffi.ScalarType;
import multiffi.StandardCallOption;
import multiffi.spi.ForeignProvider;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JNRForeignProvider extends ForeignProvider {

    @Override
    public long addressSize() {
        return JNRUtil.UnsafeHolder.RUNTIME.addressSize();
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
        return JNRUtil.UnsafeHolder.RUNTIME.longSize();
    }

    @Override
    public long wcharSize() {
        return JNRUtil.WCHAR_SIZE;
    }

    @Override
    public long pageSize() {
        return JNRUtil.UnsafeHolder.PAGE_MANAGER.pageSize();
    }

    private static final class CharsetHolder {
        private CharsetHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Charset UTF16_CHARSET = JNRUtil.IS_BIG_ENDIAN ? Charset.forName("UTF-16BE") : Charset.forName("UTF-16LE");
        public static final Charset UTF32_CHARSET = JNRUtil.IS_BIG_ENDIAN ? Charset.forName("UTF-32BE") : Charset.forName("UTF-32LE");
        public static final Charset WIDE_CHARSET = JNRUtil.WCHAR_SIZE == 2 ? UTF16_CHARSET : UTF32_CHARSET;
        public static final Charset ANSI_CHARSET = Charset.forName(System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding", Charset.defaultCharset().name())));
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
        if (throwable != null) JNRUtil.UnsafeHolder.UNSAFE.throwException(throwable);
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
    public String getStackTraceString(Throwable throwable) {
        if (throwable == null) return null;
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
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
        return ByteOrder.nativeOrder();
    }

    @Override
    public void loadLibrary(String libraryName) throws IOException {
        Objects.requireNonNull(libraryName);
        try {
            JNRLibraryLookup.loadLibrary(libraryName, JNRAccessor.DEFAULT_SEARCH_PATHS, Collections.emptyMap());
        }
        catch (UnsatisfiedLinkError e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void loadLibrary(File libraryFile) throws IOException {
        Objects.requireNonNull(libraryFile);
        try {
            JNRLibraryLookup.loadLibrary(libraryFile.getAbsolutePath(), JNRAccessor.DEFAULT_SEARCH_PATHS, Collections.emptyMap());
        }
        catch (UnsatisfiedLinkError e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public long getSymbolAddress(String symbolName) {
        return JNRLibraryLookup.getSymbolAddress(symbolName);
    }

    @Override
    public String mapLibraryName(String libraryName) {
        if (JNRUtil.PLATFORM.getOS() == Platform.OS.AIX) {
            if ("lib.*\\.(so|a\\(shr.o\\)|a\\(shr_64.o\\)|a|so.[\\.0-9]+)$".matches(libraryName)) return libraryName;
            else return "lib" + libraryName + ".a";
        }
        else return JNRUtil.PLATFORM.mapLibraryName(libraryName);
    }

    @Override
    public int getLastErrno() {
        return JNRUtil.LastErrnoHolder.ERRNO_THREAD_LOCAL.get();
    }

    @Override
    public void setLastErrno(int errno) {
        JNRUtil.LastErrnoHolder.ERRNO_THREAD_LOCAL.set(errno);
    }

    private static final class ErrorStringMapperHolder {
        private ErrorStringMapperHolder() {
            throw new UnsupportedOperationException();
        }
        private static final class Windows {
            private Windows() {
                throw new UnsupportedOperationException();
            }
            private static final ThreadLocal<Pointer> POINTER_THREAD_LOCAL = new ThreadLocal<Pointer>() {
                @Override
                protected Pointer initialValue() {
                    return Memory.allocateDirect(JNRUtil.UnsafeHolder.RUNTIME, NativeType.ADDRESS);
                }
            };
            public static String strerror(int errno) {
                Pointer lpBuffer = POINTER_THREAD_LOCAL.get();
                lpBuffer.putAddress(0, 0L);
                try {
                    if (JNRLibraries.Kernel32.INSTANCE.FormatMessageW(0x00001000 /* FORMAT_MESSAGE_FROM_SYSTEM */ | 0x00000100 /* FORMAT_MESSAGE_ALLOCATE_BUFFER */,
                            0L,
                            errno,
                            0,
                            lpBuffer.address(),
                            0,
                            0L) == 0) return "FormatMessage failed with 0x" + Integer.toHexString(errno);
                    else return lpBuffer.getPointer(0).getString(0, Integer.MAX_VALUE - 8, CharsetHolder.WIDE_CHARSET);
                } finally {
                    Pointer hMem = lpBuffer.getPointer(0);
                    if (hMem != null) JNRLibraries.Kernel32.INSTANCE.LocalFree(hMem.address());
                }
            }
        }
    }

    @Override
    public String getErrorString(int errno) {
        if (JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS) return ErrorStringMapperHolder.Windows.strerror(errno);
        else {
            long errorString = JNRLibraries.CLibrary.INSTANCE.strerror(errno);
            return errorString == 0L ? "strerror failed with 0x" + Integer.toHexString(errno) :
                    CharsetHolder.ANSI_CHARSET.decode(ByteBuffer.wrap(JNRUtil.UnsafeHolder.MEMORY_IO.getZeroTerminatedByteArray(errorString))).toString();
        }
    }

    @Override
    public FunctionHandle downcallHandle(long address, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return new JNRFunctionHandle(address, firstVararg, returnType, parameterTypes, options);
    }

    private static class NativeInvocationHandler implements InvocationHandler {
        private final Map<Method, FunctionHandle> functionHandleMap;
        public NativeInvocationHandler(Class<?>[] classes, CallOptionVisitor callOptionVisitor) {
            Map<Method, FunctionHandle> functionHandleMap = new HashMap<>();
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
                            JNRUtil.checkType(parameterForeignTypes[i], parameterTypes[i + (addReturnMemoryParameter ? 1 : 0)]);
                        }
                        FunctionHandle functionHandle = new JNRFunctionHandle(address, firstVarargIndex, returnForeignType, parameterForeignTypes, callOptions);
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

    public static final class GetCallerClassHolder {
        private GetCallerClassHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Method getCallerClassReflectionMethod;
        public static final Method getInstanceStackWalkerMethod;
        public static final Method getCallerClassStackWalkerMethod;
        public static final Object retainClassReferenceOption;
        static {
            Method method;
            try {
                Class<?> reflectionClass = Class.forName("sun.reflect.Reflection");
                method = reflectionClass.getDeclaredMethod("getCallerClass");
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                method = null;
            }
            getCallerClassReflectionMethod = method;
            if (getCallerClassReflectionMethod == null) {
                Class<?> stackWalkerClass;
                Class<?> stackWalkerOptionClass;
                try {
                    stackWalkerClass = Class.forName("java.lang.StackWalker");
                    stackWalkerOptionClass = Class.forName("java.lang.StackWalker$Option");

                } catch (ClassNotFoundException e) {
                    stackWalkerClass = null;
                    stackWalkerOptionClass = null;
                }
                if (stackWalkerClass != null) {
                    Method _getInstanceStackWalkerMethod;
                    Method _getCallerClassStackWalkerMethod;
                    Object _retainClassReferenceOption;
                    try {
                        _getInstanceStackWalkerMethod = stackWalkerClass.getDeclaredMethod("getInstance", stackWalkerOptionClass);
                        _getCallerClassStackWalkerMethod = stackWalkerClass.getDeclaredMethod("getCallerClass");
                        Field field = stackWalkerOptionClass.getDeclaredField("RETAIN_CLASS_REFERENCE");
                        _retainClassReferenceOption = field.get(null);

                    } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
                        _getInstanceStackWalkerMethod = null;
                        _getCallerClassStackWalkerMethod = null;
                        _retainClassReferenceOption = null;
                    }
                    getInstanceStackWalkerMethod = _getInstanceStackWalkerMethod;
                    getCallerClassStackWalkerMethod = _getCallerClassStackWalkerMethod;
                    retainClassReferenceOption = _retainClassReferenceOption;
                }
                else {
                    getInstanceStackWalkerMethod = null;
                    getCallerClassStackWalkerMethod = null;
                    retainClassReferenceOption = null;
                }
            }
            else {
                getInstanceStackWalkerMethod = null;
                getCallerClassStackWalkerMethod = null;
                retainClassReferenceOption = null;
            }
        }
    }

    @Override
    public Object downcallProxy(ClassLoader classLoader, Class<?>[] classes, CallOptionVisitor callOptionVisitor) {
        if (classLoader == null) {
            Class<?> clazz;
            try {
                if (GetCallerClassHolder.getCallerClassReflectionMethod != null)
                    clazz = (Class<?>) GetCallerClassHolder.getCallerClassReflectionMethod.invoke(null);
                else if (GetCallerClassHolder.getInstanceStackWalkerMethod != null) {
                    Object stackWalker = GetCallerClassHolder.getInstanceStackWalkerMethod.invoke(
                            null, GetCallerClassHolder.retainClassReferenceOption
                    );
                    clazz = (Class<?>) GetCallerClassHolder.getCallerClassStackWalkerMethod.invoke(stackWalker);
                }
                else clazz = null;
            } catch (InvocationTargetException | IllegalAccessException e) {
                clazz = null;
            }
            classLoader = clazz == null ? ClassLoader.getSystemClassLoader() : clazz.getClassLoader();
        }
        return Proxy.newProxyInstance(classLoader, classes, new NativeInvocationHandler(classes, callOptionVisitor));
    }

    @Override
    public long upcallStub(Object object, Method method, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        if (!isStatic) Objects.requireNonNull(object);
        boolean stdcall = false;
        for (CallOption option : options) {
            if (option.equals(StandardCallOption.STDCALL)) {
                stdcall = true;
                continue;
            }
            throw new IllegalArgumentException(option + " not supported");
        }
        if (!JNRUtil.STDCALL_AVAILABLE) stdcall = false;
        Type returnFFIType = JNRUtil.toFFIType(returnType);
        Type[] parameterFFITypes = JNRUtil.toFFITypes(parameterTypes);
        Class<?>[] methodParameterTypes = method.getParameterTypes();
        for (int i = 0; i < methodParameterTypes.length; i ++) {
            JNRUtil.checkType(parameterTypes[i], methodParameterTypes[i]);
        }
        Closure closure = buffer -> {
            Object[] args = new Object[parameterTypes.length];
            int index = 0;
            for (int i = 0; i < parameterTypes.length; i ++) {
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
                else args[i] = MemoryHandle.wrap(buffer.getStruct(index));
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
        Cleaner.register(isStatic ? method.getDeclaringClass() : object, handle::dispose);
        return handle.getAddress();
    }

}
