package io.github.multiffi.ffi;

import multiffi.ffi.AbstractDirectMemoryHandle;
import multiffi.ffi.Memory;
import multiffi.ffi.MemoryHandle;

import java.util.concurrent.atomic.AtomicLong;

public class DirectMemoryHandle extends AbstractDirectMemoryHandle {

    private final AtomicLong addressRef = new AtomicLong();
    private final long size;

    public DirectMemoryHandle(long address, long size) {
        this.addressRef.set(address);
        this.size = size;
    }

    @Override
    public long address() {
        return addressRef.get();
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public MemoryHandle slice(long offset) {
        checkBounds(offset);
        return new DirectSliceMemoryHandle(this, offset, size == -1 ? -1 : size - offset);
    }

    @Override
    public MemoryHandle slice(long offset, long size) {
        checkBounds(offset, size);
        return new DirectSliceMemoryHandle(this, offset, size);
    }

    @Override
    public MemoryHandle duplicate() {
        return new DirectSliceMemoryHandle(this, 0, size);
    }

    @Override
    public MemoryHandle attachment() {
        return null;
    }

    protected void free(long address) {
        Memory.free(address);
    }

    @Override
    public void close() {
        long address = addressRef.getAndSet(0);
        if (address != 0) free(address);
    }

}
