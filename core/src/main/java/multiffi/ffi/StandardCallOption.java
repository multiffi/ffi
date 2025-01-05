package multiffi.ffi;

public enum StandardCallOption implements CallOption {
    DYNCALL,
    STDCALL,
    CRITICAL,
    TRIVIAL,
    SAVE_ERRNO
}
