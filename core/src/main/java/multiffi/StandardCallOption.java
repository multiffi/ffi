package multiffi;

public enum StandardCallOption implements CallOption {
    STDCALL,
    CRITICAL,
    TRIVIAL,
    SAVE_ERRNO
}
