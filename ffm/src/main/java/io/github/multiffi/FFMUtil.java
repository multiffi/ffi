package io.github.multiffi;

import multiffi.ForeignType;
import multiffi.ScalarType;
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

@SuppressWarnings({"restricted", "deprecated", "removal"})
final class FFMUtil {

    private FFMUtil() {
        throw new AssertionError("No io.github.multiffi.FFMUtil instances for you!");
    }

    public static final String OS_NAME = System.getProperty("os.name");
    public static final boolean IS_WINDOWS = OS_NAME.startsWith("Windows");
    public static boolean osNameStartsWithIgnoreCase(String prefix) {
        if (OS_NAME == null) return false;
        else return OS_NAME.startsWith(prefix)
                || OS_NAME.toUpperCase(Locale.ENGLISH).startsWith(prefix.toUpperCase(Locale.ENGLISH))
                || OS_NAME.toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH));
    }
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
        public static final MethodHandle DEFINE_CLASS;
        static {
            try {
                DEFINE_CLASS = FFMUtil.UnsafeHolder.IMPL_LOOKUP.findVirtual(ClassLoader.class, "defineClass",
                        MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
    }

    public static Class<?> defineClass(ClassLoader classLoader, String name, byte[] bytecode, int offset, int length, ProtectionDomain protectionDomain) {
        if (classLoader == null) classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return (Class<?>) DefineClassHolder.DEFINE_CLASS.bindTo(classLoader).invokeExact(name, bytecode, offset, length, protectionDomain);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public static Class<?> defineClass(ClassLoader classLoader, String name, byte[] bytecode) {
        return defineClass(classLoader, name, bytecode, 0, bytecode.length, null);
    }

    public static long checkAddress(long address) {
        if (UnsafeHolder.UNSAFE.addressSize() == 4) {
            // Accept both zero and sign extended pointers. A valid
            // pointer will, after the +1 below, either have produced
            // the value 0x0 or 0x1. Masking off the low bit allows
            // for testing against 0.
            if ((((address >> 32) + 1) & ~1) != 0) throw new ArithmeticException("integer overflow");
        }
        return address;
    }

    public static long unsignedAddExact(long x, long y) {
        long sum = x + y;
        if (Long.compareUnsigned(x, sum) > 0) throw new ArithmeticException("long overflow");
        return sum;
    }

    public static MemoryLayout toMemoryLayout(ForeignType type) {
        Objects.requireNonNull(type);
        if (type == ScalarType.INT8) return ValueLayout.JAVA_BYTE;
        else if (type == ScalarType.INT16) return ValueLayout.JAVA_SHORT;
        else if (type == ScalarType.INT32) return ValueLayout.JAVA_INT;
        else if (type == ScalarType.INT64) return ValueLayout.JAVA_LONG;
        else if (type == ScalarType.FLOAT) return ValueLayout.JAVA_FLOAT;
        else if (type == ScalarType.DOUBLE) return ValueLayout.JAVA_DOUBLE;
        else if (type == ScalarType.BOOLEAN) return ValueLayout.JAVA_BOOLEAN;
        else if (type == ScalarType.UTF16) return ValueLayout.JAVA_CHAR;
        else {
            long size = type.size();
            if (size < 0) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(size));
            return MemoryLayout.structLayout(MemoryLayout.sequenceLayout(size, ValueLayout.JAVA_BYTE));
        }
    }

}
