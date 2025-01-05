package io.github.multiffi.ffi;

import multiffi.ffi.CallOption;
import multiffi.ffi.CallOptionVisitor;
import multiffi.ffi.Foreign;
import multiffi.ffi.ForeignType;
import multiffi.ffi.FunctionHandle;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
import multiffi.ffi.StandardCallOption;
import multiffi.ffi.UnsatisfiedLinkException;
import multiffi.ffi.spi.ForeignProvider;
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
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

@SuppressWarnings({"deprecation", "removal"})
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
    public long wcharSize() {
        return FFMUtil.ABIHolder.WCHAR_T.byteSize();
    }

    @Override
    public long pageSize() {
        return FFMUtil.UnsafeHolder.UNSAFE.pageSize() & 0xFFFFFFFFL;
    }

    private static final class CharsetHolder {
        private CharsetHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Charset UTF16_CHARSET = FFMUtil.IS_BIG_ENDIAN ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;
        public static final Charset UTF32_CHARSET = FFMUtil.IS_BIG_ENDIAN ? Charset.forName("UTF-32BE") : Charset.forName("UTF-32LE");
        public static final Charset WIDE_CHARSET = FFMUtil.ABIHolder.WCHAR_T.byteSize() == 2L ? UTF16_CHARSET : UTF32_CHARSET;
        public static final Charset ANSI_CHARSET = Charset.forName(System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding", Charset.defaultCharset().name())));
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
            // C runtime default lookup
            SymbolLookup lookup = FFMUtil.ABIHolder.LINKER.defaultLookup();
            String libraryName;
            if (FFMUtil.IS_WINDOWS) libraryName = FFMUtil.IS_WINDOWS_CE ? "coredll" : "msvcrt";
            else if (FFMUtil.IS_AIX || FFMUtil.IS_IBMI) libraryName = FFMUtil.ABIHolder.SIZE_T.byteSize() == 4L ? "libc.a(shr.o)" : "libc.a(shr_64.o)";
            else libraryName = "c";
            // libc lookup
            if (!FFMUtil.IS_LINUX)
                lookup = lookup.or(SymbolLookup.libraryLookup(LibraryNameMapperHolder.libraryNameMapperFunction.apply(libraryName), Arena.global()));
            // libm lookup
            if (!FFMUtil.IS_WINDOWS && !FFMUtil.IS_LINUX && !FFMUtil.IS_AIX && !FFMUtil.IS_IBMI)
                lookup = lookup.or(SymbolLookup.libraryLookup(LibraryNameMapperHolder.libraryNameMapperFunction.apply("m"), Arena.global()));
            GLOBAL_LOOKUP_REFERENCE.set(lookup);
        }
    }

    @Override
    public void loadLibrary(String libraryName) throws IOException {
        Objects.requireNonNull(libraryName);
        try {
            SymbolLookupHolder.GLOBAL_LOOKUP_REFERENCE.getAndUpdate(lookup -> lookup.or(SymbolLookup.libraryLookup(LibraryNameMapperHolder.libraryNameMapperFunction.apply(libraryName), Arena.global())));
        }
        catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void loadLibrary(File libraryFile) throws IOException {
        Objects.requireNonNull(libraryFile);
        try {
            SymbolLookupHolder.GLOBAL_LOOKUP_REFERENCE.getAndUpdate(lookup -> lookup.or(SymbolLookup.libraryLookup(LibraryNameMapperHolder.libraryNameMapperFunction.apply(libraryFile.getAbsoluteFile().getAbsolutePath()), Arena.global())));
        }
        catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public long getSymbolAddress(String symbolName) {
        Objects.requireNonNull(symbolName);
        Optional<MemorySegment> symbol = SymbolLookup.loaderLookup().or(SymbolLookupHolder.GLOBAL_LOOKUP_REFERENCE.get()).find(symbolName);
        if (symbol.isPresent()) return symbol.get().address();
        else throw new UnsatisfiedLinkException(String.format("Failed to get symbol: `%s`", symbolName));
    }

    private static final class LibraryNameMapperHolder {
        private LibraryNameMapperHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Function<String, String> libraryNameMapperFunction;
        static {
            if (FFMUtil.IS_WINDOWS) libraryNameMapperFunction = libraryName -> {
                if (libraryName.matches(".*\\.(drv|dll|ocx)$")) return libraryName;
                else return System.mapLibraryName(libraryName);
            };
            else if (FFMUtil.IS_MAC)
                libraryNameMapperFunction = libraryName -> {
                if (libraryName.matches("lib.*\\.(dylib|jnilib)$")) return libraryName;
                else return "lib" + libraryName + ".dylib";
            };
            else if (FFMUtil.IS_LINUX)
                libraryNameMapperFunction = libraryName -> {
                if (libraryName.matches("lib.*\\.so((?:\\.[0-9]+)*)$")) return libraryName;
                else return System.mapLibraryName(libraryName);
            };
            else if (FFMUtil.IS_AIX)
                libraryNameMapperFunction = libraryName -> {
                if (libraryName.matches("lib.*\\.(so|a\\(shr.o\\)|a\\(shr_64.o\\)|a|so.[.0-9]+)$")) return libraryName;
                else return "lib" + libraryName + ".a";
            };
            else if (FFMUtil.IS_IBMI) {
                if (FFMUtil.ABIHolder.SIZE_T.byteSize() == 4L) {
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
    }

    @Override
    public String mapLibraryName(String libraryName) {
        if (libraryName == null) return null;
        else return LibraryNameMapperHolder.libraryNameMapperFunction.apply(libraryName);
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
        public static final IntFunction<String> errorStringMapperFunction;
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
                        else return lpBuffer.get(ValueLayout.ADDRESS, 0).reinterpret((Integer.MAX_VALUE - 8) & 0xFFFFFFFFL).getString(0, CharsetHolder.WIDE_CHARSET);
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
                errorStringMapperFunction = errno -> {
                    try {
                        MemorySegment errorString = ((MemorySegment) strerror.invokeExact(errno)).reinterpret(
                                        FFMUtil.UnsafeHolder.UNSAFE.addressSize() == 8 ? Long.MAX_VALUE : Integer.MAX_VALUE);
                        if (MemorySegment.NULL.equals(errorString)) return "strerror failed with 0x" + Integer.toHexString(errno);
                        else return errorString.getString(0, CharsetHolder.ANSI_CHARSET);
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
        return ErrorStringMapperHolder.errorStringMapperFunction.apply(errno);
    }

    @Override
    public FunctionHandle downcallHandle(long address, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return new FFMFunctionHandle(address, firstVararg, returnType, parameterTypes, options);
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
    public Object downcallProxy(ClassLoader classLoader, Class<?>[] classes, CallOptionVisitor callOptionVisitor) {
        if (classes.length == 0) return null;
        else if (classes.length > 65535)
            throw new IllegalArgumentException("interface limit exceeded: " + classes.length);
        if (classLoader == null) {
            Class<?> clazz = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            classLoader = clazz == null ? ClassLoader.getSystemClassLoader() : clazz.getClassLoader();
        }
        checkProxyClasses(classLoader, classes);

        String proxyName = "multiffi.ffi.ffm.Proxy$" + nextSerialNumber.getAndIncrement();
        String proxyInternalName = proxyName.replace('.', '/');
        String[] classInternalNames = new String[classes.length];
        for (int i = 0; i < classes.length; i ++) {
            classInternalNames[i] = Type.getInternalName(classes[i]);
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PRIVATE | Opcodes.ACC_SUPER,
                proxyInternalName, null, "java/lang/Object", classInternalNames);

        MethodVisitor objectInit = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        objectInit.visitCode();
        objectInit.visitVarInsn(Opcodes.ALOAD, 0);
        objectInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        objectInit.visitInsn(Opcodes.RETURN);
        objectInit.visitMaxs(0, 0);
        objectInit.visitEnd();

        MethodVisitor classInit = classWriter.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "<clinit>", "()V", null, null);
        classInit.visitCode();

        for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                if (method.isDefault() || method.getDeclaringClass() == Object.class) continue;
                String methodName = method.getName();
                String methodFieldName = "function" + Integer.toHexString(method.hashCode());
                boolean dyncall = false;
                boolean saveErrno = false;
                boolean critical = false;
                boolean trivial = false;
                for (CallOption option : callOptionVisitor.visitCallOptions(method)) {
                    switch (option) {
                        case StandardCallOption.DYNCALL:
                            dyncall = true;
                            break;
                        case StandardCallOption.SAVE_ERRNO:
                            saveErrno = true;
                            break;
                        case StandardCallOption.TRIVIAL:
                            trivial = true;
                        case StandardCallOption.CRITICAL:
                            critical = true;
                            break;
                        case StandardCallOption.STDCALL:
                            break;
                        default:
                            throw new IllegalArgumentException(option + " not supported");
                    }
                }
                if (saveErrno && critical) critical = trivial = false;
                int firstVarArgIndex = callOptionVisitor.visitFirstVarArgIndex(method);
                long address = callOptionVisitor.visitAddress(method);

                classWriter.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                        methodFieldName, dyncall ? "Lmultiffi/ffi/FunctionHandle;" : "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();
                MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, methodName, Type.getMethodDescriptor(method),
                        null, null);
                methodVisitor.visitCode();
                if (dyncall) {
                    ForeignType[] parameterForeignTypes = callOptionVisitor.visitParameterTypes(method);
                    ForeignType returnForeignType = callOptionVisitor.visitReturnType(method);
                    boolean addReturnMemoryParameter = returnForeignType != null && returnForeignType.isCompound();
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length - 1 != parameterForeignTypes.length + (addReturnMemoryParameter ? 1 : 0))
                        throw new IndexOutOfBoundsException("Array length mismatch");
                    Class<?> returnType = method.getReturnType();

                    classInit.visitIntInsn(Opcodes.SIPUSH, parameterForeignTypes.length);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "multiffi/ffi/ForeignType");
                    classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    for (int i = 0; i < parameterForeignTypes.length; i ++) {
                        ForeignType parameterForeignType = parameterForeignTypes[i];
                        Class<?> parameterType = parameterTypes[(addReturnMemoryParameter ? 1 : 0) + i];
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitIntInsn(Opcodes.SIPUSH, i);
                        dumpForeignType(classInit, parameterType, parameterForeignType);
                        classInit.visitInsn(Opcodes.AASTORE);
                    }

                    int optionCount = 1;
                    if (critical) optionCount ++;
                    if (trivial) optionCount ++;
                    if (saveErrno) optionCount ++;
                    classInit.visitIntInsn(Opcodes.BIPUSH, optionCount);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "multiffi/ffi/CallOption");
                    classInit.visitVarInsn(Opcodes.ASTORE, 2);
                    classInit.visitVarInsn(Opcodes.ALOAD, 2);
                    classInit.visitIntInsn(Opcodes.BIPUSH, 0);
                    classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/StandardCallOption",
                            "DYNCALL", "Lmultiffi/ffi/StandardCallOption;");
                    classInit.visitInsn(Opcodes.AASTORE);
                    int optionIndex = 1;
                    if (critical) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        classInit.visitIntInsn(Opcodes.BIPUSH, optionIndex ++);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/StandardCallOption",
                                "CRITICAL", "Lmultiffi/ffi/StandardCallOption;");
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    if (trivial) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        classInit.visitIntInsn(Opcodes.BIPUSH, optionIndex ++);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/StandardCallOption",
                                "TRIVIAL", "Lmultiffi/ffi/StandardCallOption;");
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    if (saveErrno) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        classInit.visitIntInsn(Opcodes.BIPUSH, optionIndex);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/StandardCallOption",
                                "SAVE_ERRNO", "Lmultiffi/ffi/StandardCallOption;");
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    classInit.visitTypeInsn(Opcodes.NEW, "io/github/multiffi/ffi/FFMFunctionHandle");
                    classInit.visitInsn(Opcodes.DUP);
                    classInit.visitLdcInsn(address);
                    classInit.visitLdcInsn(firstVarArgIndex);
                    dumpForeignType(classInit, returnType, returnForeignType);
                    classInit.visitVarInsn(Opcodes.ALOAD, 1);
                    classInit.visitVarInsn(Opcodes.ALOAD, 2);
                    classInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "io/github/multiffi/ffi/FFMFunctionHandle", "<init>",
                            "(JILmultiffi/ffi/ForeignType;[Lmultiffi/ffi/ForeignType;[Lmultiffi/ffi/CallOption;)V", false);
                    classInit.visitFieldInsn(Opcodes.PUTSTATIC, proxyInternalName, methodFieldName,
                            "Lmultiffi/ffi/FunctionHandle;");

                    int storeIndex = parameterTypes.length + 1;
                    for (Class<?> parameterType : parameterTypes) {
                        if (parameterType == long.class || parameterType == double.class) storeIndex ++;
                    }
                    methodVisitor.visitIntInsn(Opcodes.BIPUSH, parameterTypes.length);
                    methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                    methodVisitor.visitVarInsn(Opcodes.ASTORE, storeIndex);

                    if (!parameterTypes[parameterTypes.length - 1].isArray())
                        throw new IllegalArgumentException("Last argument must be array as variadic arguments");
                    int index = 0;
                    for (int i = 0; i < parameterTypes.length; i ++) {
                        Class<?> parameterType = parameterTypes[i];
                        if (parameterType.isPrimitive()) {
                            dumpLoadOpcode(methodVisitor, parameterType, 1 + index ++);
                            if (parameterType == boolean.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf",
                                        "(Z)Ljava/lang/Boolean;", false);
                            else if (parameterType == byte.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf",
                                        "(B)Ljava/lang/Byte;", false);
                            else if (parameterType == char.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf",
                                        "(C)Ljava/lang/Character;", false);
                            else if (parameterType == short.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf",
                                        "(S)Ljava/lang/Short;", false);
                            else if (parameterType == int.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf",
                                        "(I)Ljava/lang/Integer;", false);
                            else if (parameterType == long.class) {
                                index ++;
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf",
                                        "(J)Ljava/lang/Long;", false);
                            }
                            else if (parameterType == float.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf",
                                        "(F)Ljava/lang/Float;", false);
                            else if (parameterType == double.class) {
                                index ++;
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf",
                                        "(D)Ljava/lang/Double;", false);
                            }
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, storeIndex + 1);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, storeIndex);
                            methodVisitor.visitIntInsn(Opcodes.BIPUSH, i);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, storeIndex + 1);
                        }
                        else {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, storeIndex);
                            methodVisitor.visitIntInsn(Opcodes.BIPUSH, i);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1 + index ++);
                        }
                        methodVisitor.visitInsn(Opcodes.AASTORE);
                    }

                    methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, proxyInternalName, methodFieldName, "Lmultiffi/ffi/FunctionHandle;");
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, storeIndex);
                    if (returnForeignType == ScalarType.BOOLEAN)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeBoolean",
                                "([Ljava/lang/Object;)Z", false);
                    else if (returnForeignType == ScalarType.INT8)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeInt8",
                                "([Ljava/lang/Object;)B", false);
                    else if (returnForeignType == ScalarType.CHAR)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeChar",
                                "([Ljava/lang/Object;)B", false);
                    else if (returnForeignType == ScalarType.UTF16)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeUTF16",
                                "([Ljava/lang/Object;)C", false);
                    else if (returnForeignType == ScalarType.INT16)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeInt16",
                                "([Ljava/lang/Object;)S", false);
                    else if (returnForeignType == ScalarType.WCHAR)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeWChar",
                                "([Ljava/lang/Object;)I", false);
                    else if (returnForeignType == ScalarType.INT32)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeInt32",
                                "([Ljava/lang/Object;)I", false);
                    else if (returnForeignType == ScalarType.INT64)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeInt64",
                                "([Ljava/lang/Object;)J", false);
                    else if (returnForeignType == ScalarType.SHORT)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeShort",
                                "([Ljava/lang/Object;)J", false);
                    else if (returnForeignType == ScalarType.INT)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeInt",
                                "([Ljava/lang/Object;)J", false);
                    else if (returnForeignType == ScalarType.LONG)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeLong",
                                "([Ljava/lang/Object;)J", false);
                    else if (returnForeignType == ScalarType.ADDRESS)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeAddress",
                                "([Ljava/lang/Object;)J", false);
                    else if (returnForeignType == ScalarType.FLOAT)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeFloat",
                                "([Ljava/lang/Object;)F", false);
                    else if (returnForeignType == ScalarType.DOUBLE)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeDouble",
                                "([Ljava/lang/Object;)D", false);
                    else methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeCompound",
                                "([Ljava/lang/Object;)Lmultiffi/ffi/MemoryHandle;", false);
                    dumpReturnOpcode(methodVisitor, returnType);
                    methodVisitor.visitMaxs(0, 0);
                    methodVisitor.visitEnd();
                }
                else {
                    int methodMaxLocals = method.getParameterCount() + 1;
                    for (Class<?> parameterType : method.getParameterTypes()) {
                        if (parameterType == long.class || parameterType == double.class) methodMaxLocals ++;
                    }

                    if (saveErrno) {
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/multiffi/ffi/FFMLastErrno", "segment",
                                "()Ljava/lang/foreign/MemorySegment;", false);
                        methodVisitor.visitVarInsn(Opcodes.ASTORE, methodMaxLocals);
                    }
                    methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, proxyInternalName, methodFieldName, "Ljava/lang/invoke/MethodHandle;");
                    if (saveErrno) methodVisitor.visitVarInsn(Opcodes.ALOAD, methodMaxLocals);

                    classInit.visitLdcInsn(address);
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemorySegment", "ofAddress",
                            "(J)Ljava/lang/foreign/MemorySegment;", true);
                    classInit.visitVarInsn(Opcodes.ASTORE, 1);

                    ForeignType[] parameterForeignTypes = callOptionVisitor.visitParameterTypes(method);
                    ForeignType returnForeignType = callOptionVisitor.visitReturnType(method);
                    boolean addReturnMemoryParameter = returnForeignType != null && returnForeignType.isCompound();
                    Parameter[] parameters = method.getParameters();
                    if (parameters.length != parameterForeignTypes.length + (addReturnMemoryParameter ? 1 : 0))
                        throw new IndexOutOfBoundsException("Array length mismatch");
                    Class<?> returnType = method.getReturnType();
                    classInit.visitIntInsn(Opcodes.SIPUSH, parameters.length);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                    classInit.visitVarInsn(Opcodes.ASTORE, 2);
                    int index = 0;
                    for (int i = (addReturnMemoryParameter ? 1 : 0); i < parameters.length; i ++) {
                        ForeignType parameterForeignType = parameterForeignTypes[i];
                        Class<?> parameterType = parameters[i].getType();
                        if (parameterForeignType.isCompound()) {
                            if (!MemoryHandle.class.isAssignableFrom(parameterType))
                                throw new IllegalArgumentException("Illegal mapping type; expected subclass of class MemoryHandle");
                            classInit.visitLdcInsn(parameterForeignType.size());
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_BYTE",
                                    "Ljava/lang/foreign/ValueLayout$OfByte;");
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemoryLayout",
                                    "sequenceLayout", "(JLjava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/SequenceLayout;", true);
                            classInit.visitVarInsn(Opcodes.ASTORE, 3);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/foreign/MemoryLayout");
                            classInit.visitVarInsn(Opcodes.ASTORE, 4);
                            classInit.visitVarInsn(Opcodes.ALOAD, 4);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitVarInsn(Opcodes.ALOAD, 3);
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 4);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemoryLayout",
                                    "structLayout", "(JLjava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/SequenceLayout;", true);
                            classInit.visitVarInsn(Opcodes.ASTORE, 3);
                        }
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        classInit.visitIntInsn(Opcodes.SIPUSH, i);
                        if (parameterForeignType.isCompound()) classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        else dumpMemoryLayout(classInit, parameterType, parameterForeignType);
                        classInit.visitInsn(Opcodes.AASTORE);
                        dumpLoadOpcode(methodVisitor, parameterType, 1 + index);
                        if (parameterType == long.class || parameterType == double.class) index ++;
                        index ++;
                    }
                    if (returnForeignType == null) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/FunctionDescriptor", "ofVoid",
                                "([Ljava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/FunctionDescriptor;",
                                true);
                    }
                    else {
                        if (returnForeignType.isCompound()) {
                            if (!MemoryHandle.class.isAssignableFrom(returnType))
                                throw new IllegalArgumentException("Illegal mapping type; expected subclass of class MemoryHandle");
                            classInit.visitLdcInsn(returnForeignType.size());
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_BYTE",
                                    "Ljava/lang/foreign/ValueLayout$OfByte;");
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemoryLayout",
                                    "sequenceLayout", "(JLjava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/SequenceLayout;", true);
                            classInit.visitVarInsn(Opcodes.ASTORE, 4);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/foreign/MemoryLayout");
                            classInit.visitVarInsn(Opcodes.ASTORE, 5);
                            classInit.visitVarInsn(Opcodes.ALOAD, 5);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitVarInsn(Opcodes.ALOAD, 4);
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 5);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemoryLayout",
                                    "structLayout", "(JLjava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/SequenceLayout;", true);
                        }
                        else dumpMemoryLayout(classInit, returnType, returnForeignType);
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/FunctionDescriptor", "of",
                                "(Ljava/lang/foreign/MemoryLayout;[Ljava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/FunctionDescriptor;",
                                true);
                    }
                    classInit.visitVarInsn(Opcodes.ASTORE, 2);
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker", "nativeLinker",
                            "()Ljava/lang/foreign/Linker;", true);
                    int linkerOptionsLength = 0;
                    if (saveErrno) linkerOptionsLength ++;
                    if (critical) linkerOptionsLength ++;
                    if (firstVarArgIndex != -1) linkerOptionsLength ++;
                    classInit.visitIntInsn(Opcodes.BIPUSH, linkerOptionsLength);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/foreign/Linker$Option");
                    classInit.visitVarInsn(Opcodes.ASTORE, 3);
                    int linkerOptionsIndex = 0;
                    if (saveErrno) {
                        classInit.visitInsn(Opcodes.ICONST_1);
                        classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
                        classInit.visitVarInsn(Opcodes.ASTORE, 4);
                        classInit.visitVarInsn(Opcodes.ALOAD, 4);
                        classInit.visitInsn(Opcodes.ICONST_0);
                        classInit.visitLdcInsn(FFMLastErrno.name());
                        classInit.visitInsn(Opcodes.AASTORE);
                        classInit.visitVarInsn(Opcodes.ALOAD, 4);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker$Option",
                                "captureCallState", "([Ljava/lang/String;)Ljava/lang/foreign/Linker$Option;", true);
                        classInit.visitVarInsn(Opcodes.ASTORE, 4);
                        classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        classInit.visitIntInsn(Opcodes.BIPUSH, linkerOptionsIndex ++);
                        classInit.visitVarInsn(Opcodes.ALOAD, 4);
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    if (critical) {
                        classInit.visitLdcInsn(!trivial);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker$Option",
                                "critical", "(Z)Ljava/lang/foreign/Linker$Option;", true);
                        classInit.visitVarInsn(Opcodes.ASTORE, 4);
                        classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        classInit.visitIntInsn(Opcodes.BIPUSH, linkerOptionsIndex ++);
                        classInit.visitVarInsn(Opcodes.ALOAD, 4);
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    if (firstVarArgIndex != -1) {
                        classInit.visitLdcInsn(firstVarArgIndex);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker$Option",
                                "firstVariadicArg", "(I)Ljava/lang/foreign/Linker$Option;", true);
                        classInit.visitVarInsn(Opcodes.ASTORE, 4);
                        classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        classInit.visitIntInsn(Opcodes.BIPUSH, linkerOptionsIndex);
                        classInit.visitVarInsn(Opcodes.ALOAD, 4);
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    classInit.visitVarInsn(Opcodes.ALOAD, 1);
                    classInit.visitVarInsn(Opcodes.ALOAD, 2);
                    classInit.visitVarInsn(Opcodes.ALOAD, 3);
                    classInit.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/foreign/Linker", "downcallHandle",
                            "(Ljava/lang/foreign/MemorySegment;Ljava/lang/foreign/FunctionDescriptor;[Ljava/lang/foreign/Linker$Option;)Ljava/lang/invoke/MethodHandle;",
                            true);
                    classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    for (int i = 0; i < parameters.length; i ++) {
                        ForeignType parameterForeignType = parameterForeignTypes[i];
                        if (parameterForeignType == ScalarType.SHORT && Foreign.shortSize() == 2) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "SHORT_TO_INT16",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            classInit.visitLdcInsn(i);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.INT && Foreign.intSize() == 4) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT_TO_INT32",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            classInit.visitLdcInsn(i);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.LONG && Foreign.longSize() == 4) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "LONG_TO_INT32",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            classInit.visitLdcInsn(i);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.SIZE && Foreign.addressSize() == 4) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "ADDRESS_TO_INT32",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            classInit.visitLdcInsn(i);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.ADDRESS) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT64_TO_SEGMENT",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            classInit.visitLdcInsn(i);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.WCHAR && Foreign.wcharSize() == 2) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "WCHAR_TO_UTF16",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            classInit.visitLdcInsn(i);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType.isCompound()) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "HANDLE_TO_SEGMENT",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            classInit.visitLdcInsn(i);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                    }
                    if (returnForeignType == ScalarType.SHORT && Foreign.shortSize() == 2) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT16_TO_INT64",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.INT && Foreign.intSize() == 4) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT32_TO_INT64",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.LONG && Foreign.longSize() == 4) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT32_TO_INT64",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.SIZE && Foreign.addressSize() == 4) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT32_TO_INT64",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.ADDRESS) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "SEGMENT_TO_INT64",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.WCHAR && Foreign.shortSize() == 2) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "UTF16_TO_INT32",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType != null && returnForeignType.isCompound()) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "SEGMENT_TO_HANDLE",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    classInit.visitVarInsn(Opcodes.ALOAD, 1);
                    classInit.visitFieldInsn(Opcodes.PUTSTATIC, proxyInternalName, methodFieldName,
                            "Ljava/lang/invoke/MethodHandle;");
                    methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                            getMethodDescriptor(method, saveErrno), false);
                    dumpReturnOpcode(methodVisitor, returnType);
                    methodVisitor.visitMaxs(0, 0);
                    methodVisitor.visitEnd();
                }
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

    private static void dumpMemoryLayout(MethodVisitor methodVisitor, Class<?> clazz, ForeignType type) {
        if (type == ScalarType.INT8 || type == ScalarType.CHAR) {
            if (clazz != byte.class) throw new IllegalArgumentException("Illegal mapping type; expected class byte");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_BYTE",
                    "Ljava/lang/foreign/ValueLayout$OfByte;");
        }
        else if (type == ScalarType.INT16) {
            if (clazz != short.class) throw new IllegalArgumentException("Illegal mapping type; expected class short");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_SHORT",
                    "Ljava/lang/foreign/ValueLayout$OfShort;");
        }
        else if (type == ScalarType.INT32) {
            if (clazz != int.class) throw new IllegalArgumentException("Illegal mapping type; expected class int");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                    "Ljava/lang/foreign/ValueLayout$OfInt;");
        }
        else if (type == ScalarType.INT64) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                        "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.WCHAR) {
            if (clazz != int.class) throw new IllegalArgumentException("Illegal mapping type; expected class int");
            if (Foreign.wcharSize() == 2)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_CHAR",
                        "Ljava/lang/foreign/ValueLayout$OfChar;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                        "Ljava/lang/foreign/ValueLayout$OfInt;");
        }
        else if (type == ScalarType.SHORT) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            if (Foreign.shortSize() == 2)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_SHORT",
                        "Ljava/lang/foreign/ValueLayout$OfShort;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                        "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.INT) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            if (Foreign.intSize() == 4)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                        "Ljava/lang/foreign/ValueLayout$OfInt;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                        "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.LONG) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            if (Foreign.longSize() == 4)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                        "Ljava/lang/foreign/ValueLayout$OfInt;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                        "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.SIZE) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            if (Foreign.addressSize() == 4)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                        "Ljava/lang/foreign/ValueLayout$OfInt;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                        "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.FLOAT) {
            if (clazz != float.class) throw new IllegalArgumentException("Illegal mapping type; expected class float");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_FLOAT",
                    "Ljava/lang/foreign/ValueLayout$OfFloat;");
        }
        else if (type == ScalarType.DOUBLE) {
            if (clazz != double.class) throw new IllegalArgumentException("Illegal mapping type; expected class double");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_DOUBLE",
                    "Ljava/lang/foreign/ValueLayout$OfDouble;");
        }
        else if (type == ScalarType.BOOLEAN) {
            if (clazz != boolean.class) throw new IllegalArgumentException("Illegal mapping type; expected class boolean");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_BOOLEAN",
                    "Ljava/lang/foreign/ValueLayout$OfBoolean;");
        }
        else if (type == ScalarType.UTF16) {
            if (clazz != char.class) throw new IllegalArgumentException("Illegal mapping type; expected class char");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_CHAR",
                    "Ljava/lang/foreign/ValueLayout$OfChar;");
        }
        else if (type == ScalarType.ADDRESS) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "ADDRESS",
                    "Ljava/lang/foreign/AddressLayout;");
        }
    }

    private static void dumpLoadOpcode(MethodVisitor methodVisitor, Class<?> clazz, int index) {
        int opcode;
        if (clazz == boolean.class) opcode = Opcodes.ILOAD;
        else if (clazz == char.class) opcode = Opcodes.ILOAD;
        else if (clazz == byte.class) opcode = Opcodes.ILOAD;
        else if (clazz == short.class) opcode = Opcodes.ILOAD;
        else if (clazz == int.class) opcode = Opcodes.ILOAD;
        else if (clazz == long.class) opcode = Opcodes.LLOAD;
        else if (clazz == float.class) opcode = Opcodes.FLOAD;
        else if (clazz == double.class) opcode = Opcodes.DLOAD;
        else opcode = Opcodes.ALOAD;
        methodVisitor.visitVarInsn(opcode, index);
    }

    private static void dumpReturnOpcode(MethodVisitor methodVisitor, Class<?> clazz) {
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
        else opcode = Opcodes.ARETURN;
        methodVisitor.visitInsn(opcode);
    }

    private static void dumpForeignType(MethodVisitor methodVisitor, Class<?> clazz, ForeignType type) {
        String name;
        if (type == ScalarType.INT8) {
            if (clazz != byte.class) throw new IllegalArgumentException("Illegal mapping type; expected class byte");
            name = "INT8";
        }
        else if (type == ScalarType.CHAR) {
            if (clazz != byte.class) throw new IllegalArgumentException("Illegal mapping type; expected class byte");
            name = "CHAR";
        }
        else if (type == ScalarType.INT16) {
            if (clazz != short.class) throw new IllegalArgumentException("Illegal mapping type; expected class short");
            name = "INT16";
        }
        else if (type == ScalarType.INT32) {
            if (clazz != int.class) throw new IllegalArgumentException("Illegal mapping type; expected class int");
            name = "INT32";
        }
        else if (type == ScalarType.INT64) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            name = "INT64";
        }
        else if (type == ScalarType.WCHAR) {
            if (clazz != int.class) throw new IllegalArgumentException("Illegal mapping type; expected class int");
            name = "WCHAR";
        }
        else if (type == ScalarType.SHORT) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            name = "SHORT";
        }
        else if (type == ScalarType.INT) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            name = "INT";
        }
        else if (type == ScalarType.LONG) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            name = "LONG";
        }
        else if (type == ScalarType.SIZE) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            name = "SIZE";
        }
        else if (type == ScalarType.FLOAT) {
            if (clazz != float.class) throw new IllegalArgumentException("Illegal mapping type; expected class float");
            name = "FLOAT";
        }
        else if (type == ScalarType.DOUBLE) {
            if (clazz != double.class) throw new IllegalArgumentException("Illegal mapping type; expected class double");
            name = "DOUBLE";
        }
        else if (type == ScalarType.BOOLEAN) {
            if (clazz != boolean.class) throw new IllegalArgumentException("Illegal mapping type; expected class boolean");
            name = "BOOLEAN";
        }
        else if (type == ScalarType.UTF16) {
            if (clazz != char.class) throw new IllegalArgumentException("Illegal mapping type; expected class char");
            name = "UTF16";
        }
        else if (type == ScalarType.ADDRESS) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected class long");
            name = "ADDRESS";
        }
        else throw new IllegalStateException("Unexpected exception");
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/ScalarType", name, "Lmultiffi/ffi/ScalarType;");
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
            if (clazz == int.class) descriptor = 'I';
            else if (clazz == void.class) descriptor = 'V';
            else if (clazz == boolean.class) descriptor = 'Z';
            else if (clazz == byte.class) descriptor = 'B';
            else if (clazz == char.class) descriptor = 'C';
            else if (clazz == short.class) descriptor = 'S';
            else if (clazz == double.class) descriptor = 'D';
            else if (clazz == float.class) descriptor = 'F';
            else if (clazz == long.class) descriptor = 'J';
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
        Arena arena = Arena.ofShared();
        MemoryLayout returnLayout = returnType == null ? null : FFMUtil.toMemoryLayout(returnType);
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
        for (int i = 0; i < parameterTypes.length; i ++) {
            ForeignType parameterType = parameterTypes[i];
            if (parameterType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortArgument(methodHandle, i, true);
            else if (parameterType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntArgument(methodHandle, i, true);
            else if (parameterType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongArgument(methodHandle, i, true);
            else if (parameterType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterAddressArgument(methodHandle, i, true);
            else if (parameterType == ScalarType.ADDRESS) methodHandle = MethodHandles
                    .filterArguments(methodHandle, i, FFMMethodFilters.SEGMENT_TO_INT64);
            else if (parameterType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharArgument(methodHandle, i, true);
            else if (parameterType.isCompound()) methodHandle = MethodHandles
                    .filterArguments(methodHandle, i, FFMMethodFilters.SEGMENT_TO_HANDLE);
        }
        if (returnType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortReturnValue(methodHandle, true);
        else if (returnType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntReturnValue(methodHandle, true);
        else if (returnType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongReturnValue(methodHandle, true);
        else if (returnType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterAddressReturnValue(methodHandle, true);
        else if (returnType == ScalarType.ADDRESS) methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMMethodFilters.INT64_TO_SEGMENT);
        else if (returnType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharReturnValue(methodHandle, true);
        else if (returnType != null && returnType.isCompound())
            methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMMethodFilters.HANDLE_TO_SEGMENT);
        if (Modifier.isStatic(method.getModifiers())) object = method.getDeclaringClass();
        else methodHandle = methodHandle.bindTo(object);
        long address = FFMUtil.ABIHolder.LINKER.upcallStub(methodHandle, returnType == null ?
                FunctionDescriptor.ofVoid(parameterLayouts) : FunctionDescriptor.of(returnLayout, parameterLayouts), arena,
                linkerOptions).address();
        CleanerHolder.CLEANER.register(object, arena::close);
        return address;
    }

}
