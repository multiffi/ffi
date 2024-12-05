package io.github.multiffi;

import multiffi.CallOption;
import multiffi.ForeignType;
import multiffi.FunctionPointer;
import multiffi.MemoryBlock;
import multiffi.ScalarType;
import multiffi.StandardCallOption;
import multiffi.UnsatisfiedLinkException;
import multiffi.spi.ForeignProvider;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.lang.ref.Cleaner;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntFunction;

@SuppressWarnings({"deprecated", "removal"})
public class FFMForeignProvider extends ForeignProvider {

    private static final Unsafe UNSAFE;
    private static final MethodHandles.Lookup IMPL_LOOKUP;
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

    private static final Linker LINKER = Linker.nativeLinker();
    private static final long SHORT_SIZE = LINKER.canonicalLayouts().get("short").byteSize();
    private static final long INT_SIZE = LINKER.canonicalLayouts().get("int").byteSize();
    private static final long LONG_SIZE = LINKER.canonicalLayouts().get("long").byteSize();
    private static final long WCHAR_SIZE = LINKER.canonicalLayouts().get("wchar_t").byteSize();
    private static final Runtime RUNTIME = Runtime.getRuntime();

    private static final Charset UTF16_CHARSET = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
            ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;
    private static final Charset UTF32_CHARSET = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
            ? Charset.forName("UTF-32BE") : Charset.forName("UTF-32LE");
    private static final Charset WIDE_CHARSET = WCHAR_SIZE == 2 ? UTF16_CHARSET : UTF32_CHARSET;
    private static final Charset ANSI_CHARSET = Charset.forName(System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding")));
    private static final Charset CONSOLE_CHARSET =
            Charset.forName(System.getProperty("stdout.encoding", System.getProperty("sun.stdout.encoding",
                    System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding")))));

    private static final Field ADDRESS;
    private static final Method BASE;
    static {
        try {
            ADDRESS = Buffer.class.getDeclaredField("address");
            BASE = Buffer.class.getDeclaredMethod("base");
        }
        catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new IllegalStateException("Unexpected exception");
        }
    }

    @Override
    public long addressSize() {
        return UNSAFE.addressSize();
    }

    @Override
    public long shortSize() {
        return SHORT_SIZE;
    }

    @Override
    public long intSize() {
        return INT_SIZE;
    }

    @Override
    public long longSize() {
        return LONG_SIZE;
    }

    @Override
    public long wideCharSize() {
        return WCHAR_SIZE;
    }

    @Override
    public long pageSize() {
        return UNSAFE.pageSize();
    }

    @Override
    public Charset ansiCharset() {
        return ANSI_CHARSET;
    }

    @Override
    public Charset wideCharset() {
        return WIDE_CHARSET;
    }

    @Override
    public Charset utf16Charset() {
        return UTF16_CHARSET;
    }

    @Override
    public Charset utf32Charset() {
        return UTF32_CHARSET;
    }

    @Override
    public Charset consoleCharset() {
        return CONSOLE_CHARSET;
    }

    @Override
    public ByteBuffer wrapDirectBuffer(long address, int capacity) {
        return MemorySegment.ofAddress(address).reinterpret(capacity).asByteBuffer();
    }

    @Override
    public ByteBuffer wrapDirectBuffer(long address) {
        return MemorySegment.ofAddress(address).reinterpret(Integer.MAX_VALUE - 8).asByteBuffer();
    }

    @Override
    public long getDirectBufferAddress(Buffer buffer) {
        if (buffer.isDirect()) return UNSAFE.getLong(buffer, UNSAFE.objectFieldOffset(ADDRESS));
        else return 0;
    }

    @Override
    public boolean isByteBuffer(Buffer buffer) {
        return buffer instanceof ByteBuffer || (buffer != null && buffer.getClass().getSimpleName().startsWith("ByteBufferAs"));
    }

    @Override
    public ByteBuffer getByteBuffer(Buffer buffer) {
        if (buffer instanceof ByteBuffer) return (ByteBuffer) buffer;
        else if (buffer != null && buffer.getClass().getSimpleName().startsWith("ByteBufferAs")) {
            try {
                return (ByteBuffer) UNSAFE.getObject(buffer, UNSAFE.objectFieldOffset(buffer.getClass().getDeclaredField("bb")));
            } catch (NoSuchFieldException | ClassCastException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
        else return null;
    }

    @Override
    public void cleanBuffer(Buffer buffer) {
        ByteBuffer byteBuffer = getByteBuffer(buffer);
        if (byteBuffer != null) UNSAFE.invokeCleaner(byteBuffer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Buffer> T getBufferAttachment(T buffer) {
        if (buffer == null || !buffer.isDirect()) return null;
        else {
            try {
                return (T) IMPL_LOOKUP.unreflect(buffer.getClass().getMethod("attachment")).bindTo(buffer).invokeWithArguments();
            } catch (NoSuchMethodException | ClassCastException | IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public <T extends Buffer> T sliceBuffer(T buffer, int index, int length) {
        buffer.slice(index, length);
        return buffer;
    }

    @Override
    public <T extends Buffer> T sliceBuffer(T buffer, int index) {
        buffer.slice(index, buffer.capacity() - index);
        return buffer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Buffer> T duplicateBuffer(T buffer) {
        return (T) buffer.duplicate();
    }

    @Override
    public Object getHeapBufferArray(Buffer buffer) {
        if (buffer.isDirect()) return null;
        else if (buffer.isReadOnly()) {
            try {
                return IMPL_LOOKUP.unreflect(BASE).bindTo(buffer).invokeWithArguments();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unexpected exception");
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
        else return buffer.array();
    }

    @Override
    public int getHeapBufferArrayOffset(Buffer buffer) {
        if (buffer.isDirect()) return 0;
        else if (buffer.isReadOnly()) {
            try {
                return UNSAFE.getInt(buffer, UNSAFE.objectFieldOffset(buffer.getClass().getDeclaredField("offset")));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
        else return buffer.arrayOffset() * UNSAFE.arrayIndexScale(buffer.array().getClass());
    }

    @Override
    public void sneakyThrows(Throwable throwable) {
        if (throwable != null) UNSAFE.throwException(throwable);
    }

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
        return Instant.now().getEpochSecond();
    }

    @Override
    public int currentTimeNanos() {
        return Instant.now().getNano();
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
        return System.getenv().getOrDefault(key, defaultValue);
    }

    @Override
    public String getStackTraceString(Throwable throwable) {
        if (throwable == null) return null;
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static final boolean BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
    @Override
    public boolean isBigEndian() {
        return BIG_ENDIAN;
    }
    @Override
    public boolean isLittleEndian() {
        return !BIG_ENDIAN;
    }
    @Override
    public ByteOrder endianness() {
        return ByteOrder.nativeOrder();
    }

    static final boolean IS_WINDOWS = osNameStartsWithIgnoreCase("windows");

    private static final String OS_NAME = System.getProperty("os.name");
    private static boolean osNameStartsWithIgnoreCase(String prefix) {
        if (OS_NAME == null) return false;
        else return OS_NAME.startsWith(prefix)
                || OS_NAME.toUpperCase(Locale.ENGLISH).startsWith(prefix.toUpperCase(Locale.ENGLISH))
                || OS_NAME.toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH));
    }
    private static final Function<String, String> LIBRARY_NAME_MAPPER;
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

    private static final AtomicReference<SymbolLookup> GLOBAL_LOOKUP_REFERENCE = new AtomicReference<>();
    static {
        // ClassLoader & C runtime default lookup
        SymbolLookup lookup = SymbolLookup.loaderLookup().or(LINKER.defaultLookup());
        boolean isLinux = osNameStartsWithIgnoreCase("linux");
        String libraryName;
        if (IS_WINDOWS) libraryName = osNameStartsWithIgnoreCase("windows ce") ? "coredll" : "msvcrt";
        else if (isLinux) libraryName = "libc.so.6";
        else libraryName = "c";
        // libc lookup
        lookup = lookup.or(SymbolLookup.libraryLookup(LIBRARY_NAME_MAPPER.apply(libraryName), Arena.global()));
        // libm lookup
        if (!IS_WINDOWS) lookup = lookup.or(SymbolLookup.libraryLookup(LIBRARY_NAME_MAPPER.apply(isLinux ? "libm.so.6" : "m"), Arena.global()));
        GLOBAL_LOOKUP_REFERENCE.set(lookup);
    }

    @Override
    public void loadLibrary(String libraryName) throws IOException {
        Objects.requireNonNull(libraryName);
        try {
            GLOBAL_LOOKUP_REFERENCE.getAndUpdate(lookup -> lookup.or(SymbolLookup.libraryLookup(libraryName, Arena.global())));
        }
        catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void loadLibrary(File libraryFile) throws IOException {
        Objects.requireNonNull(libraryFile);
        try {
            GLOBAL_LOOKUP_REFERENCE.getAndUpdate(lookup -> lookup.or(SymbolLookup.libraryLookup(libraryFile.getAbsoluteFile().toPath(), Arena.global())));
        }
        catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public long getSymbol(String symbolName) {
        Optional<MemorySegment> symbol = GLOBAL_LOOKUP_REFERENCE.get().find(symbolName);
        if (symbol.isPresent()) return symbol.get().address();
        else throw new UnsatisfiedLinkException(String.format("Failed to get symbol: `%s`", symbolName));
    }

    @Override
    public String mapLibraryName(String libraryName) {
        if (libraryName == null) return null;
        else return LIBRARY_NAME_MAPPER.apply(libraryName);
    }

    private static final StructLayout CAPTURE_STATE_LAYOUT = Linker.Option.captureStateLayout();
    private static final VarHandle ERRNO_HANDLE =
            CAPTURE_STATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(IS_WINDOWS ? "GetLastError" : "errno"));
    static final ThreadLocal<MemorySegment> ERRNO_THREAD_LOCAL = ThreadLocal.withInitial(() -> Arena.ofConfined().allocate(CAPTURE_STATE_LAYOUT));

    @Override
    public int getLastErrno() {
        return (int) ERRNO_HANDLE.get(ERRNO_THREAD_LOCAL.get(), 0L);
    }

    @Override
    public void setLastErrno(int errno) {
        ERRNO_HANDLE.set(ERRNO_THREAD_LOCAL.get(), errno);
    }

    private static final IntFunction<String> ERROR_STRING_MAPPER;
    static {
        if (IS_WINDOWS) {
            SymbolLookup Kernel32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());
            MethodHandle FormatMessageW = LINKER.downcallHandle(Kernel32
                            .find("FormatMessageW").orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `FormatMessageW`")),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            MethodHandle LocalFree = LINKER.downcallHandle(Kernel32
                            .find("LocalFree").orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `LocalFree`")),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            ThreadLocal<MemorySegment> lpBufferThreadLocal = ThreadLocal.withInitial(() -> Arena.global().allocate(UNSAFE.addressSize()));
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
                    else return lpBuffer.getString(0, WIDE_CHARSET);
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
                    LINKER.defaultLookup().find("strerror").orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `strerror`")),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, LINKER.canonicalLayouts().get("int"))
            );
            ERROR_STRING_MAPPER = errno -> {
                try {
                    return ((MemorySegment) strerror.invokeExact(errno)).reinterpret(
                            UNSAFE.addressSize() == 8 ? Long.MAX_VALUE : Integer.MAX_VALUE)
                            .getString(0, ANSI_CHARSET);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            };
        }
    }

    @Override
    public String getErrorString(int errno) {
        return ERROR_STRING_MAPPER.apply(errno);
    }

    @Override
    public FunctionPointer downcallHandle(long address, int firstVarArg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return new FFMFunctionPointer(address, firstVarArg, returnType, parameterTypes, options);
    }

    private static MemoryBlock segmentToBlock(MemorySegment segment) {
        if (MemorySegment.NULL.equals(segment)) return MemoryBlock.NULL;
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

    private static final MethodHandle SEGMENT_TO_BLOCK;
    static {
        try {
            SEGMENT_TO_BLOCK = MethodHandles.lookup().findStatic(FFMForeignProvider.class, "segmentToBlock",
                    MethodType.methodType(MemoryBlock.class, MemorySegment.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception");
        }
    }

    private static final Cleaner UPCALL_STUB_CLEANER = Cleaner.create(runnable -> {
        Thread thread = new Thread(runnable, "FFM UpcallStub Cleaner Thread");
        thread.setDaemon(true);
        return thread;
    });
    private static final Linker.Option[] EMPTY_OPTION_ARRAY = new Linker.Option[0];
    @Override
    public long upcallStub(Object object, Method method, int firstVarArg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        MethodHandle methodHandle;
        try {
            methodHandle = IMPL_LOOKUP.unreflect(method);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception");
        }
        MethodType methodType = methodHandle.type();
        for (int i = 0; i < methodType.parameterCount(); i ++) {
            if (MemoryBlock.class.isAssignableFrom(methodType.parameterType(i)))
                methodHandle = MethodHandles.filterArguments(methodHandle, i, SEGMENT_TO_BLOCK);
        }
        if (Modifier.isStatic(method.getModifiers())) object = method.getDeclaringClass();
        else methodHandle = methodHandle.bindTo(object);
        Arena arena = Arena.ofShared();
        MemoryLayout returnLayout = returnType == ForeignType.VOID ? null : toMemoryLayout(returnType);
        MemoryLayout[] parameterLayouts = new MemoryLayout[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i ++) {
            parameterLayouts[i] = toMemoryLayout(parameterTypes[i]);
        }
        for (CallOption option : options) {
            if (option.equals(StandardCallOption.STDCALL)) continue;
            throw new IllegalArgumentException(option + " not supported");
        }
        Linker.Option[] linkerOptions = firstVarArg >= 0 ? new Linker.Option[] { Linker.Option.firstVariadicArg(firstVarArg) }
        : EMPTY_OPTION_ARRAY;
        long address = LINKER.upcallStub(methodHandle, returnType == ForeignType.VOID ?
                FunctionDescriptor.ofVoid(parameterLayouts) : FunctionDescriptor.of(returnLayout, parameterLayouts), arena,
                linkerOptions).address();
        UPCALL_STUB_CLEANER.register(object, arena::close);
        return address;
    }

    static MemoryLayout toMemoryLayout(ForeignType type) {
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
