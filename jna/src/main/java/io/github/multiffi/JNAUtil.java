package io.github.multiffi;

import com.sun.jna.Function;
import com.sun.jna.JNAAccessor;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.Objects;

final class JNAUtil {

    private JNAUtil() {
        throw new AssertionError("No io.github.multiffi.JNAUtil instances for you!");
    }

    public static final boolean STDCALL_AVAILABLE = Platform.isWindows() && !Platform.isWindowsCE() && !Platform.is64Bit();
    public static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

    public static final class UnsafeHolder {
        private UnsafeHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Unsafe UNSAFE;
        public static final Object IMPL_LOOKUP;
        public static final Method unreflectMethod;
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
            try {
                Class<?> lookupClass = Class.forName("java.lang.invoke.MethodHandles$Lookup");
                Field field = lookupClass.getDeclaredField("IMPL_LOOKUP");
                IMPL_LOOKUP = UNSAFE.getObject(lookupClass, UNSAFE.staticFieldOffset(field));
                unreflectMethod = lookupClass.getDeclaredMethod("unreflect", Method.class);
                Class<?> methodHandleClass = Class.forName("java.lang.invoke.MethodHandle");
                bindToMethod = methodHandleClass.getDeclaredMethod("bindTo", Object.class);
                invokeWithArgumentsMethod = methodHandleClass.getDeclaredMethod("invokeWithArguments", Object[].class);
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to get the trusted java.lang.invoke.MethodHandles.Lookup instance");
            }
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

    public static final class LastErrnoHolder {
        private LastErrnoHolder() {
            throw new UnsupportedOperationException();
        }
        public static final ThreadLocal<Integer> ERRNO_THREAD_LOCAL = new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return 0;
            }
        };
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

    public static Object invoke(Object returnType, Function function, long address, int callFlags, Object... args) {
        if (returnType == null || returnType == void.class || returnType == Void.class) {
            JNAAccessor.invokeVoid(function, address, callFlags, args);
            return null;
        }
        else if (returnType == boolean.class || returnType == Boolean.class)
            return JNAAccessor.invokeInt(function, address, callFlags, args) != 0;
        else if (returnType == byte.class || returnType == Byte.class)
            return (byte) JNAAccessor.invokeInt(function, address, callFlags, args);
        else if (returnType == short.class || returnType == Short.class)
            return (short) JNAAccessor.invokeInt(function, address, callFlags, args);
        else if (returnType == int.class || returnType == Integer.class)
            return JNAAccessor.invokeInt(function, address, callFlags, args);
        else if (returnType == long.class || returnType == Long.class)
            return JNAAccessor.invokeLong(function, address, callFlags, args);
        else if (returnType == float.class || returnType == Float.class)
            return JNAAccessor.invokeFloat(function, address, callFlags, args);
        else if (returnType == double.class || returnType == Double.class)
            return JNAAccessor.invokeDouble(function, address, callFlags, args);
        else if (returnType == char.class || returnType == Character.class)
            return (char) JNAAccessor.invokeInt(function, address, callFlags, args);
        else if (returnType == Pointer.class)
            return JNAAccessor.invokePointer(function, address, callFlags, args);
        else return JNAAccessor.invokeStructure((Structure) returnType, function, address, callFlags, args);
    }

}
