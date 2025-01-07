package jnr.ffi.provider.jffi;

import io.github.multiffi.ffi.JNRLibraryLookup;
import jnr.ffi.LibraryOption;

import java.util.Collections;
import java.util.Map;

public class JNRNativeLibrary extends NativeLibrary {

    JNRNativeLibrary(Map<LibraryOption, Object> options) {
        super(Collections.emptyList(), Collections.emptyList(), options);
        functionMap = null;
    }

    private final Map<String, Long> functionMap;
    JNRNativeLibrary(Map<String, Long> functionMap, Map<LibraryOption, Object> options) {
        super(Collections.emptyList(), Collections.emptyList(), options);
        this.functionMap = Collections.unmodifiableMap(functionMap);
    }

    @Override
    long getSymbolAddress(String name) {
        return functionMap == null ? JNRLibraryLookup.getSymbolAddress(name) : functionMap.get(name);
    }

}
