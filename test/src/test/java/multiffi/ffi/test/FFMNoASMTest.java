package multiffi.ffi.test;

import org.junit.jupiter.api.BeforeAll;

public class FFMNoASMTest extends AbstractTest {

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("multiffi.foreign.proxyIntrinsics", "false");
        System.setProperty("multiffi.allocator.provider", "io.github.multiffi.ffi.FFMMemoryProvider");
        System.setProperty("multiffi.buffer.provider", "io.github.multiffi.ffi.FFMBufferProvider");
        System.setProperty("multiffi.foreign.provider", "io.github.multiffi.ffi.FFMForeignProvider");
    }

}
