package io.github.multiffi;

import multiffi.AbstractDirectMemoryBlock;
import multiffi.Allocator;
import multiffi.MemoryBlock;

import java.util.concurrent.atomic.AtomicLong;

public class DirectMemoryBlock extends AbstractDirectMemoryBlock {

    private final AtomicLong addressRef = new AtomicLong();
    private final long size;

    public DirectMemoryBlock(long address, long size) {
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
    public MemoryBlock slice(long offset) {
        checkBounds(offset);
        return new DirectSliceMemoryBlock(this, offset, size == -1 ? -1 : size - offset);
    }

    @Override
    public MemoryBlock slice(long offset, long size) {
        checkBounds(offset, size);
        return new DirectSliceMemoryBlock(this, offset, size);
    }

    @Override
    public MemoryBlock duplicate() {
        return new DirectSliceMemoryBlock(this, 0, size);
    }

    @Override
    public MemoryBlock attachment() {
        return null;
    }

    @Override
    public void close() {
        long address = addressRef.getAndSet(0);
        if (address != 0) Allocator.free(address);
    }

}
