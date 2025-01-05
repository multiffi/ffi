package multiffi.ffi;

public class ErrnoException extends RuntimeException {

    private static final long serialVersionUID = 1370693673758408886L;

    private final int errno;

    public ErrnoException(int errno, String message) {
        super(message);
        this.errno = errno;
    }

    public ErrnoException(int errno) {
        super(Foreign.getErrorString(errno));
        this.errno = errno;
    }

    public ErrnoException() {
        this(Foreign.getLastErrno());
    }

    public int getErrno() {
        return errno;
    }

}
