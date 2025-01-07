package io.github.multiffi.ffi;

import com.sun.jna.Native;

public final class JNALastErrno {

    private JNALastErrno() {
        throw new AssertionError("No io.github.multiffi.ffi.JNALastErrno instances for you!");
    }

    private static final ThreadLocal<Integer> ERRNO_THREAD_LOCAL = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    public static int get() {
        return ERRNO_THREAD_LOCAL.get();
    }

    public static void set(int errno) {
        ERRNO_THREAD_LOCAL.set(errno);
    }

    public static void dump() {
        ERRNO_THREAD_LOCAL.set(Native.getLastError());
    }

}
