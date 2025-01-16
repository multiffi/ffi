package io.github.multiffi.ffi;

import com.kenai.jffi.Array;
import com.kenai.jffi.MemoryIO;
import com.kenai.jffi.PageManager;
import com.kenai.jffi.Type;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Memory;
import jnr.ffi.NativeType;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.TypeAlias;
import jnr.ffi.annotations.IgnoreError;
import jnr.ffi.types.caddr_t;
import multiffi.ffi.Foreign;
import multiffi.ffi.ForeignType;
import multiffi.ffi.ScalarType;
import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"deprecation", "removal"})
public final class JNRUtil {

    private JNRUtil() {
        throw new AssertionError("No io.github.multiffi.ffi.JNRUtil instances for you!");
    }

    public static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

    public static final Platform PLATFORM = Platform.getNativePlatform();
    public static final boolean STDCALL_SUPPORTED = PLATFORM.getOS() == Platform.OS.WINDOWS
            && PLATFORM.is32Bit() && !PLATFORM.getOSName().startsWith("Windows CE");
    public static final boolean PROXY_INTRINSICS = Util.getBooleanProperty("multiffi.foreign.proxyIntrinsics", true)
            && Util.getBooleanProperty("jnr.ffi.asm.enabled", true)
            && !"dalvik".equalsIgnoreCase(System.getProperty("java.vm.name"));
    public static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();
    public static final boolean IS_BIG_ENDIAN = NATIVE_ORDER.equals(ByteOrder.BIG_ENDIAN);

    public static final Runtime RUNTIME = Runtime.getSystemRuntime();
    public static final MemoryIO MEMORY_IO = MemoryIO.getInstance();
    public static final PageManager PAGE_MANAGER = PageManager.getInstance();
    public static final Unsafe UNSAFE;
    public static final Object IMPL_LOOKUP;

    public static final long ADDRESS_SIZE = RUNTIME.addressSize();
    public static final long DIFF_SIZE = RUNTIME.findType(TypeAlias.size_t).size();
    public static final long LONG_SIZE = RUNTIME.longSize();
    public static final long PAGE_SIZE = PAGE_MANAGER.pageSize();
    public static final long ALIGN_SIZE = PLATFORM.getOS() == Platform.OS.WINDOWS ? ADDRESS_SIZE * 2L : ADDRESS_SIZE;
    public static final long WCHAR_SIZE = PLATFORM.getOS() == Platform.OS.WINDOWS ? 2L : 4L;

    public static final Charset UTF16_CHARSET = IS_BIG_ENDIAN ? Charset.forName("UTF-16BE") : Charset.forName("UTF-16LE");
    public static final Charset UTF32_CHARSET = IS_BIG_ENDIAN ? Charset.forName("UTF-32BE") : Charset.forName("UTF-32LE");
    public static final Charset WIDE_CHARSET = WCHAR_SIZE == 2 ? UTF16_CHARSET : UTF32_CHARSET;
    public static final Charset ANSI_CHARSET = Charset.forName(System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding", Charset.defaultCharset().name())));

    private static final Method unreflectMethod;
    private static final Method unreflectConstructorMethod;
    private static final Method bindToMethod;
    private static final Method invokeWithArgumentsMethod;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to get the sun.misc.Unsafe instance");
        }
        Object _lookup;
        Method _unreflectMethod;
        Method _unreflectConstructorMethod;
        Method _bindToMethod;
        Method _invokeWithArgumentsMethod;
        try {
            Class<?> lookupClass = Class.forName("java.lang.invoke.MethodHandles$Lookup");
            Field field = lookupClass.getDeclaredField("IMPL_LOOKUP");
            _lookup = UNSAFE.getObject(lookupClass, UNSAFE.staticFieldOffset(field));
            _unreflectMethod = lookupClass.getDeclaredMethod("unreflect", Method.class);
            _unreflectConstructorMethod = lookupClass.getDeclaredMethod("unreflectConstructor", Constructor.class);
            Class<?> methodHandleClass = Class.forName("java.lang.invoke.MethodHandle");
            _bindToMethod = methodHandleClass.getDeclaredMethod("bindTo", Object.class);
            _invokeWithArgumentsMethod = methodHandleClass.getDeclaredMethod("invokeWithArguments", Object[].class);
        } catch (NoSuchFieldException | ClassNotFoundException | NoSuchMethodException e) {
            _lookup = null;
            _unreflectMethod = null;
            _unreflectConstructorMethod = null;
            _bindToMethod = null;
            _invokeWithArgumentsMethod = null;
        }
        IMPL_LOOKUP = _lookup;
        unreflectMethod = _unreflectMethod;
        unreflectConstructorMethod = _unreflectConstructorMethod;
        bindToMethod = _bindToMethod;
        invokeWithArgumentsMethod = _invokeWithArgumentsMethod;
    }

    public static Object invoke(Object object, Method method, Object... args) throws Throwable {
        if (IMPL_LOOKUP == null) {
            method.setAccessible(true);
            try {
                return method.invoke(Modifier.isStatic(method.getModifiers()) ? null : Objects.requireNonNull(object), args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception", e);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
        else {
            try {
                Object methodHandle = unreflectMethod.invoke(IMPL_LOOKUP, method);
                if (!Modifier.isStatic(method.getModifiers()))
                    methodHandle = bindToMethod.invoke(methodHandle, Objects.requireNonNull(object));
                return invokeWithArgumentsMethod.invoke(methodHandle, (Object) args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception", e);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Constructor<T> constructor, Object... args) throws Throwable {
        if (IMPL_LOOKUP == null) {
            constructor.setAccessible(true);
            try {
                return constructor.newInstance(args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception", e);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
        else {
            try {
                Object methodHandle = unreflectConstructorMethod.invoke(IMPL_LOOKUP, constructor);
                return (T) invokeWithArgumentsMethod.invoke(methodHandle, (Object) args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception", e);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }

    public static final List<JNRInvoker> FAST_INVOKERS = Collections.unmodifiableList(Arrays.asList(
            JNRInvoker.FastInt.I0, JNRInvoker.FastInt.I1, JNRInvoker.FastInt.I2, JNRInvoker.FastInt.I3,
            JNRInvoker.FastInt.I4, JNRInvoker.FastInt.I5, JNRInvoker.FastInt.I6,
            JNRInvoker.FastLong.L0, JNRInvoker.FastLong.L1, JNRInvoker.FastLong.L2, JNRInvoker.FastLong.L3,
            JNRInvoker.FastLong.L4, JNRInvoker.FastLong.L5, JNRInvoker.FastLong.L6,
            JNRInvoker.FastNumeric.N0, JNRInvoker.FastNumeric.N1, JNRInvoker.FastNumeric.N2, JNRInvoker.FastNumeric.N3,
            JNRInvoker.FastNumeric.N4, JNRInvoker.FastNumeric.N5, JNRInvoker.FastNumeric.N6
    ));

    public static Type toFFIType(ForeignType foreignType) {
        if (foreignType == null) return Type.VOID;
        else if (foreignType == ScalarType.INT8) return Type.SINT8;
        else if (foreignType == ScalarType.UTF16 || (foreignType == ScalarType.WCHAR && Foreign.wcharSize() == 2L)) return Type.UINT16;
        else if (foreignType == ScalarType.INT16) return Type.SINT16;
        else if (foreignType == ScalarType.INT32
                || (foreignType == ScalarType.SIZE && Foreign.diffSize() == 4L)
                || (foreignType == ScalarType.WCHAR && Foreign.wcharSize() == 4L)
                || (foreignType == ScalarType.BOOLEAN && Foreign.addressSize() == 4L)) return Type.SINT32;
        else if (foreignType == ScalarType.INT64
                || (foreignType == ScalarType.SIZE && Foreign.diffSize() == 8L)
                || (foreignType == ScalarType.BOOLEAN && Foreign.addressSize() == 8L)) return Type.SINT64;
        else if (foreignType == ScalarType.CHAR) return Type.SCHAR;
        else if (foreignType == ScalarType.SHORT) return Type.SSHORT;
        else if (foreignType == ScalarType.INT) return Type.SINT;
        else if (foreignType == ScalarType.LONG) return Type.SLONG;
        else if (foreignType == ScalarType.FLOAT) return Type.FLOAT;
        else if (foreignType == ScalarType.DOUBLE) return Type.DOUBLE;
        else if (foreignType == ScalarType.ADDRESS) return Type.POINTER;
        else {
            long size = foreignType.size();
            if (size < 0 || size > (Integer.MAX_VALUE - 8)) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(size));
            int length = (int) size;
            return Array.newArray(Type.SINT8, length);
        }
    }

    public static Type[] toFFITypes(ForeignType[] foreignTypes) {
        Type[] types = new Type[foreignTypes.length];
        for (int i = 0; i < foreignTypes.length; i ++) {
            types[i] = toFFIType(Objects.requireNonNull(foreignTypes[i]));
        }
        return types;
    }

    public static Type[] toFFITypes(List<ForeignType> foreignTypes) {
        Type[] types = new Type[foreignTypes.size()];
        for (int i = 0; i < foreignTypes.size(); i ++) {
            types[i] = JNRUtil.toFFIType(Objects.requireNonNull(foreignTypes.get(i)));
        }
        return types;
    }

    public static String mapLibraryName(String libraryName) {
        if (libraryName == null) return null;
        else if (new File(libraryName).isAbsolute()) return libraryName;
        else if (PLATFORM.getOS() == Platform.OS.AIX) {
            if ("lib.*\\.(so|a\\(shr.o\\)|a\\(shr_64.o\\)|a|so.[\\.0-9]+)$".matches(libraryName)) return libraryName;
            else return "lib" + libraryName + ".a";
        }
        else return PLATFORM.mapLibraryName(libraryName);
    }

    public static final Method defineClassMethod;
    static {
        try {
            defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }

    public static Class<?> defineClass(ClassLoader classLoader, String name, byte[] bytecode, int offset, int length, ProtectionDomain protectionDomain) {
        try {
            return (Class<?>) invoke(classLoader, defineClassMethod, name, bytecode, offset, length, protectionDomain);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public static Class<?> defineClass(ClassLoader classLoader, String name, byte[] bytecode) {
        return defineClass(classLoader, name, bytecode, 0, bytecode.length, null);
    }

    public interface Kernel32 {
        Kernel32 INSTANCE = PLATFORM.getOS() != Platform.OS.WINDOWS ? null :
                LibraryLoader.create(Kernel32.class).load(PLATFORM.getName().startsWith("Windows CE") ? "coredll" : "kernel32");
        @IgnoreError
        int FormatMessageW(int dwFlags, @caddr_t long lpSource, int dwMessageId, int dwLanguageId,
                           @caddr_t long lpBuffer, int nSize, @caddr_t long arguments);
        @IgnoreError
        @caddr_t long LocalFree(@caddr_t long hMem);
    }

    public interface CLibrary {
        CLibrary INSTANCE = PLATFORM.getOS() == Platform.OS.WINDOWS ? null :
                LibraryLoader.create(CLibrary.class).load(PLATFORM.getStandardCLibraryName());
        @IgnoreError
        @caddr_t long strerror(int errno);
    }

    private static final class WindowsErrorStringMapper {
        private WindowsErrorStringMapper() {
            throw new UnsupportedOperationException();
        }
        private static final ThreadLocal<Pointer> POINTER_THREAD_LOCAL = new ThreadLocal<Pointer>() {
            @Override
            protected Pointer initialValue() {
                return Memory.allocateDirect(RUNTIME, NativeType.ADDRESS);
            }
        };
        public static String strerror(int errno) {
            Pointer lpBuffer = POINTER_THREAD_LOCAL.get();
            lpBuffer.putAddress(0, 0L);
            try {
                if (Kernel32.INSTANCE.FormatMessageW(0x00001000 /* FORMAT_MESSAGE_FROM_SYSTEM */ | 0x00000100 /* FORMAT_MESSAGE_ALLOCATE_BUFFER */,
                        0L,
                        errno,
                        0,
                        lpBuffer.address(),
                        0,
                        0L) == 0) return "FormatMessage failed with 0x" + Integer.toHexString(errno);
                else return lpBuffer.getPointer(0).getString(0, Integer.MAX_VALUE - 8, WIDE_CHARSET);
            } finally {
                Pointer hMem = lpBuffer.getPointer(0);
                if (hMem != null) Kernel32.INSTANCE.LocalFree(hMem.address());
            }
        }
    }

    public static String getErrorString(int errno) {
        if (PLATFORM.getOS() == Platform.OS.WINDOWS) return WindowsErrorStringMapper.strerror(errno);
        else {
            long errorString = CLibrary.INSTANCE.strerror(errno);
            return errorString == 0L ? "strerror failed with 0x" + Integer.toHexString(errno) :
                    ANSI_CHARSET.decode(ByteBuffer.wrap(MEMORY_IO.getZeroTerminatedByteArray(errorString))).toString();
        }
    }

}
