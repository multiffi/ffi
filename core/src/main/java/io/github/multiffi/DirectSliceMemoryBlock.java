package io.github.multiffi;

import multiffi.AbstractDirectMemoryBlock;
import multiffi.MemoryBlock;

import java.util.concurrent.atomic.AtomicReference;

public class DirectSliceMemoryBlock extends AbstractDirectMemoryBlock {

    private final AtomicReference<DirectMemoryBlock> attachmentRef = new AtomicReference<>();
    private final long offset;
    private final long size;

    protected DirectSliceMemoryBlock(DirectMemoryBlock attachment, long offset, long size) {
        this.attachmentRef.set(attachment);
        attachment.checkBounds(offset);
        this.offset = offset;
        this.size = size;
    }

    @Override
    public long address() {
        if (attachmentRef.compareAndSet(null, null)) return 0;
        else return attachmentRef.get().address() + offset;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public MemoryBlock slice(long offset) {
        checkBounds(offset);
        return new DirectSliceMemoryBlock(attachmentRef.get(), this.offset + offset, size == -1 ? -1 : size - offset);
    }

    @Override
    public MemoryBlock slice(long offset, long size) {
        checkBounds(offset, size);
        return new DirectSliceMemoryBlock(attachmentRef.get(), this.offset + offset, size);
    }

    @Override
    public MemoryBlock duplicate() {
        return new DirectSliceMemoryBlock(attachmentRef.get(), offset, size);
    }

    @Override
    public MemoryBlock attachment() {
        return attachmentRef.get();
    }

    @Override
    public void close() {
        attachmentRef.set(null);
    }

}
