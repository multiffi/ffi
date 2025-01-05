package multiffi.ffi;

public class UnsatisfiedLinkException extends RuntimeException {

    private static final long serialVersionUID = 1349068805325705935L;

    public UnsatisfiedLinkException() {
        super();
    }

    public UnsatisfiedLinkException(String message) {
        super(message);
    }

}
