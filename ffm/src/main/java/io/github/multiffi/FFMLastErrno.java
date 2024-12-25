package io.github.multiffi;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

public final class FFMLastErrno {

    private FFMLastErrno() {
        throw new AssertionError("No io.github.multiffi.FFMLastErrno instances for you!");
    }

    private static final String ERRNO_NAME = FFMUtil.IS_WINDOWS ? "GetLastError" : "errno";
    private static final VarHandle ERRNO_HANDLE =
            Linker.Option.captureStateLayout().varHandle(MemoryLayout.PathElement.groupElement(ERRNO_NAME));
    private static final ThreadLocal<MemorySegment> ERRNO_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> Arena.ofConfined().allocate(Linker.Option.captureStateLayout()));

    public static String name() {
        return ERRNO_NAME;
    }

    public static MemorySegment segment() {
        return ERRNO_THREAD_LOCAL.get();
    }

    public static int get() {
        return (int) FFMLastErrno.ERRNO_HANDLE.get(FFMLastErrno.ERRNO_THREAD_LOCAL.get(), 0L);
    }

    public static void set(int errno) {
        FFMLastErrno.ERRNO_HANDLE.set(FFMLastErrno.ERRNO_THREAD_LOCAL.get(), errno);
    }

}
