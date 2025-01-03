package io.github.multiffi;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.ptr.LongByReference;

public final class JNACurrentTime {

    private JNACurrentTime() {
        throw new AssertionError("No io.github.multiffi.JNACurrentTime instances for you!");
    }

    private static final class Kernel32 {
        private static final long exp7 = 10000000L;
        private static final long exp9 = 1000000000L;
        private static final long w2ux = 116444736000000000L;
        public static native int QueryPerformanceFrequency(LongByReference lpFrequency);
        public static native int QueryPerformanceCounter(LongByReference lpPerformanceCount);
        public static native void GetSystemTimeAsFileTime(FILETIME.ByReference lpSystemTimeAsFileTime);
        static {
            if (Platform.isWindows()) Native.register(Kernel32.class, "kernel32.dll");
        }
        private static long trunc(long value) {
            return ((int) value) & 0xFFFFFFFFL;
        }
        private static final Timespec startspec = new Timespec();
        private static double ticks2nano;
        private static long startticks, tps = 0;
        public static void clock_gettime(Timespec timespec) {
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
                startspec.tv_sec = new NativeLong(wintime / exp7);
                startspec.tv_nsec = new NativeLong(wintime % exp7 * 100L);
                ticks2nano = (double) exp9 / tps;
            }
            QueryPerformanceCounter(lptmp);
            curticks = lptmp.getValue();
            curticks -= startticks;
            timespec.tv_sec = new NativeLong(trunc(startspec.tv_sec.longValue() + (curticks / tps)));
            timespec.tv_nsec = new NativeLong(trunc((long) (startspec.tv_nsec.longValue() + (double) (curticks % tps) * ticks2nano)));
            if (!(timespec.tv_nsec.longValue() < exp9)) {
                timespec.tv_sec.setValue(trunc(timespec.tv_sec.longValue() + 1));
                timespec.tv_nsec.setValue(trunc(timespec.tv_nsec.longValue() - exp9));
            }
        }
    }

    private static final class CLibrary {
        public static native int clock_gettime(int id, Timespec.ByReference timespec);
        static {
            if (!Platform.isWindows()) Native.register(CLibrary.class, Platform.C_LIBRARY_NAME);
        }
    }

    public static long seconds() {
        Timespec.ByReference timespec = new Timespec.ByReference();
        if (Platform.isWindows()) Kernel32.clock_gettime(timespec);
        else CLibrary.clock_gettime(0, timespec);
        return timespec.tv_sec.longValue();
    }

    public static int nanos() {
        Timespec.ByReference timespec = new Timespec.ByReference();
        if (Platform.isWindows()) Kernel32.clock_gettime(timespec);
        else CLibrary.clock_gettime(0, timespec);
        return timespec.tv_nsec.intValue();
    }

}
