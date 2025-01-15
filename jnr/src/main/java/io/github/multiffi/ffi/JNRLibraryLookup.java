package io.github.multiffi.ffi;

import com.kenai.jffi.Library;
import jnr.ffi.LibraryLoader;
import jnr.ffi.LibraryOption;
import jnr.ffi.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
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

@SuppressWarnings({"deprecation", "removal"})
public final class JNRLibraryLookup {

    private JNRLibraryLookup() {
        throw new AssertionError("No io.github.multiffi.ffi.JNRLibraryLookup instances for you!");
    }

    public static final List<String> DEFAULT_SEARCH_PATHS;
    static {
        try {
            Class<?> clazz = Class.forName("jnr.ffi.LibraryLoader$DefaultLibPaths");
            Field field = clazz.getDeclaredField("PATHS");
            DEFAULT_SEARCH_PATHS = (List<String>) JNRUtil.UNSAFE.getObject(clazz, JNRUtil.UNSAFE.staticFieldOffset(field));
        } catch (ClassCastException | NoSuchFieldException | ClassNotFoundException e) {
            throw new IllegalStateException("Unexpected exception", e);
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
        synchronized (LIBRARIES) {
            for (Library library : LIBRARIES) {
                long address = library.getSymbolAddress(symbolName);
                if (address != 0L) return address;
            }
        }
        throw new UnsatisfiedLinkError(String.format("Failed to get symbol: `%s`", symbolName));
    }

    public static void loadLibrary(String libraryName) throws UnsatisfiedLinkError {
        Objects.requireNonNull(libraryName);
        loadLibrary(libraryName, DEFAULT_SEARCH_PATHS, Collections.emptyMap());
    }

    public static void loadLibrary(File libraryFile) throws UnsatisfiedLinkError {
        Objects.requireNonNull(libraryFile);
        loadLibrary(libraryFile.getAbsolutePath(), DEFAULT_SEARCH_PATHS, Collections.emptyMap());
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

    private static String readAll(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
