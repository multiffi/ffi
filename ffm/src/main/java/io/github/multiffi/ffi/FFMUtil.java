package io.github.multiffi.ffi;

import multiffi.ffi.Foreign;
import multiffi.ffi.ForeignType;
import multiffi.ffi.ScalarType;
import sun.misc.Unsafe;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;

@SuppressWarnings({"deprecation", "removal"})
public final class FFMUtil {

    private FFMUtil() {
        throw new AssertionError("No io.github.multiffi.ffi.FFMUtil instances for you!");
    }

    public static final boolean PROXY_INTRINSICS = Util.getBooleanProperty("multiffi.foreign.proxyIntrinsics", true);

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

    public static final Linker.Option[] EMPTY_LINKER_OPTION_ARRAY = new Linker.Option[0];
    public static final MemoryLayout[] EMPTY_MEMORY_LAYOUT_ARRAY = new MemoryLayout[0];

    public static final String OS_NAME = System.getProperty("os.name");
    private static boolean startsWithIgnoreCase(String string, String prefix) {
        if (string == null) return false;
        else return string.startsWith(prefix)
                || string.toUpperCase(Locale.ENGLISH).startsWith(prefix.toUpperCase(Locale.ENGLISH))
                || string.toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH));
    }
    public static final boolean IS_WINDOWS = startsWithIgnoreCase(OS_NAME, "windows");
    public static final boolean IS_WINDOWS_CE = startsWithIgnoreCase(OS_NAME, "windows ce");
    public static final boolean IS_MAC = startsWithIgnoreCase(OS_NAME, "mac") || startsWithIgnoreCase(OS_NAME, "darwin");
    public static final boolean IS_AIX = startsWithIgnoreCase(OS_NAME, "aix");
    public static final boolean IS_IBMI = startsWithIgnoreCase(OS_NAME, "os/400") || startsWithIgnoreCase(OS_NAME, "os400");
    public static final boolean IS_LINUX = startsWithIgnoreCase(OS_NAME, "linux");

    public static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();
    public static final boolean IS_BIG_ENDIAN = NATIVE_ORDER.equals(ByteOrder.BIG_ENDIAN);

    public static final Linker LINKER = Linker.nativeLinker();
    public static final SymbolLookup DEFAULT_LOOKUP = LINKER.defaultLookup();

    public static final ValueLayout SIZE_T = (ValueLayout) LINKER.canonicalLayouts().get("size_t");
    public static final ValueLayout SHORT = (ValueLayout) LINKER.canonicalLayouts().get("short");
    public static final ValueLayout INT = (ValueLayout) LINKER.canonicalLayouts().get("int");
    public static final ValueLayout LONG = (ValueLayout) LINKER.canonicalLayouts().get("long");
    public static final MemoryLayout WCHAR_T = LINKER.canonicalLayouts().get("wchar_t");

    public static final long ADDRESS_SIZE = SIZE_T.byteSize();
    public static final long SHORT_SIZE = SHORT.byteSize();
    public static final long INT_SIZE = INT.byteSize();
    public static final long LONG_SIZE = LONG.byteSize();
    public static final long WCHAR_SIZE = WCHAR_T.byteSize();
    public static final long PAGE_SIZE = UNSAFE.pageSize() & 0xFFFFFFFFL;
    public static final long ALIGNMENT_SIZE = IS_WINDOWS ? ADDRESS_SIZE * 2 : ADDRESS_SIZE;

    public static final Charset UTF16_CHARSET = IS_BIG_ENDIAN ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;
    public static final Charset UTF32_CHARSET = IS_BIG_ENDIAN ? Charset.forName("UTF-32BE") : Charset.forName("UTF-32LE");
    public static final Charset WIDE_CHARSET = WCHAR_SIZE == 2L ? UTF16_CHARSET : UTF32_CHARSET;
    public static final Charset ANSI_CHARSET = Charset.forName(System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding", Charset.defaultCharset().name())));

    private static final MethodHandle defineClassMethodHandle;
    static {
        try {
            defineClassMethodHandle = IMPL_LOOKUP.findVirtual(ClassLoader.class, "defineClass",
                    MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }

    public static Class<?> defineClass(ClassLoader classLoader, String name, byte[] bytecode, int offset, int length, ProtectionDomain protectionDomain) {
        try {
            return (Class<?>) defineClassMethodHandle.bindTo(classLoader).invokeExact(name, bytecode, offset, length, protectionDomain);
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

    public static long unsignedMultiplyExact(long x, long y) {
        long hi = Math.unsignedMultiplyHigh(x, y);
        if (hi != 0) throw new ArithmeticException("long overflow");
        return x * y;
    }

    private static final Function<String, String> libraryNameMapperFunction;
    static {
        if (IS_WINDOWS) libraryNameMapperFunction = libraryName -> {
            if (libraryName.matches(".*\\.(drv|dll|ocx)$")) return libraryName;
            else return System.mapLibraryName(libraryName);
        };
        else if (IS_MAC)
            libraryNameMapperFunction = libraryName -> {
                if (libraryName.matches("lib.*\\.(dylib|jnilib)$")) return libraryName;
                else return "lib" + libraryName + ".dylib";
            };
        else if (IS_LINUX)
            libraryNameMapperFunction = libraryName -> {
                if (libraryName.matches("lib.*\\.so((?:\\.[0-9]+)*)$")) return libraryName;
                else return System.mapLibraryName(libraryName);
            };
        else if (IS_AIX)
            libraryNameMapperFunction = libraryName -> {
                if (libraryName.matches("lib.*\\.(so|a\\(shr.o\\)|a\\(shr_64.o\\)|a|so.[.0-9]+)$")) return libraryName;
                else return "lib" + libraryName + ".a";
            };
        else if (IS_IBMI) {
            if (ADDRESS_SIZE == 4L) {
                libraryNameMapperFunction = libraryName -> {
                    if (libraryName.matches("lib.*\\.(so|a\\(shr.o\\)|a\\(shr_64.o\\)|a|so.[.0-9]+)$")) return libraryName;
                    else return "lib" + libraryName + ".a(shr.o)";
                };
            }
            else libraryNameMapperFunction = libraryName -> {
                if (libraryName.matches("lib.*\\.(so|a\\(shr.o\\)|a\\(shr_64.o\\)|a|so.[.0-9]+)$")) return libraryName;
                else return "lib" + libraryName + ".a(shr_64.o)";
            };
        }
        else libraryNameMapperFunction = libraryName -> {
            if (libraryName.matches("lib.*\\.so.*$")) return libraryName;
            else return System.mapLibraryName(libraryName);
        };
    }

    public static String mapLibraryName(String libraryName) {
        if (libraryName == null) return null;
        else if (new File(libraryName).isAbsolute()) return libraryName;
        else return libraryNameMapperFunction.apply(libraryName);
    }

    private static final IntFunction<String> errorStringMapperFunction;
    static {
        if (IS_WINDOWS) {
            SymbolLookup Kernel32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());
            MethodHandle FormatMessageW = LINKER.downcallHandle(Kernel32
                            .find("FormatMessageW").orElseThrow(() -> new UnsatisfiedLinkError("Failed to get symbol: `FormatMessageW`")),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            MethodHandle LocalFree = LINKER.downcallHandle(Kernel32
                            .find("LocalFree").orElseThrow(() -> new UnsatisfiedLinkError("Failed to get symbol: `LocalFree`")),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            ThreadLocal<MemorySegment> lpBufferThreadLocal = ThreadLocal.withInitial(() -> Arena.global().allocate(ADDRESS_SIZE));
            errorStringMapperFunction = errno -> {
                MemorySegment lpBuffer = lpBufferThreadLocal.get();
                lpBuffer.set(ValueLayout.ADDRESS, 0, MemorySegment.NULL);
                try {
                    if ((int) FormatMessageW.invokeExact(
                            0x00001000 /* FORMAT_MESSAGE_FROM_SYSTEM */ | 0x00000100 /* FORMAT_MESSAGE_ALLOCATE_BUFFER */,
                            MemorySegment.NULL,
                            errno,
                            0,
                            lpBuffer,
                            0,
                            MemorySegment.NULL) == 0) return "FormatMessage failed with 0x" + Integer.toHexString(errno);
                    else return lpBuffer.get(ValueLayout.ADDRESS, 0).reinterpret((Integer.MAX_VALUE - 8) & 0xFFFFFFFFL).getString(0, WIDE_CHARSET);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                } finally {
                    MemorySegment hMem = lpBuffer.get(ValueLayout.ADDRESS, 0);
                    if (!MemorySegment.NULL.equals(hMem)) {
                        try {
                            MemorySegment hLocal = (MemorySegment) LocalFree.invokeExact(hMem);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            };
        }
        else {
            MethodHandle strerror = LINKER.downcallHandle(
                    DEFAULT_LOOKUP.find("strerror").orElseThrow(() -> new UnsatisfiedLinkError("Failed to get symbol: `strerror`")),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, LINKER.canonicalLayouts().get("int"))
            );
            errorStringMapperFunction = errno -> {
                try {
                    MemorySegment errorString = ((MemorySegment) strerror.invokeExact(errno)).reinterpret(
                            ADDRESS_SIZE == 8L ? Long.MAX_VALUE : Integer.MAX_VALUE);
                    if (MemorySegment.NULL.equals(errorString)) return "strerror failed with 0x" + Integer.toHexString(errno);
                    else return errorString.getString(0, ANSI_CHARSET);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            };
        }
    }

    public static String getErrorString(int errno) {
        return errorStringMapperFunction.apply(errno);
    }

}
