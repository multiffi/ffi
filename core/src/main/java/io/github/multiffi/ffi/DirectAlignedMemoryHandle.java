package io.github.multiffi.ffi;

import multiffi.ffi.Memory;

public class DirectAlignedMemoryHandle extends DirectMemoryHandle {

    public DirectAlignedMemoryHandle(long address, long size) {
        super(address, size);
    }

    @Override
    protected void free(long address) {
        Memory.freeAligned(address);
    }

}
