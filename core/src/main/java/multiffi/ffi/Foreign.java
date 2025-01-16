package multiffi.ffi;

import multiffi.ffi.spi.ForeignProvider;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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

    public static long diffSize() {
        return IMPLEMENTATION.diffSize();
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

    public static long alignSize() {
        return IMPLEMENTATION.alignSize();
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

    public static void loadLibrary(String libraryName) throws UnsatisfiedLinkError {
        IMPLEMENTATION.loadLibrary(libraryName);
    }

    public static void loadLibrary(File libraryFile) throws UnsatisfiedLinkError {
        IMPLEMENTATION.loadLibrary(libraryFile);
    }

    public static long getSymbolAddress(String symbolName) throws UnsatisfiedLinkError {
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

    public static FunctionHandle downcallHandle(long address, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.downcallHandle(address, firstVarArgIndex, returnType, parameterTypes, options);
    }

    public static FunctionHandle downcallHandle(long address, int firstVarArgIndex, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.downcallHandle(address, firstVarArgIndex, returnType, parameterTypes);
    }

    public static FunctionHandle downcallHandle(long address, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.downcallHandle(address, returnType, parameterTypes, options);
    }

    public static FunctionHandle downcallHandle(long address, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.downcallHandle(address, returnType, parameterTypes);
    }

    // sun.reflect.Reflection
    private static final Method getCallerClassReflectionMethod;
    // java.lang.StackWalker
    private static final Method getInstanceStackWalkerMethod;
    private static final Method getCallerClassStackWalkerMethod;
    private static final Object retainClassReferenceOption;
    // dalvik.system.VMStack
    private static final Method getCallingClassLoaderMethod;
    static {
        Method method;
        if ("dalvik".equalsIgnoreCase(System.getProperty("java.vm.name"))) {
            try {
                Class<?> VMStackClass = Class.forName("dalvik.system.VMStack");
                method = VMStackClass.getDeclaredMethod("getCallingClassLoader");
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                method = null;
            }
            getCallingClassLoaderMethod = method;
        }
        else getCallingClassLoaderMethod = null;
        if (getCallingClassLoaderMethod == null) {
            try {
                Class<?> reflectionClass = Class.forName("sun.reflect.Reflection");
                method = reflectionClass.getDeclaredMethod("getCallerClass");
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                method = null;
            }
            getCallerClassReflectionMethod = method;
            if (getCallerClassReflectionMethod == null) {
                Class<?> stackWalkerClass;
                Class<?> stackWalkerOptionClass;
                try {
                    stackWalkerClass = Class.forName("java.lang.StackWalker");
                    stackWalkerOptionClass = Class.forName("java.lang.StackWalker$Option");

                } catch (ClassNotFoundException e) {
                    stackWalkerClass = null;
                    stackWalkerOptionClass = null;
                }
                if (stackWalkerClass != null) {
                    Method _getInstanceStackWalkerMethod;
                    Method _getCallerClassStackWalkerMethod;
                    Object _retainClassReferenceOption;
                    try {
                        _getInstanceStackWalkerMethod = stackWalkerClass.getDeclaredMethod("getInstance", stackWalkerOptionClass);
                        _getCallerClassStackWalkerMethod = stackWalkerClass.getDeclaredMethod("getCallerClass");
                        Field field = stackWalkerOptionClass.getDeclaredField("RETAIN_CLASS_REFERENCE");
                        _retainClassReferenceOption = field.get(null);

                    } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
                        _getInstanceStackWalkerMethod = null;
                        _getCallerClassStackWalkerMethod = null;
                        _retainClassReferenceOption = null;
                    }
                    getInstanceStackWalkerMethod = _getInstanceStackWalkerMethod;
                    getCallerClassStackWalkerMethod = _getCallerClassStackWalkerMethod;
                    retainClassReferenceOption = _retainClassReferenceOption;
                }
                else {
                    getInstanceStackWalkerMethod = null;
                    getCallerClassStackWalkerMethod = null;
                    retainClassReferenceOption = null;
                }
            }
            else {
                getInstanceStackWalkerMethod = null;
                getCallerClassStackWalkerMethod = null;
                retainClassReferenceOption = null;
            }
        }
        else {
            getCallerClassReflectionMethod = null;
            getInstanceStackWalkerMethod = null;
            getCallerClassStackWalkerMethod = null;
            retainClassReferenceOption = null;
        }
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz,
                                      long address, int firstVarArgIndexIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        if (classLoader == null) {
            try {
                if (getCallingClassLoaderMethod != null) {
                    classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                    if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
                }
                else {
                    Class<?> caller;
                    if (getCallerClassReflectionMethod != null)
                        caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                    else if (getInstanceStackWalkerMethod != null) {
                        Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                        caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                    }
                    else caller = null;
                    classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, firstVarArgIndexIndex, returnType, parameterTypes, options);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz, long address, int firstVarArgIndexIndex, ForeignType returnType, ForeignType... parameterTypes) {
        if (classLoader == null) {
            try {
                if (getCallingClassLoaderMethod != null) {
                    classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                    if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
                }
                else {
                    Class<?> caller;
                    if (getCallerClassReflectionMethod != null)
                        caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                    else if (getInstanceStackWalkerMethod != null) {
                        Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                        caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                    }
                    else caller = null;
                    classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, firstVarArgIndexIndex, returnType, parameterTypes);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz,
                                      long address, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        if (classLoader == null) {
            try {
                if (getCallingClassLoaderMethod != null) {
                    classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                    if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
                }
                else {
                    Class<?> caller;
                    if (getCallerClassReflectionMethod != null)
                        caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                    else if (getInstanceStackWalkerMethod != null) {
                        Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                        caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                    }
                    else caller = null;
                    classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, returnType, parameterTypes, options);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz, long address, ForeignType returnType, ForeignType... parameterTypes) {
        if (classLoader == null) {
            try {
                if (getCallingClassLoaderMethod != null) {
                    classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                    if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
                }
                else {
                    Class<?> caller;
                    if (getCallerClassReflectionMethod != null)
                        caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                    else if (getInstanceStackWalkerMethod != null) {
                        Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                        caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                    }
                    else caller = null;
                    classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, returnType, parameterTypes);
    }

    public static <T> T downcallProxy(Class<T> clazz, long address, int firstVarArgIndexIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        ClassLoader classLoader;
        try {
            if (getCallingClassLoaderMethod != null) {
                classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
            } else {
                Class<?> caller;
                if (getCallerClassReflectionMethod != null)
                    caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                else if (getInstanceStackWalkerMethod != null) {
                    Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                    caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                } else caller = null;
                classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, firstVarArgIndexIndex, returnType, parameterTypes, options);
    }

    public static <T> T downcallProxy(Class<T> clazz, long address, int firstVarArgIndexIndex, ForeignType returnType, ForeignType... parameterTypes) {
        ClassLoader classLoader;
        try {
            if (getCallingClassLoaderMethod != null) {
                classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
            } else {
                Class<?> caller;
                if (getCallerClassReflectionMethod != null)
                    caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                else if (getInstanceStackWalkerMethod != null) {
                    Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                    caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                } else caller = null;
                classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, firstVarArgIndexIndex, returnType, parameterTypes);
    }

    public static <T> T downcallProxy(Class<T> clazz, long address, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        ClassLoader classLoader;
        try {
            if (getCallingClassLoaderMethod != null) {
                classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
            } else {
                Class<?> caller;
                if (getCallerClassReflectionMethod != null)
                    caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                else if (getInstanceStackWalkerMethod != null) {
                    Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                    caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                } else caller = null;
                classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, returnType, parameterTypes, options);
    }

    public static <T> T downcallProxy(Class<T> clazz, long address, ForeignType returnType, ForeignType... parameterTypes) {
        ClassLoader classLoader;
        try {
            if (getCallingClassLoaderMethod != null) {
                classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
            } else {
                Class<?> caller;
                if (getCallerClassReflectionMethod != null)
                    caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                else if (getInstanceStackWalkerMethod != null) {
                    Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                    caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                } else caller = null;
                classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, address, returnType, parameterTypes);
    }

    public static Object downcallProxy(ClassLoader classLoader, Class<?>[] classes, FunctionOptionVisitor functionOptionVisitor) {
        if (classLoader == null) {
            try {
                if (getCallingClassLoaderMethod != null) {
                    classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                    if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
                }
                else {
                    Class<?> caller;
                    if (getCallerClassReflectionMethod != null)
                        caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                    else if (getInstanceStackWalkerMethod != null) {
                        Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                        caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                    }
                    else caller = null;
                    classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        return IMPLEMENTATION.downcallProxy(classLoader, classes, functionOptionVisitor);
    }

    public static Object downcallProxy(Class<?>[] classes, FunctionOptionVisitor functionOptionVisitor) {
        ClassLoader classLoader;
        try {
            if (getCallingClassLoaderMethod != null) {
                classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
            } else {
                Class<?> caller;
                if (getCallerClassReflectionMethod != null)
                    caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                else if (getInstanceStackWalkerMethod != null) {
                    Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                    caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                } else caller = null;
                classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return IMPLEMENTATION.downcallProxy(classLoader, classes, functionOptionVisitor);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz, FunctionOptionVisitor functionOptionVisitor) {
        if (classLoader == null) {
            try {
                if (getCallingClassLoaderMethod != null) {
                    classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                    if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
                }
                else {
                    Class<?> caller;
                    if (getCallerClassReflectionMethod != null)
                        caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                    else if (getInstanceStackWalkerMethod != null) {
                        Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                        caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                    }
                    else caller = null;
                    classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, functionOptionVisitor);
    }

    public static <T> T downcallProxy(Class<T> clazz, FunctionOptionVisitor functionOptionVisitor) {
        ClassLoader classLoader;
        try {
            if (getCallingClassLoaderMethod != null) {
                classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
            } else {
                Class<?> caller;
                if (getCallerClassReflectionMethod != null)
                    caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                else if (getInstanceStackWalkerMethod != null) {
                    Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                    caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                } else caller = null;
                classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz, functionOptionVisitor);
    }

    public static Object downcallProxy(ClassLoader classLoader, Class<?>[] classes) {
        if (classLoader == null) {
            try {
                if (getCallingClassLoaderMethod != null) {
                    classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                    if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
                }
                else {
                    Class<?> caller;
                    if (getCallerClassReflectionMethod != null)
                        caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                    else if (getInstanceStackWalkerMethod != null) {
                        Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                        caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                    }
                    else caller = null;
                    classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        return IMPLEMENTATION.downcallProxy(classLoader, classes);
    }

    public static Object downcallProxy(Class<?>... classes) {
        ClassLoader classLoader;
        try {
            if (getCallingClassLoaderMethod != null) {
                classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
            } else {
                Class<?> caller;
                if (getCallerClassReflectionMethod != null)
                    caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                else if (getInstanceStackWalkerMethod != null) {
                    Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                    caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                } else caller = null;
                classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return IMPLEMENTATION.downcallProxy(classLoader, classes);
    }

    public static <T> T downcallProxy(ClassLoader classLoader, Class<T> clazz) {
        if (classLoader == null) {
            try {
                if (getCallingClassLoaderMethod != null) {
                    classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                    if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
                }
                else {
                    Class<?> caller;
                    if (getCallerClassReflectionMethod != null)
                        caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                    else if (getInstanceStackWalkerMethod != null) {
                        Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                        caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                    }
                    else caller = null;
                    classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz);
    }

    public static <T> T downcallProxy(Class<T> clazz) {
        ClassLoader classLoader;
        try {
            if (getCallingClassLoaderMethod != null) {
                classLoader = (ClassLoader) getCallingClassLoaderMethod.invoke(null);
                if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
            } else {
                Class<?> caller;
                if (getCallerClassReflectionMethod != null)
                    caller = (Class<?>) getCallerClassReflectionMethod.invoke(null);
                else if (getInstanceStackWalkerMethod != null) {
                    Object stackWalker = getInstanceStackWalkerMethod.invoke(null, retainClassReferenceOption);
                    caller = (Class<?>) getCallerClassStackWalkerMethod.invoke(stackWalker);
                } else caller = null;
                classLoader = caller == null ? ClassLoader.getSystemClassLoader() : caller.getClassLoader();
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return IMPLEMENTATION.downcallProxy(classLoader, clazz);
    }

    public static MemoryHandle upcallStub(Object object, Method method, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.upcallStub(object, method, firstVarArgIndex, returnType, parameterTypes, options);
    }

    public static MemoryHandle upcallStub(Object object, Method method, int firstVarArgIndex, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.upcallStub(object, method, firstVarArgIndex, returnType, parameterTypes);
    }

    public static MemoryHandle upcallStub(Object object, Method method, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        return IMPLEMENTATION.upcallStub(object, method, returnType, parameterTypes, options);
    }

    public static MemoryHandle upcallStub(Object object, Method method, ForeignType returnType, ForeignType... parameterTypes) {
        return IMPLEMENTATION.upcallStub(object, method, returnType, parameterTypes);
    }

    public static Runnable registerCleaner(Object object, Runnable cleanup) {
        return IMPLEMENTATION.registerCleaner(object, cleanup);
    }

}
