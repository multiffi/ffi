package io.github.multiffi.ffi;

import com.kenai.jffi.Library;
import jnr.ffi.LibraryLoader;
import jnr.ffi.LibraryOption;
import jnr.ffi.Platform;
import multiffi.ffi.UnsatisfiedLinkException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JNRLibraryLookup {

    private JNRLibraryLookup() {
        throw new AssertionError("No io.github.multiffi.ffi.JNRLibraryLookup instances for you!");
    }

    private static final class GetCallerClassHolder {
        private GetCallerClassHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Method getCallerClassReflectionMethod;
        public static final Method getInstanceStackWalkerMethod;
        public static final Method getCallerClassStackWalkerMethod;
        public static final Object retainClassReferenceOption;
        static {
            Method method;
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
    }

    private static final class FindNativeMethodHolder {
        private FindNativeMethodHolder() {
            throw new UnsupportedOperationException();
        }
        public static final Method findNativeMethod;
        static {
            Method method;
            try {
                method = ClassLoader.class.getDeclaredMethod("findNative", ClassLoader.class, String.class);
            } catch (NoSuchMethodException e) {
                method = null;
            }
            findNativeMethod = method;
        }
    }

    @SuppressWarnings("unchecked")
    public static final class DefaultLibraryPathHolder {
        private DefaultLibraryPathHolder() {
            throw new UnsupportedOperationException();
        }
        public static final List<String> DEFAULT_SEARCH_PATHS;
        static {
            try {
                Class<?> clazz = Class.forName("jnr.ffi.LibraryLoader$DefaultLibPaths");
                Field field = clazz.getDeclaredField("PATHS");
                DEFAULT_SEARCH_PATHS = (List<String>) JNRUtil.UnsafeHolder.UNSAFE.getObject(clazz, JNRUtil.UnsafeHolder.UNSAFE.staticFieldOffset(field));
            } catch (ClassCastException | NoSuchFieldException | ClassNotFoundException e) {
                throw new IllegalStateException("Unexpected exception");
            }
        }
    }

    private static final Set<Library> LIBRARIES = new HashSet<>();
    static {
        LIBRARIES.add(Library.getDefault());
        Platform platform = Platform.getNativePlatform();
        if (platform.getOS() != Platform.OS.LINUX)
            LIBRARIES.add(Library.getCachedInstance((platform.getOS() == Platform.OS.WINDOWS && platform.getOSName().startsWith("Windows CE"))
                    ? "coredll.dll" : platform.getStandardCLibraryName(), Library.GLOBAL | Library.LAZY));
        if (platform.getOS() != Platform.OS.LINUX && platform.getOS() != Platform.OS.WINDOWS
                && platform.getOS() != Platform.OS.AIX && platform.getOS() != Platform.OS.IBMI)
            LIBRARIES.add(Library.getCachedInstance(platform.mapLibraryName("m"), Library.GLOBAL | Library.LAZY));
    }

    public static long getSymbolAddress(String symbolName) {
        Objects.requireNonNull(symbolName);
        long address;
        if (FindNativeMethodHolder.findNativeMethod != null) {
            Class<?> clazz;
            try {
                if (JNRLibraryLookup.GetCallerClassHolder.getCallerClassReflectionMethod != null)
                    clazz = (Class<?>) JNRLibraryLookup.GetCallerClassHolder.getCallerClassReflectionMethod.invoke(null);
                else if (JNRLibraryLookup.GetCallerClassHolder.getInstanceStackWalkerMethod != null) {
                    Object stackWalker = JNRLibraryLookup.GetCallerClassHolder.getInstanceStackWalkerMethod.invoke(
                            null, JNRLibraryLookup.GetCallerClassHolder.retainClassReferenceOption
                    );
                    clazz = (Class<?>) JNRLibraryLookup.GetCallerClassHolder.getCallerClassStackWalkerMethod.invoke(stackWalker);
                }
                else clazz = null;
            } catch (InvocationTargetException | IllegalAccessException e) {
                clazz = null;
            }
            ClassLoader classLoader = clazz == null ? ClassLoader.getSystemClassLoader() : clazz.getClassLoader();
            try {
                address = (long) JNRUtil.invoke(classLoader, FindNativeMethodHolder.findNativeMethod, symbolName);
            } catch (Throwable e) {
                address = 0L;
            }
            if (address != 0L) return address;
        }
        synchronized (LIBRARIES) {
            for (Library library : LIBRARIES) {
                address = library.getSymbolAddress(symbolName);
                if (address != 0L) return address;
            }
        }
        throw new UnsatisfiedLinkException(String.format("Failed to get symbol: `%s`", symbolName));
    }

    public static void loadLibrary(String libraryName, Collection<String> searchPaths, Map<LibraryOption, Object> options) {
        List<String> paths = Collections.unmodifiableList(new ArrayList<>(searchPaths));
        if (libraryName == null || libraryName.equals(LibraryLoader.DEFAULT_LIBRARY)) return;
        synchronized (LIBRARIES) {
            // try opening ignoring search paths AND any name mapping, so just literal given name
            Library library = openLibrary(libraryName);
            if (library == null) {
                String path = Platform.getNativePlatform().locateLibrary(libraryName, paths, options); // try opening with mapping and search paths
                if (!libraryName.equals(path)) {
                    library = openLibrary(path);
                }
            }
            if (library == null) {
                throw new UnsatisfiedLinkError(Library.getLastError() +
                        "\nLibrary name:\n" + libraryName +
                        "\nSearch paths:\n" + paths);
            }
            LIBRARIES.add(library);
        }
    }

    private static final Pattern BAD_ELF = Pattern.compile("(.*): (invalid ELF header|file too short|invalid file format)");
    private static final Pattern ELF_GROUP = Pattern.compile("GROUP\\s*\\(\\s*(\\S*).*\\)");

    private static Library openLibrary(String path) {

        Library lib = Library.getCachedInstance(path, Library.LAZY | Library.GLOBAL);
        if (lib != null) return lib;

        // If dlopen() fails with 'invalid ELF header', then it is likely to be a ld script - parse it for the real library path
        Matcher badElf = BAD_ELF.matcher(Library.getLastError());
        if (badElf.lookingAt()) {
            File f = new File(badElf.group(1));
            if (f.isFile() && f.length() < (4 * 1024)) {
                Matcher sharedObject = ELF_GROUP.matcher(readAll(f));
                if (sharedObject.find()) {
                    return Library.getCachedInstance(sharedObject.group(1), Library.LAZY | Library.GLOBAL);
                }
            }
        }

        return null;
    }

    private static String readAll(File f) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

}
