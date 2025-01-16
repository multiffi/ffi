package io.github.multiffi.ffi;

import multiffi.ffi.CallOption;
import multiffi.ffi.ForeignType;
import multiffi.ffi.FunctionHandle;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
import multiffi.ffi.FunctionOptionVisitor;
import multiffi.ffi.StandardCallOption;
import multiffi.ffi.spi.ForeignProvider;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Cleaner;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Map;

@SuppressWarnings({"deprecation", "removal"})
public class FFMForeignProvider extends ForeignProvider {

    public FFMForeignProvider() {
        this(true);
    }

    private final boolean proxyIntrinsics;
    public FFMForeignProvider(boolean proxyIntrinsics) {
        this.proxyIntrinsics = proxyIntrinsics;
    }

    @Override
    public long addressSize() {
        return FFMUtil.ADDRESS_SIZE;
    }

    @Override
    public long diffSize() {
        return FFMUtil.DIFF_SIZE;
    }

    @Override
    public long shortSize() {
        return FFMUtil.SHORT_SIZE;
    }

    @Override
    public long intSize() {
        return FFMUtil.INT_SIZE;
    }

    @Override
    public long longSize() {
        return FFMUtil.LONG_SIZE;
    }

    @Override
    public long wcharSize() {
        return FFMUtil.WCHAR_SIZE;
    }

    @Override
    public long pageSize() {
        return FFMUtil.PAGE_SIZE;
    }

    @Override
    public long alignSize() {
        return FFMUtil.ALIGN_SIZE;
    }

    @Override
    public Charset ansiCharset() {
        return FFMUtil.ANSI_CHARSET;
    }

    @Override
    public Charset wideCharset() {
        return FFMUtil.WIDE_CHARSET;
    }

    @Override
    public Charset utf16Charset() {
        return FFMUtil.UTF16_CHARSET;
    }

    @Override
    public Charset utf32Charset() {
        return FFMUtil.UTF32_CHARSET;
    }

    @Override
    public void sneakyThrows(Throwable throwable) {
        if (throwable != null) FFMUtil.UNSAFE.throwException(throwable);
    }

    @Override
    public void exit(int status) {
        Runtime.getRuntime().exit(status);
    }

    @Override
    public void halt(int status) {
        Runtime.getRuntime().halt(status);
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
    public boolean isBigEndian() {
        return FFMUtil.IS_BIG_ENDIAN;
    }

    @Override
    public boolean isLittleEndian() {
        return !FFMUtil.IS_BIG_ENDIAN;
    }

    @Override
    public ByteOrder endianness() {
        return FFMUtil.NATIVE_ORDER;
    }

    @Override
    public void loadLibrary(String libraryName) throws UnsatisfiedLinkError {
        FFMSymbolLookup.loadLibrary(libraryName);
    }

    @Override
    public void loadLibrary(File libraryFile) throws UnsatisfiedLinkError {
        FFMSymbolLookup.loadLibrary(libraryFile);
    }

    @Override
    public long getSymbolAddress(String symbolName) throws UnsatisfiedLinkError {
        return FFMSymbolLookup.getSymbolAddress(symbolName);
    }

    @Override
    public String mapLibraryName(String libraryName) {
        return FFMUtil.mapLibraryName(libraryName);
    }

    @Override
    public int getLastErrno() {
        return FFMLastErrno.get();
    }

    @Override
    public void setLastErrno(int errno) {
        FFMLastErrno.set(errno);
    }

    @Override
    public String getErrorString(int errno) {
        return FFMUtil.getErrorString(errno);
    }

    @Override
    public FunctionHandle downcallHandle(long address, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return new FFMFunctionHandle(address, firstVarArgIndex, returnType, parameterTypes, options);
    }

    @Override
    public Object downcallProxy(ClassLoader classLoader, Class<?>[] classes, FunctionOptionVisitor functionOptionVisitor) {
        if (functionOptionVisitor == null) functionOptionVisitor = Util.DEFAULT_SIGNATURE_VISITOR;
        if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
        return (proxyIntrinsics && FFMUtil.PROXY_INTRINSICS) ? FFMASMRuntime.generateProxy(classLoader, classes, functionOptionVisitor) :
                classes == null || classes.length == 0 ? null : Proxy.newProxyInstance(classLoader, classes, new ForeignInvocationHandler(this, classes, functionOptionVisitor));
    }

    @Override
    public MemoryHandle upcallStub(Object object, Method method, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        MethodHandle methodHandle;
        try {
            methodHandle = FFMUtil.IMPL_LOOKUP.unreflect(method);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
        Arena arena = Arena.ofShared();
        MemoryLayout returnLayout = returnType == null ? null : FFMUtil.toMemoryLayout(returnType);
        MemoryLayout[] parameterLayouts;
        if (parameterTypes == null || parameterTypes.length == 1 && parameterTypes[0] == null)
            parameterLayouts = FFMUtil.EMPTY_MEMORY_LAYOUT_ARRAY;
        else {
            parameterLayouts = new MemoryLayout[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i ++) {
                parameterLayouts[i] = FFMUtil.toMemoryLayout(parameterTypes[i]);
            }
        }
        int parameterCount = parameterTypes == null ? 0 : parameterTypes.length;
        firstVarArgIndex = firstVarArgIndex >= 0 ? Math.min(firstVarArgIndex, parameterCount) : -1;
        Linker.Option[] linkerOptions;
        if (options == null) linkerOptions = FFMUtil.EMPTY_LINKER_OPTION_ARRAY;
        else {
            linkerOptions = firstVarArgIndex >= 0 ? new Linker.Option[] { Linker.Option.firstVariadicArg(firstVarArgIndex) } : null;
            for (CallOption option : options) {
                if (option.equals(StandardCallOption.STDCALL)) continue;
                throw new IllegalArgumentException(option + " not supported");
            }
        }
        for (int i = 0; i < parameterCount; i ++) {
            ForeignType parameterType = parameterTypes[i];
            if (parameterType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortArgument(methodHandle, i, true);
            else if (parameterType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntArgument(methodHandle, i, true);
            else if (parameterType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongArgument(methodHandle, i, true);
            else if (parameterType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterSizeArgument(methodHandle, i, true);
            else if (parameterType == ScalarType.ADDRESS) methodHandle = MethodHandles
                    .filterArguments(methodHandle, i, FFMMethodFilters.SEGMENT_TO_INT64);
            else if (parameterType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharArgument(methodHandle, i, true);
            else if (parameterType == ScalarType.BOOLEAN) methodHandle = FFMMethodFilters.filterBooleanArgument(methodHandle, i, true);
            else if (parameterType.isCompound()) methodHandle = MethodHandles
                    .filterArguments(methodHandle, i, FFMMethodFilters.SEGMENT_TO_HANDLE);
        }
        if (returnType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortReturnValue(methodHandle, true);
        else if (returnType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntReturnValue(methodHandle, true);
        else if (returnType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongReturnValue(methodHandle, true);
        else if (returnType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterSizeReturnValue(methodHandle, true);
        else if (returnType == ScalarType.ADDRESS) methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMMethodFilters.INT64_TO_SEGMENT);
        else if (returnType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharReturnValue(methodHandle, true);
        else if (returnType == ScalarType.BOOLEAN) methodHandle = FFMMethodFilters.filterBooleanReturnValue(methodHandle, true);
        else if (returnType != null && returnType.isCompound())
            methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMMethodFilters.HANDLE_TO_SEGMENT);
        if (Modifier.isStatic(method.getModifiers())) object = method.getDeclaringClass();
        else methodHandle = methodHandle.bindTo(object);
        long address = FFMUtil.LINKER.upcallStub(methodHandle, returnType == null ?
                FunctionDescriptor.ofVoid(parameterLayouts) : FunctionDescriptor.of(returnLayout, parameterLayouts), arena,
                linkerOptions).address();
        Cleaner.Cleanable cleanup = FFMCleaner.register(object, arena::close);
        return new DirectWrapperMemoryHandle(address, 0) {
            @Override
            protected void free(long address) {
                cleanup.clean();
            }
        };
    }

    @Override
    public Runnable registerCleaner(Object object, Runnable cleanup) {
        return FFMCleaner.register(object, cleanup)::clean;
    }

}
