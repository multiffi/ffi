package io.github.multiffi;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public final class JNRMappedTypes {

    private JNRMappedTypes() {
        throw new AssertionError("No io.github.multiffi.JNRMappedTypes instances for you!");
    }

    public static class timespec extends Struct {

        public final time_t tv_sec = new time_t();
        public final SignedLong tv_nsec = new SignedLong();

        public timespec() {
            super(JNRUtil.UnsafeHolder.RUNTIME);
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

    public static class FILETIME extends Struct {

        public final DWORD dwLowDateTime = new DWORD();
        public final DWORD dwHighDateTime = new DWORD();

        public FILETIME() {
            super(JNRUtil.UnsafeHolder.RUNTIME);
        }

        public FILETIME(Runtime runtime) {
            super(runtime);
        }

        public FILETIME(Runtime runtime, Alignment alignment) {
            super(runtime, alignment);
        }

        public FILETIME(Runtime runtime, Struct enclosing) {
            super(runtime, enclosing);
        }

    }

}
