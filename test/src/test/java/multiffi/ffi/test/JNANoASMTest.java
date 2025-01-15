package multiffi.ffi.test;

import org.junit.jupiter.api.BeforeAll;

public class JNANoASMTest extends AbstractTest {

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("multiffi.foreign.proxyIntrinsics", "false");
        System.setProperty("multiffi.allocator.provider", "io.github.multiffi.ffi.JNAAllocatorProvider");
        System.setProperty("multiffi.buffer.provider", "io.github.multiffi.ffi.JNABufferProvider");
        System.setProperty("multiffi.foreign.provider", "io.github.multiffi.ffi.JNAForeignProvider");
    }

}
