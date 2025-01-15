package io.github.multiffi.ffi;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Platform;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.annotations.IgnoreError;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.LongLongByReference;

public final class JNRCurrentTime {

    private JNRCurrentTime() {
        throw new AssertionError("No io.github.multiffi.ffi.JNRCurrentTime instances for you!");
    }

    public interface Kernel32 {
        Kernel32 INSTANCE = JNRUtil.PLATFORM.getOS() != Platform.OS.WINDOWS ? null :
                LibraryLoader.create(Kernel32.class).load(JNRUtil.PLATFORM.getName().startsWith("Windows CE") ? "coredll" : "kernel32");
        @IgnoreError
        int QueryPerformanceFrequency(@Out LongLongByReference lpFrequency);
        @IgnoreError
        int QueryPerformanceCounter(@Out LongLongByReference lpPerformanceCount);
        @IgnoreError
        void GetSystemTimeAsFileTime(@Out FILETIME lpSystemTimeAsFileTime);
    }

    public interface CLibrary {
        CLibrary INSTANCE = JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS ? null :
                LibraryLoader.create(CLibrary.class).load(JNRUtil.PLATFORM.getStandardCLibraryName());
        @IgnoreError
        int clock_gettime(int id, @Out timespec timespec);
    }

    private static final class Windows {
        private Windows() {
            throw new UnsupportedOperationException();
        }
        private static final long exp7 = 10000000L;
        private static final long exp9 = 1000000000L;
        private static final long w2ux = 116444736000000000L;
        private static final timespec startspec = new timespec();
        private static double ticks2nano;
        private static long startticks, tps = 0;
        public static void clock_gettime(timespec timespec) {
            LongLongByReference lptmp = new LongLongByReference();
            long curticks;
            Kernel32.INSTANCE.QueryPerformanceFrequency(lptmp);
            long tmp = lptmp.getValue();
            if (tps != tmp) {
                tps = tmp;
                Kernel32.INSTANCE.QueryPerformanceCounter(lptmp);
                startticks = lptmp.getValue();
                FILETIME lpFileTime = new FILETIME();
                Kernel32.INSTANCE.GetSystemTimeAsFileTime(lpFileTime);
                long wintime = lpFileTime.dwLowDateTime.longValue() | (lpFileTime.dwHighDateTime.longValue() << 32);
                wintime -= w2ux;
                startspec.tv_sec.set(wintime / exp7);
                startspec.tv_nsec.set(wintime % exp7 * 100L);
                ticks2nano = (double) exp9 / tps;
            }
            Kernel32.INSTANCE.QueryPerformanceCounter(lptmp);
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
        timespec timespec = new timespec();
        if (JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS) Windows.clock_gettime(timespec);
        else CLibrary.INSTANCE.clock_gettime(0, timespec);
        return timespec.tv_sec.longValue();
    }

    public static int nanos() {
        timespec timespec = new timespec();
        if (JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS) Windows.clock_gettime(timespec);
        else CLibrary.INSTANCE.clock_gettime(0, timespec);
        return timespec.tv_nsec.intValue();
    }

    public static class FILETIME extends Struct {
        public final DWORD dwLowDateTime = new DWORD();
        public final DWORD dwHighDateTime = new DWORD();
        public FILETIME() {
            super(JNRUtil.RUNTIME);
        }
        public FILETIME(jnr.ffi.Runtime runtime) {
            super(runtime);
        }
        public FILETIME(jnr.ffi.Runtime runtime, Alignment alignment) {
            super(runtime, alignment);
        }
        public FILETIME(Runtime runtime, Struct enclosing) {
            super(runtime, enclosing);
        }
    }

    public static class timespec extends Struct {
        public final time_t tv_sec = new time_t();
        public final SignedLong tv_nsec = new SignedLong();
        public timespec() {
            super(JNRUtil.RUNTIME);
        }
        public timespec(Runtime runtime) {
            super(runtime);
        }
        public timespec(Runtime runtime, Alignment alignment) {
            super(runtime, alignment);
        }
        public timespec(Runtime runtime, Struct enclosing) {
            super(runtime, enclosing);
        }
    }

}
