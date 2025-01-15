package jnr.ffi.provider.jffi;

import jnr.ffi.LibraryOption;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class JNRNativeLibraryLoader<T> extends jnr.ffi.LibraryLoader<T> {

    private final Class<T> interfaceClass;
    public JNRNativeLibraryLoader(Class<T> interfaceClass) {
        super(interfaceClass);
        this.interfaceClass = Objects.requireNonNull(interfaceClass);
    }

    public T loadLibrary(Class<T> interfaceClass, Collection<String> libraryNames, Collection<String> searchPaths,
                         Map<LibraryOption, Object> options, boolean failImmediately) {
        NativeLibrary nativeLibrary = new JNRNativeLibrary(options);

        try {
            return NativeLibraryLoader.ASM_ENABLED
                ? new AsmLibraryLoader().loadLibrary(nativeLibrary, interfaceClass, options, failImmediately)
                : new ReflectionLibraryLoader().loadLibrary(nativeLibrary, interfaceClass, options, failImmediately);

        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public T loadLibrary(Map<String, Long> functionMap, Map<LibraryOption, Object> options, boolean failImmediately) {
        NativeLibrary nativeLibrary = new JNRNativeLibrary(functionMap, options);

        try {
            return NativeLibraryLoader.ASM_ENABLED
                ? new AsmLibraryLoader().loadLibrary(nativeLibrary, interfaceClass, options, failImmediately)
                : new ReflectionLibraryLoader().loadLibrary(nativeLibrary, interfaceClass, options, failImmediately);

        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
