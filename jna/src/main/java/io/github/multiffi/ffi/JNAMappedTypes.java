package io.github.multiffi.ffi;

import com.sun.jna.CallbackProxy;
import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

public final class JNAMappedTypes {

    private JNAMappedTypes() {
        throw new AssertionError("No io.github.multiffi.ffi.JNAMappedTypes instances for you!");
    }

    public interface StdCallCallbackProxy extends StdCallLibrary.StdCallCallback, CallbackProxy {
    }

    public static class size_t extends IntegerType {

        public static final int SIZE = Native.SIZE_T_SIZE;

        public size_t() {
            this(0);
        }

        public size_t(long value) {
            super(SIZE, value);
        }

    }

    @Structure.FieldOrder({"tv_sec", "tv_nsec"})
    public static class timespec extends Structure {

        public time_t tv_sec;
        public NativeLong tv_nsec;

        public timespec() {
            super();
        }

        public static class ByReference extends timespec implements Structure.ByReference {
        }

        public static class ByValue extends timespec implements Structure.ByValue {
        }

    }

    public static class time_t extends IntegerType {

        public static final int SIZE;
        static {
            if (Platform.isAIX() || (
                    (Platform.isFreeBSD() || Platform.isNetBSD() || Platform.isOpenBSD() || Platform.isDragonFlyBSD())
                            && !(Platform.isARM() && Platform.is64Bit()))) SIZE = 4;
            else if (Platform.isWindows() && Platform.is64Bit()) SIZE = 8;
            else SIZE = Native.LONG_SIZE;
        }

        public time_t() {
            this(0);
        }

        public time_t(long value) {
            super(SIZE, value);
        }

    }

    @Structure.FieldOrder({"dwLowDateTime", "dwHighDateTime"})
    public static class FILETIME extends Structure {

        public int dwLowDateTime;
        public int dwHighDateTime;

        public FILETIME() {
            super();
        }

        public static class ByReference extends FILETIME implements Structure.ByReference {
        }

        public static class ByValue extends FILETIME implements Structure.ByValue {
        }

    }

}
