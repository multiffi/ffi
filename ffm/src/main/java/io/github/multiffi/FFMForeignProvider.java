package io.github.multiffi;

import multiffi.CallOption;
import multiffi.ForeignType;
import multiffi.FunctionPointer;
import multiffi.MemoryBlock;
import multiffi.StandardCallOption;
import multiffi.UnsatisfiedLinkException;
import multiffi.spi.ForeignProvider;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.Cleaner;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings({"deprecated", "removal"})
public class FFMForeignProvider extends ForeignProvider {

    @Override
    public long addressSize() {
        return FFMUtil.UnsafeHolder.UNSAFE.addressSize();
    }

    @Override
    public long shortSize() {
        return FFMUtil.ABIHolder.SHORT.byteSize();
    }

    @Override
    public long intSize() {
        return FFMUtil.ABIHolder.INT.byteSize();
    }

    @Override
    public long longSize() {
        return FFMUtil.ABIHolder.LONG.byteSize();
    }

    @Override
    public long wideCharSize() {
        return FFMUtil.ABIHolder.WCHAR_T.byteSize();
    }

    @Override
    public long pageSize() {
        return FFMUtil.UnsafeHolder.UNSAFE.pageSize();
    }

    @Override
    public Charset ansiCharset() {
        return FFMUtil.CharsetHolder.ANSI_CHARSET;
    }

    @Override
    public Charset wideCharset() {
        return FFMUtil.CharsetHolder.WIDE_CHARSET;
    }

    @Override
    public Charset utf16Charset() {
        return FFMUtil.CharsetHolder.UTF16_CHARSET;
    }

    @Override
    public Charset utf32Charset() {
        return FFMUtil.CharsetHolder.UTF32_CHARSET;
    }

    @Override
    public Charset consoleCharset() {
        return FFMUtil.CharsetHolder.CONSOLE_CHARSET;
    }

    @Override
    public ByteBuffer wrapDirectBuffer(long address, int capacity) {
        return MemorySegment.ofAddress(address).reinterpret(capacity).asByteBuffer();
    }

    @Override
    public ByteBuffer wrapDirectBuffer(long address) {
        return MemorySegment.ofAddress(address).reinterpret(Integer.MAX_VALUE - 8).asByteBuffer();
    }

    private static final class BufferFieldMethodHolder {
        private BufferFieldMethodHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Field ADDRESS;
        public static final Method BASE;
        static {
            try {
                ADDRESS = Buffer.class.getDeclaredField("address");
                BASE = Buffer.class.getDeclaredMethod("base");
            }
            catch (NoSuchFieldException | NoSuchMethodException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
    }

    @Override
    public long getDirectBufferAddress(Buffer buffer) {
        if (buffer.isDirect()) return FFMUtil.UnsafeHolder.UNSAFE.getLong(buffer,
                FFMUtil.UnsafeHolder.UNSAFE.objectFieldOffset(BufferFieldMethodHolder.ADDRESS));
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
                return (ByteBuffer) FFMUtil.UnsafeHolder.UNSAFE.getObject(buffer,
                        FFMUtil.UnsafeHolder.UNSAFE.objectFieldOffset(buffer.getClass().getDeclaredField("bb")));
            } catch (NoSuchFieldException | ClassCastException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
        else return null;
    }

    @Override
    public void cleanBuffer(Buffer buffer) {
        ByteBuffer byteBuffer = getByteBuffer(buffer);
        if (byteBuffer != null) FFMUtil.UnsafeHolder.UNSAFE.invokeCleaner(byteBuffer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Buffer> T getBufferAttachment(T buffer) {
        if (buffer == null || !buffer.isDirect()) return null;
        else {
            try {
                return (T) FFMUtil.UnsafeHolder.IMPL_LOOKUP
                        .unreflect(buffer.getClass().getMethod("attachment")).bindTo(buffer).invokeWithArguments();
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
                return FFMUtil.UnsafeHolder.IMPL_LOOKUP.unreflect(BufferFieldMethodHolder.BASE).bindTo(buffer).invokeWithArguments();
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
                return FFMUtil.UnsafeHolder.UNSAFE.getInt(buffer,
                        FFMUtil.UnsafeHolder.UNSAFE.objectFieldOffset(buffer.getClass().getDeclaredField("offset")));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
        else return buffer.arrayOffset() * FFMUtil.UnsafeHolder.UNSAFE.arrayIndexScale(buffer.array().getClass());
    }

    @Override
    public void sneakyThrows(Throwable throwable) {
        if (throwable != null) FFMUtil.UnsafeHolder.UNSAFE.throwException(throwable);
    }

    private static final Runtime RUNTIME = Runtime.getRuntime();

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

    @Override
    public boolean isBigEndian() {
        return FFMUtil.IS_BIG_ENDIAN;
    }

    @Override
    public boolean isLittleEndian() {
        return !FFMUtil.IS_BIG_ENDIAN;
    }

    @Override
    public ByteOrder endianness() {
        return ByteOrder.nativeOrder();
    }

    @Override
    public void loadLibrary(String libraryName) throws IOException {
        Objects.requireNonNull(libraryName);
        try {
            FFMUtil.SymbolLookupHolder.GLOBAL_LOOKUP_REFERENCE.getAndUpdate(lookup -> lookup.or(SymbolLookup.libraryLookup(libraryName, Arena.global())));
        }
        catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void loadLibrary(File libraryFile) throws IOException {
        Objects.requireNonNull(libraryFile);
        try {
            FFMUtil.SymbolLookupHolder.GLOBAL_LOOKUP_REFERENCE.getAndUpdate(lookup -> lookup.or(SymbolLookup.libraryLookup(libraryFile.getAbsoluteFile().toPath(), Arena.global())));
        }
        catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public long getSymbol(String symbolName) {
        Optional<MemorySegment> symbol = FFMUtil.SymbolLookupHolder.GLOBAL_LOOKUP_REFERENCE.get().find(symbolName);
        if (symbol.isPresent()) return symbol.get().address();
        else throw new UnsatisfiedLinkException(String.format("Failed to get symbol: `%s`", symbolName));
    }

    @Override
    public String mapLibraryName(String libraryName) {
        if (libraryName == null) return null;
        else return FFMUtil.LibraryNameMapperHolder.LIBRARY_NAME_MAPPER.apply(libraryName);
    }

    @Override
    public int getLastErrno() {
        return (int) FFMUtil.ErrnoHolder.ERRNO_HANDLE.get(FFMUtil.ErrnoHolder.ERRNO_THREAD_LOCAL.get(), 0L);
    }

    @Override
    public void setLastErrno(int errno) {
        FFMUtil.ErrnoHolder.ERRNO_HANDLE.set(FFMUtil.ErrnoHolder.ERRNO_THREAD_LOCAL.get(), errno);
    }

    @Override
    public String getErrorString(int errno) {
        return FFMUtil.ErrorStringMapperHolder.ERROR_STRING_MAPPER.apply(errno);
    }

    @Override
    public FunctionPointer downcallHandle(long address, int firstVarArg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return new FFMFunctionPointer(address, firstVarArg, returnType, parameterTypes, options);
    }

    private static final class UpcallStubCleanerHolder {
        private UpcallStubCleanerHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Cleaner UPCALL_STUB_CLEANER = Cleaner.create(runnable -> {
            Thread thread = new Thread(runnable, "FFM UpcallStub Cleaner Thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static final Linker.Option[] EMPTY_OPTION_ARRAY = new Linker.Option[0];
    @Override
    public long upcallStub(Object object, Method method, int firstVarArg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        MethodHandle methodHandle;
        try {
            methodHandle = FFMUtil.UnsafeHolder.IMPL_LOOKUP.unreflect(method);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception");
        }
        MethodType methodType = methodHandle.type();
        for (int i = 0; i < methodType.parameterCount(); i ++) {
            if (MemoryBlock.class.isAssignableFrom(methodType.parameterType(i)))
                methodHandle = MethodHandles.filterArguments(methodHandle, i, FFMUtil.MarshallerHolder.SEGMENT_TO_BLOCK);
        }
        if (MemoryBlock.class.isAssignableFrom(methodType.returnType()))
            methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMUtil.MarshallerHolder.BLOCK_TO_SEGMENT);
        if (Modifier.isStatic(method.getModifiers())) object = method.getDeclaringClass();
        else methodHandle = methodHandle.bindTo(object);
        Arena arena = Arena.ofShared();
        MemoryLayout returnLayout = returnType == ForeignType.VOID ? null : FFMUtil.toMemoryLayout(returnType);
        MemoryLayout[] parameterLayouts = new MemoryLayout[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i ++) {
            parameterLayouts[i] = FFMUtil.toMemoryLayout(parameterTypes[i]);
        }
        for (CallOption option : options) {
            if (option.equals(StandardCallOption.STDCALL)) continue;
            throw new IllegalArgumentException(option + " not supported");
        }
        Linker.Option[] linkerOptions = firstVarArg >= 0 ? new Linker.Option[] { Linker.Option.firstVariadicArg(firstVarArg) }
        : EMPTY_OPTION_ARRAY;
        long address = FFMUtil.ABIHolder.LINKER.upcallStub(methodHandle, returnType == ForeignType.VOID ?
                FunctionDescriptor.ofVoid(parameterLayouts) : FunctionDescriptor.of(returnLayout, parameterLayouts), arena,
                linkerOptions).address();
        UpcallStubCleanerHolder.UPCALL_STUB_CLEANER.register(object, arena::close);
        return address;
    }

}
