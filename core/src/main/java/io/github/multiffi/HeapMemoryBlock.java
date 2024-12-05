package io.github.multiffi;

import multiffi.AbstractHeapMemoryBlock;
import multiffi.MemoryBlock;

import java.util.concurrent.atomic.AtomicReference;

public class HeapMemoryBlock extends AbstractHeapMemoryBlock {

    private final AtomicReference<Object> arrayRef = new AtomicReference<>();
    private final long arrayOffset;
    private final long arrayLength;
    private final long size;

    public HeapMemoryBlock(Object array, long offset, long size) {
        if (array == null) this.arrayLength = 0;
        else {
            this.arrayLength = getArrayContentSize(array);
            this.arrayRef.set(array);
            long index = unsignedAddExact(offset, size);
            if (index < 0 || index > arrayLength)
                throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        }
        this.arrayOffset = offset;
        this.size = size;
    }

    private static long getArrayContentSize(Object array) {
        if (array == null || !array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())
            throw new IllegalArgumentException("not a primitive array");
        else if (array.getClass().getComponentType() == boolean.class) throw new IllegalArgumentException("boolean array not supported");
        else if (array instanceof byte[]) return ((byte[]) array).length;
        else if (array instanceof char[]) return (long) ((char[]) array).length << 1;
        else if (array instanceof short[]) return (long) ((short[]) array).length << 1;
        else if (array instanceof int[]) return (long) ((int[]) array).length << 2;
        else if (array instanceof long[]) return (long) ((long[]) array).length << 3;
        else if (array instanceof float[]) return (long) ((float[]) array).length << 2;
        else if (array instanceof double[]) return (long) ((double[]) array).length << 3;
        else throw new IllegalStateException("Unexpected exception");
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
    public MemoryBlock slice(long offset) {
        checkBounds(arrayOffset, offset);
        return new HeapMemoryBlock(array(), arrayOffset + offset, size - offset);
    }

    @Override
    public MemoryBlock slice(long offset, long size) {
        checkBounds(arrayOffset, offset);
        long index = arrayOffset + offset;
        checkBounds(index, size);
        return new HeapMemoryBlock(array(), index, size);
    }

    @Override
    public MemoryBlock duplicate() {
        return new HeapMemoryBlock(array(), arrayOffset, size);
    }

    @Override
    public MemoryBlock attachment() {
        return null;
    }

    @Override
    public boolean isNullPointer() {
        return size == 0 || array() == null;
    }

    @Override
    public void close() {
        arrayRef.set(null);
    }

    private static long unsignedAddExact(long x, long y) {
        long sum = x + y;
        if (Long.compareUnsigned(x, sum) > 0 && Long.compareUnsigned(y, sum) > 0) throw new ArithmeticException("long overflow");
        return sum;
    }

}
