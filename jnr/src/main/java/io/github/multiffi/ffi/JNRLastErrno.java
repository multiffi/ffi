package io.github.multiffi.ffi;

import jnr.ffi.LastError;

public final class JNRLastErrno {

    private JNRLastErrno() {
        throw new AssertionError("No io.github.multiffi.ffi.JNRLastErrno instances for you!");
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
        ERRNO_THREAD_LOCAL.set(LastError.getLastError(JNRUtil.RUNTIME));
    }

}
