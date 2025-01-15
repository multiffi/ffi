package io.github.multiffi.ffi;

import multiffi.ffi.AbstractHeapMemoryHandle;
import multiffi.ffi.MemoryHandle;

import java.util.concurrent.atomic.AtomicReference;

public class HeapMemoryHandle extends AbstractHeapMemoryHandle {

    private final AtomicReference<Object> arrayRef = new AtomicReference<>();
    private final long arrayOffset;
    private final long arrayLength;
    private final long size;

    public HeapMemoryHandle(Object array, long offset, long size) {
        if (array == null) this.arrayLength = 0;
        else {
            this.arrayLength = Util.getArrayContentSize(array);
            this.arrayRef.set(array);
            long index = Util.unsignedAddExact(offset, size);
            if (index < 0 || index > arrayLength)
                throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        }
        this.arrayOffset = offset;
        this.size = size;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public Object array() {
        return arrayRef.get();
    }

    @Override
    public long arrayOffset() {
        return arrayOffset;
    }

    @Override
    public long arrayLength() {
        return arrayLength;
    }

    @Override
    public MemoryHandle slice(long offset) {
        checkBounds(arrayOffset, offset);
        return new HeapMemoryHandle(array(), arrayOffset + offset, size - offset);
    }

    @Override
    public MemoryHandle slice(long offset, long size) {
        checkBounds(arrayOffset, offset);
        long index = arrayOffset + offset;
        checkBounds(index, size);
        return new HeapMemoryHandle(array(), index, size);
    }

    @Override
    public MemoryHandle duplicate() {
        return new HeapMemoryHandle(array(), arrayOffset, size);
    }

    @Override
    public MemoryHandle attachment() {
        return null;
    }

    @Override
    public boolean isNil() {
        return size == 0 || array() == null;
    }

    @Override
    public void close() {
        arrayRef.set(null);
    }

}
