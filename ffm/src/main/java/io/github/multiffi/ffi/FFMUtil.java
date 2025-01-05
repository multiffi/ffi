package io.github.multiffi.ffi;

import multiffi.ffi.Foreign;
import multiffi.ffi.ForeignType;
import multiffi.ffi.ScalarType;
import sun.misc.Unsafe;

import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.security.ProtectionDomain;
import java.util.Locale;
import java.util.Objects;

@SuppressWarnings({"deprecation", "removal"})
final class FFMUtil {

    private FFMUtil() {
        throw new AssertionError("No io.github.multiffi.ffi.FFMUtil instances for you!");
    }

    public static final String OS_NAME = System.getProperty("os.name");
    private static boolean startsWithIgnoreCase(String string, String prefix) {
        if (string == null) return false;
        else return string.startsWith(prefix)
                || string.toUpperCase(Locale.ENGLISH).startsWith(prefix.toUpperCase(Locale.ENGLISH))
                || string.toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH));
    }
    public static final boolean IS_WINDOWS = startsWithIgnoreCase(OS_NAME, "windows");
    public static final boolean IS_WINDOWS_CE = startsWithIgnoreCase(OS_NAME, "windowsce");
    public static final boolean IS_MAC = startsWithIgnoreCase(OS_NAME, "mac") || startsWithIgnoreCase(OS_NAME, "darwin");
    public static final boolean IS_AIX = startsWithIgnoreCase(OS_NAME, "aix");
    public static final boolean IS_IBMI = startsWithIgnoreCase(OS_NAME, "os/400") || startsWithIgnoreCase(OS_NAME, "os400");
    public static final boolean IS_LINUX = startsWithIgnoreCase(OS_NAME, "linux");
    public static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

    public static final class UnsafeHolder {
        private UnsafeHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Unsafe UNSAFE;
        public static final MethodHandles.Lookup IMPL_LOOKUP;
        static {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                UNSAFE = (Unsafe) field.get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException("Failed to get the sun.misc.Unsafe instance");
            }
            try {
                MethodHandles.publicLookup().ensureInitialized(MethodHandles.Lookup.class);
                Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                IMPL_LOOKUP = (MethodHandles.Lookup) UNSAFE.getObject(MethodHandles.Lookup.class, UNSAFE.staticFieldOffset(field));
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to get the trusted java.lang.invoke.MethodHandles.Lookup instance");
            }
        }
    }

    public static final class ABIHolder {
        private ABIHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Linker LINKER = Linker.nativeLinker();
        public static final ValueLayout SIZE_T = (ValueLayout) LINKER.canonicalLayouts().get("size_t");
        public static final ValueLayout SHORT = (ValueLayout) LINKER.canonicalLayouts().get("short");
        public static final ValueLayout INT = (ValueLayout) LINKER.canonicalLayouts().get("int");
        public static final ValueLayout LONG = (ValueLayout) LINKER.canonicalLayouts().get("long");
        public static final MemoryLayout WCHAR_T = LINKER.canonicalLayouts().get("wchar_t");
    }

    private static final class DefineClassHolder {
        private DefineClassHolder() {
            throw new UnsupportedOperationException();
        }
        public static final MethodHandle defineClassMethodHandle;
        static {
            try {
                defineClassMethodHandle = FFMUtil.UnsafeHolder.IMPL_LOOKUP.findVirtual(ClassLoader.class, "defineClass",
                        MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
    }

    public static Class<?> defineClass(ClassLoader classLoader, String name, byte[] bytecode, int offset, int length, ProtectionDomain protectionDomain) {
        try {
            return (Class<?>) DefineClassHolder.defineClassMethodHandle.bindTo(classLoader).invokeExact(name, bytecode, offset, length, protectionDomain);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public static Class<?> defineClass(ClassLoader classLoader, String name, byte[] bytecode) {
        return defineClass(classLoader, name, bytecode, 0, bytecode.length, null);
    }

    public static MemoryLayout toMemoryLayout(ForeignType type) {
        Objects.requireNonNull(type);
        if (type == ScalarType.INT8 || type == ScalarType.CHAR) return ValueLayout.JAVA_BYTE;
        else if (type == ScalarType.INT16 || (type == ScalarType.SHORT && Foreign.shortSize() == 2)) 
            return ValueLayout.JAVA_SHORT;
        else if (type == ScalarType.INT32
                || (type == ScalarType.INT && Foreign.intSize() == 4)
                || (type == ScalarType.LONG && Foreign.longSize() == 4)
                || (type == ScalarType.SIZE && Foreign.addressSize() == 4)
                || (type == ScalarType.WCHAR && Foreign.wcharSize() == 4))
            return ValueLayout.JAVA_INT;
        else if (type == ScalarType.INT64
                || (type == ScalarType.SHORT && Foreign.shortSize() == 8)
                || (type == ScalarType.INT && Foreign.intSize() == 8)
                || (type == ScalarType.LONG && Foreign.longSize() == 8)
                || (type == ScalarType.SIZE && Foreign.addressSize() == 8)) 
            return ValueLayout.JAVA_LONG;
        else if (type == ScalarType.FLOAT) return ValueLayout.JAVA_FLOAT;
        else if (type == ScalarType.DOUBLE) return ValueLayout.JAVA_DOUBLE;
        else if (type == ScalarType.BOOLEAN) return ValueLayout.JAVA_BOOLEAN;
        else if (type == ScalarType.UTF16 || (type == ScalarType.WCHAR && Foreign.wcharSize() == 2))
            return ValueLayout.JAVA_CHAR;
        else if (type == ScalarType.ADDRESS) return ValueLayout.ADDRESS;
        else {
            long size = type.size();
            if (size < 0) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(size));
            return MemoryLayout.structLayout(MemoryLayout.sequenceLayout(size, ValueLayout.JAVA_BYTE));
        }
    }

}
