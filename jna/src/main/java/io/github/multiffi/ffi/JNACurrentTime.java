package io.github.multiffi.ffi;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import com.sun.jna.ptr.LongByReference;

public final class JNACurrentTime {

    private JNACurrentTime() {
        throw new AssertionError("No io.github.multiffi.ffi.JNACurrentTime instances for you!");
    }

    private static final class Kernel32 {
        private Kernel32() {
            throw new UnsupportedOperationException();
        }
        private static final long exp7 = 10000000L;
        private static final long exp9 = 1000000000L;
        private static final long w2ux = 116444736000000000L;
        public static native int QueryPerformanceFrequency(LongByReference lpFrequency);
        public static native int QueryPerformanceCounter(LongByReference lpPerformanceCount);
        public static native void GetSystemTimeAsFileTime(FILETIME.ByReference lpSystemTimeAsFileTime);
        static {
            if (Platform.isWindows()) Native.register(Kernel32.class, Platform.isWindowsCE() ? "coredll" : "kernel32");
        }
        private static final timespec startspec = new timespec();
        private static double ticks2nano;
        private static long startticks, tps = 0;
        public static void clock_gettime(timespec timespec) {
            LongByReference lptmp = new LongByReference();
            long curticks;
            QueryPerformanceFrequency(lptmp);
            long tmp = lptmp.getValue();
            if (tps != tmp) {
                tps = tmp;
                QueryPerformanceCounter(lptmp);
                startticks = lptmp.getValue();
                FILETIME.ByReference lpFileTime = new FILETIME.ByReference();
                GetSystemTimeAsFileTime(lpFileTime);
                long wintime = ((long) lpFileTime.dwLowDateTime & 0xFFFFFFFFL) | (((long) lpFileTime.dwHighDateTime << 32) & 0xFFFFFFFF00000000L);
                wintime -= w2ux;
                startspec.tv_sec = new time_t(wintime / exp7);
                startspec.tv_nsec = new NativeLong(wintime % exp7 * 100L);
                ticks2nano = (double) exp9 / tps;
            }
            QueryPerformanceCounter(lptmp);
            curticks = lptmp.getValue();
            curticks -= startticks;
            timespec.tv_sec = new time_t(startspec.tv_sec.longValue() + (curticks / tps));
            timespec.tv_nsec = new NativeLong((long) (startspec.tv_nsec.longValue() + (double) (curticks % tps) * ticks2nano));
            if (!(timespec.tv_nsec.longValue() < exp9)) {
                timespec.tv_sec.setValue(timespec.tv_sec.longValue() + 1);
                timespec.tv_nsec.setValue(timespec.tv_nsec.longValue() - exp9);
            }
        }
    }

    private static final class CLibrary {
        private CLibrary() {
            throw new UnsupportedOperationException();
        }
        public static native int clock_gettime(int id, timespec.ByReference timespec);
        static {
            if (!Platform.isWindows()) Native.register(CLibrary.class, Platform.C_LIBRARY_NAME);
        }
    }

    public static long seconds() {
        timespec.ByReference timespec = new timespec.ByReference();
        if (Platform.isWindows()) Kernel32.clock_gettime(timespec);
        else CLibrary.clock_gettime(0, timespec);
        return timespec.tv_sec.longValue();
    }

    public static int nanos() {
        timespec.ByReference timespec = new timespec.ByReference();
        if (Platform.isWindows()) Kernel32.clock_gettime(timespec);
        else CLibrary.clock_gettime(0, timespec);
        return timespec.tv_nsec.intValue();
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
        private static final long serialVersionUID = 1438841474868317022L;

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

}
