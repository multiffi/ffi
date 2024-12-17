package io.github.multiffi;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;

public final class FFMLastErrno {

    private FFMLastErrno() {
        throw new AssertionError("No io.github.multiffi.FFMLastErrno instances for you!");
    }

    private static final StructLayout CAPTURE_STATE_LAYOUT = Linker.Option.captureStateLayout();
    private static final String ERRNO_NAME = FFMUtil.IS_WINDOWS ? "GetLastError" : "errno";
    private static final VarHandle ERRNO_HANDLE =
            CAPTURE_STATE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(ERRNO_NAME));
    private static final ThreadLocal<MemorySegment> ERRNO_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> Arena.ofConfined().allocate(CAPTURE_STATE_LAYOUT));

    public static String name() {
        return ERRNO_NAME;
    }

    public static MemorySegment handle() {
        return ERRNO_THREAD_LOCAL.get();
    }

    public static int get() {
        return (int) FFMLastErrno.ERRNO_HANDLE.get(FFMLastErrno.ERRNO_THREAD_LOCAL.get(), 0L);
    }

    public static void set(int errno) {
        FFMLastErrno.ERRNO_HANDLE.set(FFMLastErrno.ERRNO_THREAD_LOCAL.get(), errno);
    }

}
