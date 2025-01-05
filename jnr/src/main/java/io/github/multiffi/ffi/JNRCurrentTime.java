package io.github.multiffi.ffi;

import jnr.ffi.Platform;
import jnr.ffi.byref.LongLongByReference;

public final class JNRCurrentTime {

    private JNRCurrentTime() {
        throw new AssertionError("No io.github.multiffi.ffi.JNACurrentTime instances for you!");
    }

    private static final class Windows {
        private Windows() {
            throw new UnsupportedOperationException();
        }
        private static final long exp7 = 10000000L;
        private static final long exp9 = 1000000000L;
        private static final long w2ux = 116444736000000000L;
        private static final JNRMappedTypes.timespec startspec = new JNRMappedTypes.timespec();
        private static double ticks2nano;
        private static long startticks, tps = 0;
        public static void clock_gettime(JNRMappedTypes.timespec timespec) {
            LongLongByReference lptmp = new LongLongByReference();
            long curticks;
            JNRLibraries.Kernel32.INSTANCE.QueryPerformanceFrequency(lptmp);
            long tmp = lptmp.getValue();
            if (tps != tmp) {
                tps = tmp;
                JNRLibraries.Kernel32.INSTANCE.QueryPerformanceCounter(lptmp);
                startticks = lptmp.getValue();
                JNRMappedTypes.FILETIME lpFileTime = new JNRMappedTypes.FILETIME();
                JNRLibraries.Kernel32.INSTANCE.GetSystemTimeAsFileTime(lpFileTime);
                long wintime = lpFileTime.dwLowDateTime.longValue() | (lpFileTime.dwHighDateTime.longValue() << 32);
                wintime -= w2ux;
                startspec.tv_sec.set(wintime / exp7);
                startspec.tv_nsec.set(wintime % exp7 * 100L);
                ticks2nano = (double) exp9 / tps;
            }
            JNRLibraries.Kernel32.INSTANCE.QueryPerformanceCounter(lptmp);
            curticks = lptmp.getValue();
            curticks -= startticks;
            timespec.tv_sec.set(startspec.tv_sec.longValue() + (curticks / tps));
            timespec.tv_nsec.set((long) (startspec.tv_nsec.longValue() + (double) (curticks % tps) * ticks2nano));
            if (!(timespec.tv_nsec.longValue() < exp9)) {
                timespec.tv_sec.set(timespec.tv_sec.longValue() + 1);
                timespec.tv_nsec.set(timespec.tv_nsec.longValue() - exp9);
            }
        }
    }

    public static long seconds() {
        JNRMappedTypes.timespec timespec = new JNRMappedTypes.timespec();
        if (JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS) Windows.clock_gettime(timespec);
        else JNRLibraries.CLibrary.INSTANCE.clock_gettime(0, timespec);
        return timespec.tv_sec.longValue();
    }

    public static int nanos() {
        JNRMappedTypes.timespec timespec = new JNRMappedTypes.timespec();
        if (JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS) Windows.clock_gettime(timespec);
        else JNRLibraries.CLibrary.INSTANCE.clock_gettime(0, timespec);
        return timespec.tv_nsec.intValue();
    }

}
