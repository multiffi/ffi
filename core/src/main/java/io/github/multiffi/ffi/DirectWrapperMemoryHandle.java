package io.github.multiffi.ffi;

public class DirectWrapperMemoryHandle extends DirectMemoryHandle {

    public DirectWrapperMemoryHandle(long address, long size) {
        super(address, size);
    }

    @Override
    protected void free(long address) {
    }

}
