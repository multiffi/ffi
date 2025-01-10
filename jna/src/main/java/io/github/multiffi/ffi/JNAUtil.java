package io.github.multiffi.ffi;

import com.sun.jna.Function;
import com.sun.jna.JNAAccessor;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
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
import java.util.Objects;

@SuppressWarnings({"deprecation", "removal"})
final class JNAUtil {

    private JNAUtil() {
        throw new AssertionError("No io.github.multiffi.ffi.JNAUtil instances for you!");
    }

    private static boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(System.getProperty(propertyName, Boolean.valueOf(defaultValue).toString()));
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public static final boolean STDCALL_AVAILABLE = Platform.isWindows() && !Platform.isWindowsCE() && !Platform.is64Bit();
    public static final boolean ASM_AVAILABLE = !getBooleanProperty("multiffi.ffi.jna.noasm", Platform.isAndroid());
    public static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

    public static final class UnsafeHolder {
        private UnsafeHolder() {
            throw new UnsupportedOperationException();
        }
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

    public static String mapLibraryName(String libraryName) {
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
