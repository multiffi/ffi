package io.github.multiffi.ffi;

import com.sun.jna.Function;
import com.sun.jna.JNAAccessor;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import multiffi.ffi.ForeignType;
import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.ProtectionDomain;
import java.util.Objects;

@SuppressWarnings({"deprecation", "removal"})
public final class JNAUtil {

    private JNAUtil() {
        throw new AssertionError("No io.github.multiffi.ffi.JNAUtil instances for you!");
    }

    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    public static final boolean STDCALL_SUPPORTED = Platform.isWindows() && !Platform.isWindowsCE() && !Platform.is64Bit();
    public static final boolean PROXY_INTRINSICS = Util.getBooleanProperty("multiffi.foreign.proxyIntrinsics", true)
            && !"dalvik".equalsIgnoreCase(System.getProperty("java.vm.name"));
    public static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();
    public static final boolean IS_BIG_ENDIAN = NATIVE_ORDER.equals(ByteOrder.BIG_ENDIAN);

    public static final Unsafe UNSAFE;
    public static final Object IMPL_LOOKUP;

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

    public static final long PAGE_SIZE = UNSAFE.pageSize() & 0xFFFFFFFFL;
    public static final long ALIGNMENT_SIZE = Platform.isWindows() ? Native.POINTER_SIZE * 2L : Native.POINTER_SIZE;

    public static final Charset UTF16_CHARSET = JNAUtil.IS_BIG_ENDIAN ? Charset.forName("UTF-16BE") : Charset.forName("UTF-16LE");
    public static final Charset UTF32_CHARSET = JNAUtil.IS_BIG_ENDIAN ? Charset.forName("UTF-32BE") : Charset.forName("UTF-32LE");
    public static final Charset WIDE_CHARSET = Native.WCHAR_SIZE == 2 ? UTF16_CHARSET : UTF32_CHARSET;
    public static final Charset ANSI_CHARSET = Charset.forName(System.getProperty("jna.encoding", System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding", Charset.defaultCharset().name()))));

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

    public static String mapLibraryName(String libraryName) {
        if (libraryName == null) return null;
        else if (new File(libraryName).isAbsolute()) return libraryName;
        else {
            if (Platform.isMac()) {
                if (libraryName.startsWith("lib") && (libraryName.endsWith(".dylib") || libraryName.endsWith(".jnilib"))) {
                    return libraryName;
                }
                String name = System.mapLibraryName(libraryName);
                // On MacOSX, System.mapLibraryName() returns the .jnilib extension
                // (the suffix for JNI libraries); ordinarily shared libraries have
                // a .dylib suffix
                if (name.endsWith(".jnilib")) return name.substring(0, name.lastIndexOf(".jnilib")) + ".dylib";
                else return name;
            }
            else if (Platform.isAIX()) {    // can be libx.a, libx.a(shr.o), libx.a(shr_64.o), libx.so
                if (isVersionedName(libraryName)
                        || (libraryName.startsWith("lib") && (libraryName.endsWith(".so") || libraryName.endsWith(".a") || libraryName.endsWith(".a(shr.o)") || libraryName.endsWith(".a(shr_64.o)")))) {
                    // A specific version was requested - use as is for search
                    return libraryName;
                }
            }
            else if (Platform.isWindows()) {
                if (libraryName.endsWith(".drv") || libraryName.endsWith(".dll") || libraryName.endsWith(".ocx"))
                    return libraryName;
            }
            else /* if (Platform.isLinux() || Platform.isFreeBSD()) */ {
                if (isVersionedName(libraryName) || libraryName.endsWith(".so")) {
                    // A specific version was requested - use as is for search
                    return libraryName;
                }
            }
            String mappedName = System.mapLibraryName(libraryName);
            if (Platform.isAIX() && mappedName.endsWith(".so")) return mappedName.replaceAll(".so$", ".a");
            else return mappedName;
        }
    }

    private static boolean isVersionedName(String libraryName) {
        if (libraryName.startsWith("lib")) {
            int so = libraryName.lastIndexOf(".so.");
            if (so != -1 && so + 4 < libraryName.length()) {
                for (int i = so + 4; i < libraryName.length(); i++) {
                    char ch = libraryName.charAt(i);
                    if (!Character.isDigit(ch) && ch != '.') return false;
                }
                return true;
            }
        }
        return false;
    }

    private static final JNAAccessor.NativeAccessor NATIVE = JNAAccessor.getNativeAccessor();
    public static Object invoke(Object returnType, Function function, int callFlags, Object... args) {
        long address = Pointer.nativeValue(function);
        if (returnType == null || returnType == void.class || returnType == Void.class) {
            NATIVE.invokeVoid(function, address, callFlags, args);
            return null;
        }
        else if (returnType == boolean.class || returnType == Boolean.class)
            return NATIVE.invokeInt(function, address, callFlags, args) != 0;
        else if (returnType == byte.class || returnType == Byte.class)
            return (byte) NATIVE.invokeInt(function, address, callFlags, args);
        else if (returnType == short.class || returnType == Short.class)
            return (short) NATIVE.invokeInt(function, address, callFlags, args);
        else if (returnType == int.class || returnType == Integer.class)
            return NATIVE.invokeInt(function, address, callFlags, args);
        else if (returnType == long.class || returnType == Long.class)
            return NATIVE.invokeLong(function, address, callFlags, args);
        else if (returnType == float.class || returnType == Float.class)
            return NATIVE.invokeFloat(function, address, callFlags, args);
        else if (returnType == double.class || returnType == Double.class)
            return NATIVE.invokeDouble(function, address, callFlags, args);
        else if (returnType == char.class || returnType == Character.class)
            return (char) NATIVE.invokeInt(function, address, callFlags, args);
        else if (returnType == Pointer.class)
            return NATIVE.invokePointer(function, address, callFlags, args);
        else return NATIVE.invokeStructure((Structure) returnType, function, address, callFlags, args);
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

    private static final class ErrorStringMapper {
        private ErrorStringMapper() {
            throw new UnsupportedOperationException();
        }
        private static final class Kernel32 {
            private Kernel32() {
                throw new UnsupportedOperationException();
            }
            public static native int FormatMessageW(int dwFlags, Pointer lpSource, int dwMessageId, int dwLanguageId, Pointer lpBuffer, int nSize, Pointer arguments);
            public static native Pointer LocalFree(Pointer hMem);
            static {
                if (Platform.isWindows()) Native.register(Kernel32.class, Platform.isWindowsCE() ? "coredll" : "kernel32");
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
                    else return lpBuffer.getPointer(0).getString(0, WIDE_CHARSET.name());
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
    
    public static String getErrorString(int errno) {
        if (Platform.isWindows()) return ErrorStringMapper.Kernel32.strerror(errno);
        else {
            Pointer errorString = ErrorStringMapper.CLibrary.strerror(errno);
            return errorString == null ? "strerror failed with 0x" + Integer.toHexString(errno) : errorString.getString(0, ANSI_CHARSET.name());
        }
    }

    public static Class<?> toNativeType(ForeignType type) {
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

}
