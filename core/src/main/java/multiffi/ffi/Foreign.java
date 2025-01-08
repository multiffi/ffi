package multiffi.ffi;

import multiffi.ffi.spi.ForeignProvider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Map;

public final class Foreign {

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_FAILURE = 1;

    public static final long NULL = 0L;
    public static final int EOF = -1;

    private Foreign() {
        throw new AssertionError("No multiffi.ffi.Foreign instances for you!");
    }

    private static final ForeignProvider IMPLEMENTATION = ForeignProvider.getImplementation();

    public static long addressSize() {
        return IMPLEMENTATION.addressSize();
    }

    public static long shortSize() {
        return IMPLEMENTATION.shortSize();
    }

    public static long intSize() {
        return IMPLEMENTATION.intSize();
    }

    public static long longSize() {
        return IMPLEMENTATION.longSize();
    }

    public static long wcharSize() {
        return IMPLEMENTATION.wcharSize();
    }

    public static long pageSize() {
        return IMPLEMENTATION.pageSize();
    }

    public static Charset ansiCharset() {
        return IMPLEMENTATION.ansiCharset();
    }

    public static Charset wideCharset() {
        return IMPLEMENTATION.wideCharset();
    }

    public static Charset utf16Charset() {
        return IMPLEMENTATION.utf16Charset();
    }

    public static Charset utf32Charset() {
        return IMPLEMENTATION.utf32Charset();
    }

    public static void sneakyThrows(Throwable throwable) {
        IMPLEMENTATION.sneakyThrows(throwable);
    }

    public static void exit(int status) {
        IMPLEMENTATION.exit(status);
    }

    public static void halt(int status) {
        IMPLEMENTATION.halt(status);
    }

    public static long currentTimeMillis() {
        return IMPLEMENTATION.currentTimeMillis();
    }

    public static long currentTimeMonotonic() {
        return IMPLEMENTATION.currentTimeMonotonic();
    }

    public static long currentTimeSeconds() {
        return IMPLEMENTATION.currentTimeSeconds();
    }

    public static int currentTimeNanos() {
        return IMPLEMENTATION.currentTimeNanos();
    }

    public static Map<String, String> environ() {
        return IMPLEMENTATION.environ();
    }

    public static String getEnviron(String key) {
        return IMPLEMENTATION.getEnviron(key);
    }

    public static String getEnviron(String key, String defaultValue) {
        return IMPLEMENTATION.getEnviron(key, defaultValue);
    }

    public static String getStackTraceString(Throwable throwable) {
        return IMPLEMENTATION.getStackTraceString(throwable);
    }

    public static boolean isBigEndian() {
        return IMPLEMENTATION.isBigEndian();
    }

    public static boolean isLittleEndian() {
        return IMPLEMENTATION.isLittleEndian();
    }

    public static ByteOrder endianness() {
        return IMPLEMENTATION.endianness();
    }

    public static void loadLibrary(String libraryName) throws IOException {
        IMPLEMENTATION.loadLibrary(libraryName);
    }

    public static void loadLibrary(File libraryFile) throws IOException {
        IMPLEMENTATION.loadLibrary(libraryFile);
    }

    public static long getSymbolAddress(String symbolName) {
        return IMPLEMENTATION.getSymbolAddress(symbolName);
    }

    public static String mapLibraryName(String libraryName) {
        return IMPLEMENTATION.mapLibraryName(libraryName);
    }

    public static int getLastErrno() {
        return IMPLEMENTATION.getLastErrno();
    }

    public static void setLastErrno(int errno) {
        IMPLEMENTATION.setLastErrno(errno);
    }

    public static String getErrorString(int errno) {
        return IMPLEMENTATION.getErrorString(errno);
    }

    public static String getLastErrorString() {
        return IMPLEMENTATION.getLastErrorString();
    }

    public static FunctionHandle downcallHandle(long address, int firstVarArg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.downcallHandle(address, firstVarArg, returnType, parameterTypes, options);
    }

    public static FunctionHandle downcallHandle(long address, int firstVarArg, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.downcallHandle(address, firstVarArg, returnType, parameterTypes);
    }

    public static FunctionHandle downcallHandle(long address, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.downcallHandle(address, returnType, parameterTypes, options);
    }

    public static FunctionHandle downcallHandle(long address, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.downcallHandle(address, returnType, parameterTypes);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz,
                                      long address, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, firstVararg, returnType, parameterTypes, options);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz, long address, int firstVararg, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, firstVararg, returnType, parameterTypes);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz,
                                      long address, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, returnType, parameterTypes, options);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz, long address, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, returnType, parameterTypes);
    }

    public static <T> T downcallProxy(Class<T> clazz, long address, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.downcallProxy(clazz, address, firstVararg, returnType, parameterTypes, options);
    }

    public static <T> T downcallProxy(Class<T> clazz, long address, int firstVararg, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.downcallProxy(clazz, address, firstVararg, returnType, parameterTypes);
    }

    public static <T> T downcallProxy(Class<T> clazz, long address, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.downcallProxy(clazz, address, returnType, parameterTypes, options);
    }

    public static <T> T downcallProxy(Class<T> clazz, long address, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.downcallProxy(clazz, address, returnType, parameterTypes);
    }

    public static Object downcallProxy(ClassLoader classLoader, Class<?>[] classes, CallOptionVisitor callOptionVisitor) {
        return IMPLEMENTATION.downcallProxy(classLoader, classes, callOptionVisitor);
    }

    public static Object downcallProxy(Class<?>[] classes, CallOptionVisitor callOptionVisitor) {
        return IMPLEMENTATION.downcallProxy(classes, callOptionVisitor);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz, CallOptionVisitor callOptionVisitor) {
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, callOptionVisitor);
    }

    public static <T> T downcallProxy(Class<T> clazz, CallOptionVisitor callOptionVisitor) {
        return IMPLEMENTATION.downcallProxy(clazz, callOptionVisitor);
    }

    public static Object downcallProxy(ClassLoader classLoader, Class<?>[] classes) {
        return IMPLEMENTATION.downcallProxy(classLoader, classes);
    }

    public static Object downcallProxy(Class<?>[] classes) {
        return IMPLEMENTATION.downcallProxy(classes);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz) {
        return IMPLEMENTATION.downcallProxy(classLoader, clazz);
    }

    public static <T> T downcallProxy(Class<T> clazz) {
        return IMPLEMENTATION.downcallProxy(clazz);
    }

    public static long upcallStub(Object object, Method method, int firstVarArg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.upcallStub(object, method, firstVarArg, returnType, parameterTypes, options);
    }

    public static long upcallStub(Object object, Method method, int firstVarArg, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.upcallStub(object, method, firstVarArg, returnType, parameterTypes);
    }

    public static long upcallStub(Object object, Method method, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.upcallStub(object, method, returnType, parameterTypes, options);
    }

    public static long upcallStub(Object object, Method method, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.upcallStub(object, method, returnType, parameterTypes);
    }

}
