package io.github.multiffi.ffi;

import com.kenai.jffi.Array;
import com.kenai.jffi.MemoryIO;
import com.kenai.jffi.PageManager;
import com.kenai.jffi.Type;
import jnr.ffi.Platform;
import jnr.ffi.Runtime;
import multiffi.ffi.Foreign;
import multiffi.ffi.ForeignType;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"deprecation", "removal"})
public final class JNRUtil {

    private JNRUtil() {
        throw new AssertionError("No io.github.multiffi.ffi.JNAUtil instances for you!");
    }

    public static final Platform PLATFORM = Platform.getNativePlatform();
    public static final boolean STDCALL_AVAILABLE = PLATFORM.getOS() == Platform.OS.WINDOWS
            && PLATFORM.is32Bit() && !PLATFORM.getOSName().startsWith("Windows CE");
    public static final boolean ASM_AVAILABLE = PLATFORM.getOS() != Platform.OS.LINUX || !"dalvik".equalsIgnoreCase(System.getProperty("java.vm.name"));
    public static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
    public static final int WCHAR_SIZE = PLATFORM.getOS() == Platform.OS.WINDOWS ? 2 : 4;

    public static boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(System.getProperty(propertyName, Boolean.valueOf(defaultValue).toString()));
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public static final class UnsafeHolder {
        private UnsafeHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Runtime RUNTIME = Runtime.getSystemRuntime();
        public static final MemoryIO MEMORY_IO = MemoryIO.getInstance();
        public static final PageManager PAGE_MANAGER = PageManager.getInstance();
        public static final Unsafe UNSAFE;
        public static final Object IMPL_LOOKUP;
        public static final Method unreflectMethod;
        public static final Method unreflectConstructorMethod;
        public static final Method bindToMethod;
        public static final Method invokeWithArgumentsMethod;
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
    }

    public static Object invoke(Object object, Method method, Object... args) throws Throwable {
        if (UnsafeHolder.IMPL_LOOKUP == null) {
            method.setAccessible(true);
            try {
                return method.invoke(Modifier.isStatic(method.getModifiers()) ? null : Objects.requireNonNull(object), args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
        else {
            try {
                Object methodHandle = UnsafeHolder.unreflectMethod.invoke(UnsafeHolder.IMPL_LOOKUP, method);
                if (!Modifier.isStatic(method.getModifiers()))
                    methodHandle = UnsafeHolder.bindToMethod.invoke(methodHandle, Objects.requireNonNull(object));
                return UnsafeHolder.invokeWithArgumentsMethod.invoke(methodHandle, (Object) args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Constructor<T> constructor, Object... args) throws Throwable {
        if (UnsafeHolder.IMPL_LOOKUP == null) {
            constructor.setAccessible(true);
            try {
                return constructor.newInstance(args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
        else {
            try {
                Object methodHandle = UnsafeHolder.unreflectConstructorMethod.invoke(UnsafeHolder.IMPL_LOOKUP, constructor);
                return (T) UnsafeHolder.invokeWithArgumentsMethod.invoke(methodHandle, (Object) args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }

    public static final class InvokerHolder {
        private InvokerHolder() {
            throw new UnsupportedOperationException();
        }
        public static final List<JNRInvoker> FAST_INVOKERS = Collections.unmodifiableList(Arrays.asList(
                JNRInvoker.FastInt.I0, JNRInvoker.FastInt.I1, JNRInvoker.FastInt.I2, JNRInvoker.FastInt.I3,
                JNRInvoker.FastInt.I4, JNRInvoker.FastInt.I5, JNRInvoker.FastInt.I6,
                JNRInvoker.FastLong.L0, JNRInvoker.FastLong.L1, JNRInvoker.FastLong.L2, JNRInvoker.FastLong.L3,
                JNRInvoker.FastLong.L4, JNRInvoker.FastLong.L5, JNRInvoker.FastLong.L6,
                JNRInvoker.FastNumeric.N0, JNRInvoker.FastNumeric.N1, JNRInvoker.FastNumeric.N2, JNRInvoker.FastNumeric.N3,
                JNRInvoker.FastNumeric.N4, JNRInvoker.FastNumeric.N5, JNRInvoker.FastNumeric.N6
        ));
    }

    public static Type toFFIType(ForeignType foreignType) {
        if (foreignType == null) return Type.VOID;
        else if (foreignType == ScalarType.BOOLEAN) return Type.UINT8;
        else if (foreignType == ScalarType.INT8) return Type.SINT8;
        else if (foreignType == ScalarType.UTF16 || (foreignType == ScalarType.WCHAR && Foreign.wcharSize() == 2L)) return Type.UINT16;
        else if (foreignType == ScalarType.INT16) return Type.SINT16;
        else if (foreignType == ScalarType.INT32
                || (foreignType == ScalarType.SIZE && Foreign.addressSize() == 4L)
                || (foreignType == ScalarType.WCHAR && Foreign.wcharSize() == 4L)) return Type.SINT32;
        else if (foreignType == ScalarType.INT64 || (foreignType == ScalarType.SIZE && Foreign.addressSize() == 8L)) return Type.SINT64;
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

    public static void checkType(ForeignType type, Class<?> clazz) {
        Class<?> expected;
        if (type == ScalarType.BOOLEAN) expected = boolean.class;
        else if (type == ScalarType.UTF16) expected = char.class;
        else if (type == ScalarType.INT8 || type == ScalarType.CHAR) expected = byte.class;
        else if (type == ScalarType.INT16) expected = short.class;
        else if (type == ScalarType.INT32 || type == ScalarType.WCHAR) expected = int.class;
        else if (type == ScalarType.INT64 || type == ScalarType.SHORT || type == ScalarType.INT
                || type == ScalarType.LONG || type == ScalarType.SIZE || type == ScalarType.ADDRESS)
            expected = long.class;
        else if (type == ScalarType.FLOAT) expected = float.class;
        else if (type == ScalarType.DOUBLE) expected = double.class;
        else expected = MemoryHandle.class;
        if (clazz != expected) throw new IllegalArgumentException("Illegal mapping type; expected " + expected);
    }

}
