package io.github.multiffi.ffi;

import multiffi.ffi.AbstractDirectMemoryHandle;
import multiffi.ffi.MemoryHandle;

import java.util.concurrent.atomic.AtomicReference;

public class DirectSliceMemoryHandle extends AbstractDirectMemoryHandle {

    private final AtomicReference<DirectMemoryHandle> attachmentRef = new AtomicReference<>();
    private final long offset;
    private final long size;

    public DirectSliceMemoryHandle(DirectMemoryHandle attachment, long offset, long size) {
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
    public MemoryHandle slice(long offset) {
        checkBounds(offset);
        return new DirectSliceMemoryHandle(attachmentRef.get(), this.offset + offset, size == -1 ? -1 : size - offset);
    }

    @Override
    public MemoryHandle slice(long offset, long size) {
        checkBounds(offset, size);
        return new DirectSliceMemoryHandle(attachmentRef.get(), this.offset + offset, size);
    }

    @Override
    public MemoryHandle duplicate() {
        return new DirectSliceMemoryHandle(attachmentRef.get(), offset, size);
    }

    @Override
    public MemoryHandle attachment() {
        return attachmentRef.get();
    }

    @Override
    public void close() {
        attachmentRef.set(null);
    }

}
