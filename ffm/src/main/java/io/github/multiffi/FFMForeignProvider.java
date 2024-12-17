package io.github.multiffi;

import multiffi.CallOption;
import multiffi.CallOptions;
import multiffi.FirstVararg;
import multiffi.ForeignType;
import multiffi.FunctionPointer;
import multiffi.MarshalAs;
import multiffi.MemoryHandle;
import multiffi.RedirectTo;
import multiffi.StandardCallOption;
import multiffi.UnsatisfiedLinkException;
import multiffi.spi.ForeignProvider;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.Cleaner;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntFunction;

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

    private static final class CharsetHolder {
        private CharsetHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Charset UTF16_CHARSET = FFMUtil.IS_BIG_ENDIAN ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;
        public static final Charset UTF32_CHARSET = FFMUtil.IS_BIG_ENDIAN ? Charset.forName("UTF-32BE") : Charset.forName("UTF-32LE");
        public static final Charset WIDE_CHARSET = FFMUtil.ABIHolder.WCHAR_T.byteSize() == 2L ? UTF16_CHARSET : UTF32_CHARSET;
        public static final Charset ANSI_CHARSET = Charset.forName(System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding")));
        public static final Charset CONSOLE_CHARSET =
                Charset.forName(System.getProperty("stdout.encoding", System.getProperty("sun.stdout.encoding",
                        System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding")))));
    }

    @Override
    public Charset ansiCharset() {
        return CharsetHolder.ANSI_CHARSET;
    }

    @Override
    public Charset wideCharset() {
        return CharsetHolder.WIDE_CHARSET;
    }

    @Override
    public Charset utf16Charset() {
        return CharsetHolder.UTF16_CHARSET;
    }

    @Override
    public Charset utf32Charset() {
        return CharsetHolder.UTF32_CHARSET;
    }

    @Override
    public Charset consoleCharset() {
        return CharsetHolder.CONSOLE_CHARSET;
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

    private static final class SymbolLookupHolder {
        private SymbolLookupHolder() {
            throw new UnsupportedOperationException();
        }
        public static final AtomicReference<SymbolLookup> GLOBAL_LOOKUP_REFERENCE = new AtomicReference<>();
        static {
            // ClassLoader & C runtime default lookup
            SymbolLookup lookup = SymbolLookup.loaderLookup().or(FFMUtil.ABIHolder.LINKER.defaultLookup());
            boolean isLinux = FFMUtil.osNameStartsWithIgnoreCase("linux");
            String libraryName;
            if (FFMUtil.IS_WINDOWS) libraryName = FFMUtil.OS_NAME.startsWith("Windows CE") ? "coredll" : "msvcrt";
            else if (isLinux) libraryName = "libc.so.6";
            else libraryName = "c";
            // libc lookup
            lookup = lookup.or(SymbolLookup.libraryLookup(LibraryNameMapperHolder.LIBRARY_NAME_MAPPER.apply(libraryName), Arena.global()));
            // libm lookup
            if (!FFMUtil.IS_WINDOWS) lookup = lookup.or(SymbolLookup.libraryLookup(LibraryNameMapperHolder.LIBRARY_NAME_MAPPER.apply(isLinux ? "libm.so.6" : "m"), Arena.global()));
            GLOBAL_LOOKUP_REFERENCE.set(lookup);
        }
    }

    @Override
    public void loadLibrary(String libraryName) throws IOException {
        Objects.requireNonNull(libraryName);
        try {
            SymbolLookupHolder.GLOBAL_LOOKUP_REFERENCE.getAndUpdate(lookup -> lookup.or(SymbolLookup.libraryLookup(libraryName, Arena.global())));
        }
        catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void loadLibrary(File libraryFile) throws IOException {
        Objects.requireNonNull(libraryFile);
        try {
            SymbolLookupHolder.GLOBAL_LOOKUP_REFERENCE.getAndUpdate(lookup -> lookup.or(SymbolLookup.libraryLookup(libraryFile.getAbsoluteFile().toPath(), Arena.global())));
        }
        catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public long getSymbol(String symbolName) {
        Optional<MemorySegment> symbol = SymbolLookupHolder.GLOBAL_LOOKUP_REFERENCE.get().find(symbolName);
        if (symbol.isPresent()) return symbol.get().address();
        else throw new UnsatisfiedLinkException(String.format("Failed to get symbol: `%s`", symbolName));
    }

    private static final class LibraryNameMapperHolder {
        private LibraryNameMapperHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Function<String, String> LIBRARY_NAME_MAPPER;
        static {
            if (FFMUtil.IS_WINDOWS) LIBRARY_NAME_MAPPER = libraryName -> {
                if (libraryName.matches(".*\\.dll$")) return libraryName;
                else return System.mapLibraryName(libraryName);
            };
            else if (FFMUtil.osNameStartsWithIgnoreCase("mac") || FFMUtil.osNameStartsWithIgnoreCase("darwin"))
                LIBRARY_NAME_MAPPER = libraryName -> {
                if (libraryName.matches("lib.*\\.(dylib|jnilib)$")) return libraryName;
                else return "lib" + libraryName + ".dylib";
            };
            else if (FFMUtil.osNameStartsWithIgnoreCase("os/400") || FFMUtil.osNameStartsWithIgnoreCase("os400"))
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

    @Override
    public String mapLibraryName(String libraryName) {
        if (libraryName == null) return null;
        else return LibraryNameMapperHolder.LIBRARY_NAME_MAPPER.apply(libraryName);
    }

    @Override
    public int getLastErrno() {
        return FFMLastErrno.get();
    }

    @Override
    public void setLastErrno(int errno) {
        FFMLastErrno.set(errno);
    }

    private static final class ErrorStringMapperHolder {
        private ErrorStringMapperHolder() {
            throw new UnsupportedOperationException();
        }
        public static final IntFunction<String> ERROR_STRING_MAPPER;
        static {
            if (FFMUtil.IS_WINDOWS) {
                SymbolLookup Kernel32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());
                MethodHandle FormatMessageW = FFMUtil.ABIHolder.LINKER.downcallHandle(Kernel32
                                .find("FormatMessageW").orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `FormatMessageW`")),
                        FunctionDescriptor.of(ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                MethodHandle LocalFree = FFMUtil.ABIHolder.LINKER.downcallHandle(Kernel32
                                .find("LocalFree").orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `LocalFree`")),
                        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                ThreadLocal<MemorySegment> lpBufferThreadLocal = ThreadLocal.withInitial(() -> Arena.global().allocate(FFMUtil.UnsafeHolder.UNSAFE.addressSize()));
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
                MethodHandle strerror = FFMUtil.ABIHolder.LINKER.downcallHandle(
                        FFMUtil.ABIHolder.LINKER.defaultLookup().find("strerror").orElseThrow(() -> new UnsatisfiedLinkException("Failed to get symbol: `strerror`")),
                        FunctionDescriptor.of(ValueLayout.ADDRESS, FFMUtil.ABIHolder.LINKER.canonicalLayouts().get("int"))
                );
                ERROR_STRING_MAPPER = errno -> {
                    try {
                        return ((MemorySegment) strerror.invokeExact(errno)).reinterpret(
                                        FFMUtil.UnsafeHolder.UNSAFE.addressSize() == 8 ? Long.MAX_VALUE : Integer.MAX_VALUE)
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

    @Override
    public String getErrorString(int errno) {
        return ErrorStringMapperHolder.ERROR_STRING_MAPPER.apply(errno);
    }

    @Override
    public FunctionPointer downcallHandle(long address, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return new FFMFunctionPointer(address, firstVararg, returnType, parameterTypes, options);
    }

    private static void checkProxyClasses(ClassLoader classLoader, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            if (!clazz.isInterface()) throw new IllegalArgumentException(clazz.getName() + " is not an interface");
            if (clazz.isHidden()) throw new IllegalArgumentException(clazz.getName() + " is a hidden interface");
            if (clazz.isSealed()) throw new IllegalArgumentException(clazz.getName() + " is a sealed interface");
            checkVisible(classLoader, clazz);
            for (Method method : clazz.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    for (Class<?> parameterType : method.getParameterTypes()) {
                        while (parameterType.isArray()) parameterType = parameterType.getComponentType();
                        if (!parameterType.isPrimitive()) checkVisible(classLoader, parameterType);
                    }
                    for (Class<?> exceptionType : method.getExceptionTypes()) {
                        while (exceptionType.isArray()) exceptionType = exceptionType.getComponentType();
                        if (!exceptionType.isPrimitive()) checkVisible(classLoader, exceptionType);
                    }
                    Class<?> returnType = method.getReturnType();
                    while (returnType.isArray()) returnType = returnType.getComponentType();
                    if (!returnType.isPrimitive()) checkVisible(classLoader, returnType);
                }
            }
        }
    }
    private static void checkVisible(ClassLoader classLoader, Class<?> clazz) {
        Class<?> type = null;
        try {
            type = Class.forName(clazz.getName(), false, classLoader);
        }
        catch (ClassNotFoundException ignored) {
        }
        if (type != clazz) throw new IllegalArgumentException(clazz.getName() +
                " referenced from a method is not visible from class loader: " + classLoader);
    }

    private static final AtomicInteger nextSerialNumber = new AtomicInteger();
    @Override
    public Object downcallProxy(ClassLoader classLoader, Class<?>... classes) {
        if (classes.length == 0) return null;
        else if (classes.length > 65535)
            throw new IllegalArgumentException("interface limit exceeded: " + classes.length);
        if (classLoader == null) classLoader = Thread.currentThread().getContextClassLoader();
        checkProxyClasses(classLoader, classes);

        String proxyName = "multiffi.ffm.Proxy$" + nextSerialNumber.getAndIncrement();
        String proxyInternalName = proxyName.replace('.', '/');
        String[] classInternalNames = new String[classes.length];
        for (int i = 0; i < classes.length; i ++) {
            classInternalNames[i] = Type.getInternalName(classes[i]);
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PRIVATE | Opcodes.ACC_SUPER,
                proxyInternalName, null, "java/lang/Object", classInternalNames);

        MethodVisitor objectInit = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        objectInit.visitCode();
        objectInit.visitVarInsn(Opcodes.ALOAD, 0);
        objectInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        objectInit.visitInsn(Opcodes.RETURN);
        objectInit.visitMaxs(1, 1);
        objectInit.visitEnd();

        MethodVisitor classInit = classWriter.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "<clinit>", "()V", null, null);
        classInit.visitCode();

        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "multiffi/spi/ForeignProvider", "getImplementation",
                "()Lmultiffi/spi/ForeignProvider;", false);
        classInit.visitVarInsn(Opcodes.ASTORE, 1);
        for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                if (method.isDefault() || method.getDeclaringClass() == Object.class) continue;
                String methodName = method.getName();
                String methodPrivateName = "handle" + Integer.toHexString(method.hashCode());
                CallOptions callOptions = method.getDeclaredAnnotation(CallOptions.class);
                FirstVararg firstVararg = method.getDeclaredAnnotation(FirstVararg.class);
                RedirectTo redirectTo = method.getDeclaredAnnotation(RedirectTo.class);
                boolean saveErrno = false;
                boolean critical = false;
                boolean trivial = false;
                if (callOptions != null) {
                    for (String callOption : callOptions.value()) {
                        if (callOption.equalsIgnoreCase("critical")) critical = true;
                        else if (callOption.equalsIgnoreCase("trivial")) trivial = critical = true;
                        else if (callOption.equalsIgnoreCase("save_errno")) saveErrno = true;
                        else if (!callOption.equalsIgnoreCase("stdcall"))
                            throw new IllegalArgumentException("Illegal call option: '" + callOption + "'");
                    }
                }
                if (saveErrno) critical = trivial = false;
                int firstVarargIndex = firstVararg == null ? -1 : firstVararg.value();
                String symbolName = redirectTo == null ? methodName : redirectTo.value();

                classWriter.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                        methodPrivateName, "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();

                int maxLocals = method.getParameterCount() + 1;
                for (Class<?> parameterType : method.getParameterTypes()) {
                    if (parameterType == long.class || parameterType == double.class) maxLocals ++;
                }
                MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, methodName, Type.getMethodDescriptor(method),
                        null, null);
                methodVisitor.visitCode();
                if (saveErrno) {
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/multiffi/FFMLastErrno", "handle",
                            "()Ljava/lang/foreign/MemorySegment;", false);
                    methodVisitor.visitVarInsn(Opcodes.ASTORE, maxLocals);
                }
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, proxyInternalName, methodPrivateName, "Ljava/lang/invoke/MethodHandle;");
                if (saveErrno) methodVisitor.visitVarInsn(Opcodes.ALOAD, maxLocals);

                classInit.visitVarInsn(Opcodes.ALOAD, 1);
                classInit.visitLdcInsn(symbolName);
                classInit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/spi/ForeignProvider", "getSymbol",
                        "(Ljava/lang/String;)J", false);
                classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemorySegment", "ofAddress",
                        "(J)Ljava/lang/foreign/MemorySegment;", true);
                classInit.visitVarInsn(Opcodes.ASTORE, 2);
                Parameter[] parameters = method.getParameters();
                Class<?> returnType = method.getReturnType();
                classInit.visitIntInsn(Opcodes.SIPUSH, parameters.length);
                classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                classInit.visitVarInsn(Opcodes.ASTORE, 3);
                int index = 0;
                for (int i = 0; i < parameters.length; i ++) {
                    classInit.visitVarInsn(Opcodes.ALOAD, 3);
                    classInit.visitIntInsn(Opcodes.SIPUSH, i);
                    dumpMemoryLayout(classInit, parameters[i].getType(), parameters[i].getDeclaredAnnotation(MarshalAs.class));
                    classInit.visitInsn(Opcodes.AASTORE);
                    dumpParameter(methodVisitor, parameters[i].getType(), 1 + index);
                    if (parameters[i].getType() == long.class || parameters[i].getType() == double.class) index ++;
                    index ++;
                }
                if (returnType == void.class) {
                    classInit.visitVarInsn(Opcodes.ALOAD, 3);
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/FunctionDescriptor", "ofVoid",
                            "([Ljava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/FunctionDescriptor;",
                            true);
                }
                else {
                    dumpMemoryLayout(classInit, returnType, method.getDeclaredAnnotation(MarshalAs.class));
                    classInit.visitVarInsn(Opcodes.ALOAD, 3);
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/FunctionDescriptor", "of",
                            "(Ljava/lang/foreign/MemoryLayout;[Ljava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/FunctionDescriptor;",
                            true);
                }
                classInit.visitVarInsn(Opcodes.ASTORE, 3);
                classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker", "nativeLinker",
                        "()Ljava/lang/foreign/Linker;", true);
                int linkerOptionsLength = 0;
                if (saveErrno) linkerOptionsLength ++;
                if (critical) linkerOptionsLength ++;
                if (firstVarargIndex != -1) linkerOptionsLength ++;
                classInit.visitIntInsn(Opcodes.BIPUSH, linkerOptionsLength);
                classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/foreign/Linker$Option");
                classInit.visitVarInsn(Opcodes.ASTORE, 4);
                int linkerOptionsIndex = 0;
                if (saveErrno) {
                    classInit.visitIntInsn(Opcodes.BIPUSH, 1);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
                    classInit.visitVarInsn(Opcodes.ASTORE, 5);
                    classInit.visitVarInsn(Opcodes.ALOAD, 5);
                    classInit.visitIntInsn(Opcodes.BIPUSH, 0);
                    classInit.visitLdcInsn(FFMLastErrno.name());
                    classInit.visitInsn(Opcodes.AASTORE);
                    classInit.visitVarInsn(Opcodes.ALOAD, 5);
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker$Option",
                            "captureCallState", "([Ljava/lang/String;)Ljava/lang/foreign/Linker$Option;", true);
                    classInit.visitVarInsn(Opcodes.ASTORE, 5);
                    classInit.visitVarInsn(Opcodes.ALOAD, 4);
                    classInit.visitIntInsn(Opcodes.BIPUSH, linkerOptionsIndex ++);
                    classInit.visitVarInsn(Opcodes.ALOAD, 5);
                    classInit.visitInsn(Opcodes.AASTORE);
                }
                if (critical) {
                    classInit.visitLdcInsn(!trivial);
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker$Option",
                            "critical", "(Z)Ljava/lang/foreign/Linker$Option;", true);
                    classInit.visitVarInsn(Opcodes.ASTORE, 5);
                    classInit.visitVarInsn(Opcodes.ALOAD, 4);
                    classInit.visitIntInsn(Opcodes.BIPUSH, linkerOptionsIndex ++);
                    classInit.visitVarInsn(Opcodes.ALOAD, 5);
                    classInit.visitInsn(Opcodes.AASTORE);
                }
                if (firstVarargIndex != -1) {
                    classInit.visitLdcInsn(firstVarargIndex);
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker$Option",
                            "firstVariadicArg", "(I)Ljava/lang/foreign/Linker$Option;", true);
                    classInit.visitVarInsn(Opcodes.ASTORE, 5);
                    classInit.visitVarInsn(Opcodes.ALOAD, 4);
                    classInit.visitIntInsn(Opcodes.BIPUSH, linkerOptionsIndex);
                    classInit.visitVarInsn(Opcodes.ALOAD, 5);
                    classInit.visitInsn(Opcodes.AASTORE);
                }
                classInit.visitVarInsn(Opcodes.ALOAD, 2);
                classInit.visitVarInsn(Opcodes.ALOAD, 3);
                classInit.visitVarInsn(Opcodes.ALOAD, 4);
                classInit.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/foreign/Linker", "downcallHandle",
                        "(Ljava/lang/foreign/MemorySegment;Ljava/lang/foreign/FunctionDescriptor;[Ljava/lang/foreign/Linker$Option;)Ljava/lang/invoke/MethodHandle;",
                        true);
                classInit.visitVarInsn(Opcodes.ASTORE, 2);
                for (int i = 0; i < parameters.length; i ++) {
                    if (parameters[i].getType() != long.class) continue;
                    MarshalAs marshalAs = parameters[i].getDeclaredAnnotation(MarshalAs.class);
                    String marshalTypeString = marshalAs == null ? null : marshalAs.value();
                    if ("short".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.SHORT.byteSize() == 2) {
                        classInit.visitInsn(Opcodes.ICONST_1);
                        classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                        classInit.visitVarInsn(Opcodes.ASTORE, 3);
                        classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        classInit.visitInsn(Opcodes.ICONST_1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/FFMMethodFilters", "LONG_TO_SHORT",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitInsn(Opcodes.AASTORE);
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        classInit.visitLdcInsn(i);
                        classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 2);
                    }
                    else if (("int".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.INT.byteSize() == 4)
                            || ("long".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.LONG.byteSize() == 4)
                            || ("size_t".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.SIZE_T.byteSize() == 4)) {
                        classInit.visitInsn(Opcodes.ICONST_1);
                        classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                        classInit.visitVarInsn(Opcodes.ASTORE, 3);
                        classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        classInit.visitInsn(Opcodes.ICONST_1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/FFMMethodFilters", "LONG_TO_INT",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitInsn(Opcodes.AASTORE);
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        classInit.visitLdcInsn(i);
                        classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 2);
                    }
                    else if (marshalAs != null && !"short".equalsIgnoreCase(marshalTypeString) && !"int".equalsIgnoreCase(marshalTypeString)
                            && !"long".equalsIgnoreCase(marshalTypeString) && !"size_t".equalsIgnoreCase(marshalTypeString))
                        throw new IllegalArgumentException("Illegal marshal type: '" + marshalTypeString + "'");
                }
                MarshalAs marshalAs = method.getDeclaredAnnotation(MarshalAs.class);
                String marshalTypeString = marshalAs == null ? null : marshalAs.value();
                if ("short".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.SHORT.byteSize() == 2) {
                    classInit.visitVarInsn(Opcodes.ALOAD, 2);
                    classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/FFMMethodFilters", "SHORT_TO_LONG",
                            "Ljava/lang/invoke/MethodHandle;");
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                            "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                    classInit.visitVarInsn(Opcodes.ASTORE, 2);
                }
                else if (("int".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.INT.byteSize() == 4)
                        || ("long".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.LONG.byteSize() == 4)
                        || ("size_t".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.SIZE_T.byteSize() == 4)) {
                    classInit.visitVarInsn(Opcodes.ALOAD, 2);
                    classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/FFMMethodFilters", "INT_TO_LONG",
                            "Ljava/lang/invoke/MethodHandle;");
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                            "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                    classInit.visitVarInsn(Opcodes.ASTORE, 2);
                }
                else if (marshalAs != null && !"short".equalsIgnoreCase(marshalTypeString) && !"int".equalsIgnoreCase(marshalTypeString)
                        && !"long".equalsIgnoreCase(marshalTypeString) && !"size_t".equalsIgnoreCase(marshalTypeString))
                    throw new IllegalArgumentException("Illegal marshal type: '" + marshalTypeString + "'");
                classInit.visitVarInsn(Opcodes.ALOAD, 2);
                classInit.visitFieldInsn(Opcodes.PUTSTATIC, proxyInternalName, methodPrivateName,
                        "Ljava/lang/invoke/MethodHandle;");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                        getMethodDescriptor(method, saveErrno), false);
                dumpReturn(methodVisitor, returnType);
                methodVisitor.visitMaxs(0, 0);
                methodVisitor.visitEnd();
            }
        }
        classInit.visitInsn(Opcodes.RETURN);
        classInit.visitMaxs(0, 0);
        classInit.visitEnd();

        try {
            return FFMUtil.UnsafeHolder.IMPL_LOOKUP.findConstructor(FFMUtil.defineClass(classLoader, proxyName, classWriter.toByteArray()),
                    MethodType.methodType(void.class)).invoke();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static void dumpMemoryLayout(MethodVisitor methodVisitor, Class<?> clazz, MarshalAs marshalAs) {
        if (clazz == boolean.class)
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_BOOLEAN",
                    "Ljava/lang/foreign/ValueLayout$OfBoolean;");
        else if (clazz == char.class)
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_CHAR",
                    "Ljava/lang/foreign/ValueLayout$OfChar;");
        else if (clazz == byte.class)
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_BYTE",
                    "Ljava/lang/foreign/ValueLayout$OfByte;");
        else if (clazz == short.class)
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_SHORT",
                    "Ljava/lang/foreign/ValueLayout$OfShort;");
        else if (clazz == int.class)
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                    "Ljava/lang/foreign/ValueLayout$OfInt;");
        else if (clazz == long.class) {
            String marshalTypeString = marshalAs == null ? null : marshalAs.value();
            if ("short".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.SHORT.byteSize() == 2)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_SHORT",
                        "Ljava/lang/foreign/ValueLayout$OfShort;");
            else if (("int".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.INT.byteSize() == 4)
                    || ("long".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.LONG.byteSize() == 4)
                    || ("size_t".equalsIgnoreCase(marshalTypeString) && FFMUtil.ABIHolder.SIZE_T.byteSize() == 4))
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                        "Ljava/lang/foreign/ValueLayout$OfInt;");
            else if (marshalAs != null && !"short".equalsIgnoreCase(marshalTypeString) && !"int".equalsIgnoreCase(marshalTypeString)
                    && !"long".equalsIgnoreCase(marshalTypeString) && !"size_t".equalsIgnoreCase(marshalTypeString))
                throw new IllegalArgumentException("Illegal marshal type: '" + marshalTypeString + "'");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                        "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (clazz == float.class)
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_FLOAT",
                    "Ljava/lang/foreign/ValueLayout$OfFloat;");
        else if (clazz == double.class)
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_DOUBLE",
                    "Ljava/lang/foreign/ValueLayout$OfDouble;");
        else throw new IllegalArgumentException("Illegal mapping type: " + clazz);
    }

    private static void dumpParameter(MethodVisitor methodVisitor, Class<?> clazz, int index) {
        int opcode;
        if (clazz == boolean.class) opcode = Opcodes.ILOAD;
        else if (clazz == char.class) opcode = Opcodes.ILOAD;
        else if (clazz == byte.class) opcode = Opcodes.ILOAD;
        else if (clazz == short.class) opcode = Opcodes.ILOAD;
        else if (clazz == int.class) opcode = Opcodes.ILOAD;
        else if (clazz == long.class) opcode = Opcodes.LLOAD;
        else if (clazz == float.class) opcode = Opcodes.FLOAD;
        else if (clazz == double.class) opcode = Opcodes.DLOAD;
        else throw new IllegalStateException("Unexpected exception");
        methodVisitor.visitVarInsn(opcode, index);
    }

    private static void dumpReturn(MethodVisitor methodVisitor, Class<?> clazz) {
        int opcode;
        if (clazz == void.class) opcode = Opcodes.RETURN;
        else if (clazz == boolean.class) opcode = Opcodes.IRETURN;
        else if (clazz == char.class) opcode = Opcodes.IRETURN;
        else if (clazz == byte.class) opcode = Opcodes.IRETURN;
        else if (clazz == short.class) opcode = Opcodes.IRETURN;
        else if (clazz == int.class) opcode = Opcodes.IRETURN;
        else if (clazz == long.class) opcode = Opcodes.LRETURN;
        else if (clazz == float.class) opcode = Opcodes.FRETURN;
        else if (clazz == double.class) opcode = Opcodes.DRETURN;
        else throw new IllegalStateException("Unexpected exception");
        methodVisitor.visitInsn(opcode);
    }

    public static String getMethodDescriptor(Method method, boolean saveErrno) {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        if (saveErrno) appendDescriptor(MemorySegment.class, builder);
        for (Class<?> parameter : method.getParameterTypes()) {
            appendDescriptor(parameter, builder);
        }
        builder.append(')');
        appendDescriptor(method.getReturnType(), builder);
        return builder.toString();
    }
    private static void appendDescriptor(Class<?> clazz, StringBuilder stringBuilder) {
        while (clazz.isArray()) {
            stringBuilder.append('[');
            clazz = clazz.getComponentType();
        }
        if (clazz.isPrimitive()) {
            char descriptor;
            if (clazz == Integer.TYPE) descriptor = 'I';
            else if (clazz == Void.TYPE) descriptor = 'V';
            else if (clazz == Boolean.TYPE) descriptor = 'Z';
            else if (clazz == Byte.TYPE) descriptor = 'B';
            else if (clazz == Character.TYPE) descriptor = 'C';
            else if (clazz == Short.TYPE) descriptor = 'S';
            else if (clazz == Double.TYPE) descriptor = 'D';
            else if (clazz == Float.TYPE) descriptor = 'F';
            else if (clazz == Long.TYPE) descriptor = 'J';
            else throw new IllegalStateException("Unexpected exception");
            stringBuilder.append(descriptor);
        }
        else stringBuilder.append('L').append(Type.getInternalName(clazz)).append(';');
    }

    private static final class CleanerHolder {
        private CleanerHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Cleaner CLEANER = Cleaner.create(runnable -> {
            Thread thread = new Thread(runnable, "MultiFFI/FFM Cleaner Thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static final Linker.Option[] EMPTY_OPTION_ARRAY = new Linker.Option[0];
    @Override
    public long upcallStub(Object object, Method method, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        MethodHandle methodHandle;
        try {
            methodHandle = FFMUtil.UnsafeHolder.IMPL_LOOKUP.unreflect(method);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception");
        }
        MethodType methodType = methodHandle.type();
        for (int i = 0; i < methodType.parameterCount(); i ++) {
            if (MemoryHandle.class.isAssignableFrom(methodType.parameterType(i)))
                methodHandle = MethodHandles.filterArguments(methodHandle, i, FFMMethodFilters.SEGMENT_TO_HANDLE);
        }
        if (MemoryHandle.class.isAssignableFrom(methodType.returnType()))
            methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMMethodFilters.HANDLE_TO_SEGMENT);
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
        Linker.Option[] linkerOptions = firstVararg >= 0 ? new Linker.Option[] { Linker.Option.firstVariadicArg(firstVararg) }
        : EMPTY_OPTION_ARRAY;
        long address = FFMUtil.ABIHolder.LINKER.upcallStub(methodHandle, returnType == ForeignType.VOID ?
                FunctionDescriptor.ofVoid(parameterLayouts) : FunctionDescriptor.of(returnLayout, parameterLayouts), arena,
                linkerOptions).address();
        CleanerHolder.CLEANER.register(object, arena::close);
        return address;
    }

}
