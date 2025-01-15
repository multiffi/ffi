package multiffi.ffi;

import io.github.multiffi.ffi.Util;

public final class CompoundElement {

    private final ForeignType type;
    private final long offset;
    private final long repetition;
    private final long size;

    CompoundElement(ForeignType type, long offset, long repetition) {
        this.type = type;
        this.offset = offset;
        this.repetition = repetition;
        this.size = Util.unsignedMultiplyExact(type.size(), repetition);
    }

    public ForeignType getType() {
        return type;
    }

    public boolean getBoolean(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getBoolean(Util.unsignedAddExact(offset, index * type.size()));
    }

    public char getUTF16(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getUTF16(Util.unsignedAddExact(offset, index * type.size()));
    }

    public byte getChar(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getChar(Util.unsignedAddExact(offset, index * type.size()));
    }

    public int getWChar(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getWChar(Util.unsignedAddExact(offset, index * type.size()));
    }

    public byte getInt8(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getInt8(Util.unsignedAddExact(offset, index * type.size()));
    }

    public short getInt16(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getInt16(Util.unsignedAddExact(offset, index * type.size()));
    }

    public int getInt32(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getInt32(Util.unsignedAddExact(offset, index * type.size()));
    }

    public long getInt64(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getInt64(Util.unsignedAddExact(offset, index * type.size()));
    }

    public long getAddress(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getAddress(Util.unsignedAddExact(offset, index * type.size()));
    }

    public long getSize(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getSize(Util.unsignedAddExact(offset, index * type.size()));
    }

    public long getLong(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getLong(Util.unsignedAddExact(offset, index * type.size()));
    }

    public long getInt(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getInt(Util.unsignedAddExact(offset, index * type.size()));
    }

    public long getShort(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getShort(Util.unsignedAddExact(offset, index * type.size()));
    }

    public float getFloat(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getFloat(Util.unsignedAddExact(offset, index * type.size()));
    }

    public double getDouble(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.getDouble(Util.unsignedAddExact(offset, index * type.size()));
    }

    public void setBoolean(MemoryHandle memoryHandle, long offset, long index, boolean value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setBoolean(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setUTF16(MemoryHandle memoryHandle, long offset, long index, char value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setUTF16(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setChar(MemoryHandle memoryHandle, long offset, long index, byte value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setChar(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setWChar(MemoryHandle memoryHandle, long offset, long index, int value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setWChar(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setInt8(MemoryHandle memoryHandle, long offset, long index, byte value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setInt8(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setInt16(MemoryHandle memoryHandle, long offset, long index, short value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setInt16(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setInt32(MemoryHandle memoryHandle, long offset, long index, int value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setInt32(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setInt64(MemoryHandle memoryHandle, long offset, long index, long value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setInt64(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setAddress(MemoryHandle memoryHandle, long offset, long index, long value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setAddress(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setSize(MemoryHandle memoryHandle, long offset, long index, long value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setSize(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setLong(MemoryHandle memoryHandle, long offset, long index, long value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setLong(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setInt(MemoryHandle memoryHandle, long offset, long index, long value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setInt(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setShort(MemoryHandle memoryHandle, long offset, long index, long value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setShort(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setFloat(MemoryHandle memoryHandle, long offset, long index, float value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setFloat(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public void setDouble(MemoryHandle memoryHandle, long offset, long index, double value) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        memoryHandle.setDouble(Util.unsignedAddExact(offset, index * type.size()), value);
    }

    public MemoryHandle sliceAt(MemoryHandle memoryHandle, long offset, long index) {
        if (Long.compareUnsigned(index, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
        return memoryHandle.slice(Util.unsignedAddExact(offset, index * type.size()), type.size());
    }

    public MemoryHandle sliceAtRange(MemoryHandle memoryHandle, long offset, long index, long length) {
        long size = Util.unsignedAddExact(index, length);
        if (Long.compareUnsigned(size, repetition) >= 0) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(size));
        return memoryHandle.slice(Util.unsignedAddExact(offset, index * type.size()), length * type.size());
    }

    public MemoryHandle sliceAll(MemoryHandle memoryHandle, long offset) {
        return memoryHandle.slice(offset, size);
    }

    public long size() {
        return size;
    }

    public long offset() {
        return offset;
    }

    public long repetition() {
        return repetition;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        CompoundElement that = (CompoundElement) object;

        return size == that.size;
    }

    @Override
    public int hashCode() {
        return (int) (size ^ (size >>> 32));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(type);
        if (repetition > 1) builder.append("[").append(Long.toUnsignedString(repetition)).append("]");
        return builder.toString();
    }

}
