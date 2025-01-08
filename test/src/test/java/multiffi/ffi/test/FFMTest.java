package multiffi.ffi.test;

import org.junit.jupiter.api.BeforeAll;

public class FFMTest extends AbstractTest {

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("multiffi.allocator.provider", "io.github.multiffi.ffi.FFMAllocatorProvider");
        System.setProperty("multiffi.buffer.provider", "io.github.multiffi.ffi.FFMBufferProvider");
        System.setProperty("multiffi.foreign.provider", "io.github.multiffi.ffi.FFMForeignProvider");
    }

}
