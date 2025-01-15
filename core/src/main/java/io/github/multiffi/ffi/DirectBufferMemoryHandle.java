package io.github.multiffi.ffi;

import multiffi.ffi.Buffers;

import java.nio.Buffer;
import java.util.concurrent.atomic.AtomicReference;

public class DirectBufferMemoryHandle extends DirectMemoryHandle {

    private final AtomicReference<Buffer> bufferRef = new AtomicReference<>();

    public DirectBufferMemoryHandle(Buffer buffer, long size) {
        super(Buffers.address(buffer), size);
        if (!buffer.isDirect()) throw new IllegalArgumentException("Not a direct buffer");
        bufferRef.set(buffer);
    }

    @Override
    protected void free(long address) {
        Buffers.clean(bufferRef.getAndSet(null));
    }

}
