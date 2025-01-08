package multiffi.ffi.test;

import org.junit.jupiter.api.BeforeAll;

public class JNRTest extends AbstractTest {

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("multiffi.allocator.provider", "io.github.multiffi.ffi.JNRAllocatorProvider");
        System.setProperty("multiffi.buffer.provider", "io.github.multiffi.ffi.JNRBufferProvider");
        System.setProperty("multiffi.foreign.provider", "io.github.multiffi.ffi.JNRForeignProvider");
    }

}
