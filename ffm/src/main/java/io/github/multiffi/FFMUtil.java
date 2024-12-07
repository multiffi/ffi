package io.github.multiffi;

import multiffi.Foreign;
import multiffi.ForeignType;
import multiffi.MemoryBlock;
import multiffi.ScalarType;
import multiffi.UnsatisfiedLinkException;
import sun.misc.Unsafe;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntFunction;

@SuppressWarnings({"restricted", "deprecated", "removal"})
final class FFMUtil {

    private FFMUtil() {
        throw new AssertionError("No multiffi.FFMUtil instances for you!");
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

    public static final class StdlibHolder {
        private StdlibHolder() {
            throw new UnsupportedOperationException();
        }
        public static final MethodHandle CALLOC;
        public static final MethodHandle MEMCHR;
        public static final MethodHandle MEMCMP;
        static {
            SymbolLookup stdlib = ABIHolder.LINKER.defaultLookup();
            MemorySegment address = stdlib.find("calloc")
                    .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `calloc`"));
            FunctionDescriptor signature = FunctionDescriptor.of(ABIHolder.SIZE_T, ABIHolder.SIZE_T, ABIHolder.SIZE_T);
            CALLOC = ABIHolder.LINKER.downcallHandle(address, signature);
            address = stdlib.find("memchr")
                    .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `memchr`"));
            signature = FunctionDescriptor.of(ABIHolder.SIZE_T, ABIHolder.SIZE_T, ValueLayout.JAVA_INT, ABIHolder.SIZE_T);
            MEMCHR = ABIHolder.LINKER.downcallHandle(address, signature);
            address = stdlib.find("memcmp")
                    .orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `memcmp`"));
            signature = FunctionDescriptor.of(ValueLayout.JAVA_INT, ABIHolder.SIZE_T, ABIHolder.SIZE_T, ABIHolder.SIZE_T);
            MEMCMP = ABIHolder.LINKER.downcallHandle(address, signature);
        }
    }

    public static final class CharsetHolder {
        private CharsetHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Charset UTF16_CHARSET = IS_BIG_ENDIAN ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;
        public static final Charset UTF32_CHARSET = IS_BIG_ENDIAN ? Charset.forName("UTF-32BE") : Charset.forName("UTF-32LE");
        public static final Charset WIDE_CHARSET = ABIHolder.WCHAR_T.byteSize() == 2L ? UTF16_CHARSET : UTF32_CHARSET;
        public static final Charset ANSI_CHARSET = Charset.forName(System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding")));
        public static final Charset CONSOLE_CHARSET =
                Charset.forName(System.getProperty("stdout.encoding", System.getProperty("sun.stdout.encoding",
                        System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding")))));
    }

    public static final class LibraryNameMapperHolder {
        private LibraryNameMapperHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Function<String, String> LIBRARY_NAME_MAPPER;
        static {
            if (IS_WINDOWS) LIBRARY_NAME_MAPPER = libraryName -> {
                if (libraryName.matches(".*\\.dll$")) return libraryName;
                else return System.mapLibraryName(libraryName);
            };
            else if (osNameStartsWithIgnoreCase("mac") || osNameStartsWithIgnoreCase("darwin"))
                LIBRARY_NAME_MAPPER = libraryName -> {
                if (libraryName.matches("lib.*\\.(dylib|jnilib)$")) return libraryName;
                else return "lib" + libraryName + ".dylib";
            };
            else if (osNameStartsWithIgnoreCase("os/400") || osNameStartsWithIgnoreCase("os400"))
                LIBRARY_NAME_MAPPER = libraryName -> {
                if (libraryName.matches("lib.*\\.so.*$")) return libraryName;
                else return "lib" + libraryName + ".so";
            };
            else LIBRARY_NAME_MAPPER = libraryName -> {
                if (libraryName.matches("lib.*\\.so.*$")) return libraryName;
                else return System.mapLibraryName(libraryName);
            };
        }
    }

    public static final class SymbolLookupHolder {
        private SymbolLookupHolder() {
            throw new UnsupportedOperationException();
        }
        public static final AtomicReference<SymbolLookup> GLOBAL_LOOKUP_REFERENCE = new AtomicReference<>();
        static {
            // ClassLoader & C runtime default lookup
            SymbolLookup lookup = SymbolLookup.loaderLookup().or(ABIHolder.LINKER.defaultLookup());
            boolean isLinux = osNameStartsWithIgnoreCase("linux");
            String libraryName;
            if (IS_WINDOWS) libraryName = OS_NAME.startsWith("Windows CE") ? "coredll" : "msvcrt";
            else if (isLinux) libraryName = "libc.so.6";
            else libraryName = "c";
            // libc lookup
            lookup = lookup.or(SymbolLookup.libraryLookup(LibraryNameMapperHolder.LIBRARY_NAME_MAPPER.apply(libraryName), Arena.global()));
            // libm lookup
            if (!IS_WINDOWS) lookup = lookup.or(SymbolLookup.libraryLookup(LibraryNameMapperHolder.LIBRARY_NAME_MAPPER.apply(isLinux ? "libm.so.6" : "m"), Arena.global()));
            GLOBAL_LOOKUP_REFERENCE.set(lookup);
        }
    }

    public static final class ErrnoHolder {
        private ErrnoHolder() {
            throw new UnsupportedOperationException();
        }
        private static final StructLayout CAPTURE_STATE_LAYOUT = Linker.Option.captureStateLayout();
        public static final String ERRNO_NAME = IS_WINDOWS ? "GetLastError" : "errno";
        public static final VarHandle ERRNO_HANDLE =
                CAPTURE_STATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(ERRNO_NAME));
        public static final ThreadLocal<MemorySegment> ERRNO_THREAD_LOCAL =
                ThreadLocal.withInitial(() -> Arena.ofConfined().allocate(CAPTURE_STATE_LAYOUT));
    }

    public static final class ErrorStringMapperHolder {
        private ErrorStringMapperHolder() {
            throw new UnsupportedOperationException();
        }
        public static final IntFunction<String> ERROR_STRING_MAPPER;
        static {
            if (IS_WINDOWS) {
                SymbolLookup Kernel32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());
                MethodHandle FormatMessageW = ABIHolder.LINKER.downcallHandle(Kernel32
                                .find("FormatMessageW").orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `FormatMessageW`")),
                        FunctionDescriptor.of(ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                MethodHandle LocalFree = ABIHolder.LINKER.downcallHandle(Kernel32
                                .find("LocalFree").orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `LocalFree`")),
                        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                ThreadLocal<MemorySegment> lpBufferThreadLocal = ThreadLocal.withInitial(() -> Arena.global().allocate(UnsafeHolder.UNSAFE.addressSize()));
                ERROR_STRING_MAPPER = errno -> {
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
                        else return lpBuffer.getString(0, CharsetHolder.WIDE_CHARSET);
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
                MethodHandle strerror = ABIHolder.LINKER.downcallHandle(
                        ABIHolder.LINKER.defaultLookup().find("strerror").orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `strerror`")),
                        FunctionDescriptor.of(ValueLayout.ADDRESS, ABIHolder.LINKER.canonicalLayouts().get("int"))
                );
                ERROR_STRING_MAPPER = errno -> {
                    try {
                        return ((MemorySegment) strerror.invokeExact(errno)).reinterpret(
                                        UnsafeHolder.UNSAFE.addressSize() == 8 ? Long.MAX_VALUE : Integer.MAX_VALUE)
                                .getString(0, CharsetHolder.ANSI_CHARSET);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
            }
        }
    }

    public static final class MarshallerHolder {
        private MarshallerHolder() {
            throw new UnsupportedOperationException();
        }
        private static MemorySegment blockToSegment(MemoryBlock block) {
            if (block == null || block.isNil()) throw new NullPointerException();
            else if (block.isDirect()) return MemorySegment.ofAddress(block.address())
                    .reinterpret(block.isBounded() ? block.size() : Foreign.addressSize() == 8 ? Long.MAX_VALUE : Integer.MAX_VALUE);
            else {
                Object array = block.array();
                return switch (array) {
                    case byte[] byteArray -> MemorySegment.ofArray(byteArray);
                    case short[] shortArray -> MemorySegment.ofArray(shortArray);
                    case int[] intArray -> MemorySegment.ofArray(intArray);
                    case long[] longArray -> MemorySegment.ofArray(longArray);
                    case float[] floatArray -> MemorySegment.ofArray(floatArray);
                    case double[] doubleArray -> MemorySegment.ofArray(doubleArray);
                    case char[] charArray -> MemorySegment.ofArray(charArray);
                    case null, default -> throw new IllegalStateException("Unexpected exception");
                };
            }
        }
        private static MemoryBlock segmentToBlock(MemorySegment segment) {
            if (segment == null || MemorySegment.NULL.equals(segment)) throw new NullPointerException();
            else if (segment.isNative()) return MemoryBlock.wrap(segment.address(), segment.byteSize());
            else {
                Object array = segment.heapBase().orElse(null);
                return switch (array) {
                    case byte[] byteArray -> MemoryBlock.wrap(byteArray);
                    case short[] shortArray -> MemoryBlock.wrap(shortArray);
                    case int[] intArray -> MemoryBlock.wrap(intArray);
                    case long[] longArray -> MemoryBlock.wrap(longArray);
                    case float[] floatArray -> MemoryBlock.wrap(floatArray);
                    case double[] doubleArray -> MemoryBlock.wrap(doubleArray);
                    case char[] charArray -> MemoryBlock.wrap(charArray);
                    case null, default -> throw new IllegalStateException("Unexpected exception");
                };
            }
        }
        public static final MethodHandle BLOCK_TO_SEGMENT;
        public static final MethodHandle SEGMENT_TO_BLOCK;
        static {
            try {
                BLOCK_TO_SEGMENT = MethodHandles.lookup().findStatic(MarshallerHolder.class, "blockToSegment",
                        MethodType.methodType(MemorySegment.class, MemoryBlock.class));
                SEGMENT_TO_BLOCK = MethodHandles.lookup().findStatic(MarshallerHolder.class, "segmentToBlock",
                        MethodType.methodType(MemoryBlock.class, MemorySegment.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
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
