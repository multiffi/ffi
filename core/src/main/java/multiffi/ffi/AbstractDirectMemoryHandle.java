package multiffi.ffi;

import io.github.multiffi.ffi.Util;

public abstract class AbstractDirectMemoryHandle extends MemoryHandle {

    @Override
    public boolean isDirect() {
        return true;
    }

    @Override
    public boolean isBounded() {
        return size() != -1;
    }

    @Override
    public boolean hasArray() {
        return false;
    }

    @Override
    public Object array() {
        return null;
    }

    @Override
    public long arrayOffset() {
        return 0;
    }

    @Override
    public long arrayLength() {
        return 0;
    }

    @Override
    public boolean getBoolean(long offset) {
        checkBounds(offset, 1);
        return Memory.getBoolean(address() + offset);
    }

    @Override
    public byte getInt8(long offset) {
        checkBounds(offset, 1);
        return Memory.getInt8(address() + offset);
    }

    @Override
    public short getInt16(long offset) {
        checkBounds(offset, 2);
        return Memory.getInt16(address() + offset);
    }

    @Override
    public char getUTF16(long offset) {
        checkBounds(offset, 2);
        return Memory.getUTF16(address() + offset);
    }

    @Override
    public int getInt32(long offset) {
        checkBounds(offset, 4);
        return Memory.getInt32(address() + offset);
    }

    @Override
    public long getInt64(long offset) {
        checkBounds(offset, 8);
        return Memory.getInt64(address() + offset);
    }

    @Override
    public float getFloat(long offset) {
        checkBounds(offset, 4);
        return Memory.getFloat(address() + offset);
    }

    @Override
    public double getDouble(long offset) {
        checkBounds(offset, 8);
        return Memory.getDouble(address() + offset);
    }

    @Override
    public void setBoolean(long offset, boolean value) {
        checkBounds(offset, 1);
        Memory.setBoolean(address() + offset, value);
    }

    @Override
    public void setInt8(long offset, byte value) {
        checkBounds(offset, 1);
        Memory.setInt8(address() + offset, value);
    }

    @Override
    public void setInt16(long offset, short value) {
        checkBounds(offset, 2);
        Memory.setInt16(address() + offset, value);
    }

    @Override
    public void setUTF16(long offset, char value) {
        checkBounds(offset, 2);
        Memory.setUTF16(address() + offset, value);
    }

    @Override
    public void setInt32(long offset, int value) {
        checkBounds(offset, 4);
        Memory.setInt32(address() + offset, value);
    }

    @Override
    public void setInt64(long offset, long value) {
        checkBounds(offset, 8);
        Memory.setInt64(address() + offset, value);
    }

    @Override
    public void setFloat(long offset, float value) {
        checkBounds(offset, 4);
        Memory.setFloat(address() + offset, value);
    }

    @Override
    public void setDouble(long offset, double value) {
        checkBounds(offset, 8);
        Memory.setDouble(address() + offset, value);
    }

    @Override
    public void transferTo(long srcOffset, MemoryHandle destMemoryHandle, long destOffset, long size) {
        checkBounds(srcOffset, size);
        destMemoryHandle.checkBounds(destOffset, size);
        if (destMemoryHandle.isDirect()) Memory.copy(destMemoryHandle.address() + destOffset, address() + srcOffset, size);
        else Memory.copy(destMemoryHandle.array(), destOffset, address() + srcOffset, size);
    }

    @Override
    public void transferTo(MemoryHandle destMemoryHandle, long size) {
        transferTo(0, destMemoryHandle, 0, size);
    }

    @Override
    public void transferFrom(long destOffset, MemoryHandle srcMemoryHandle, long srcOffset, long size) {
        checkBounds(destOffset, size);
        srcMemoryHandle.checkBounds(srcOffset, size);
        if (srcMemoryHandle.isDirect()) Memory.copy(address() + destOffset, srcMemoryHandle.address() + srcOffset, size);
        else Memory.copy(address() + destOffset, srcMemoryHandle.array(), srcOffset, size);
    }

    @Override
    public void transferFrom(MemoryHandle srcMemoryHandle, long size) {
        transferFrom(0, srcMemoryHandle, 0, size);
    }

    @Override
    public void checkBounds(long offset) throws IndexOutOfBoundsException {
        if (Long.compareUnsigned(offset, size()) >= 0) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(offset));
    }

    @Override
    public void checkBounds(long offset, long size) throws IndexOutOfBoundsException {
        long index = Util.unsignedAddExact(offset, size);
        if (Long.compareUnsigned(index, size()) > 0) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(index));
    }

    @Override
    public boolean inBounds(long offset) {
        return Long.compareUnsigned(offset, size()) < 0;
    }

    @Override
    public boolean inBounds(long offset, long size) {
        long index = Util.unsignedAddExact(offset, size);
        return Long.compareUnsigned(index, size()) < 0;
    }

    @Override
    public void fill(long offset, byte value, long size) {
        Memory.fill(address() + offset, value, size);
    }

    @Override
    public void fill(long offset, int value, long size) {
        Memory.fill(address() + offset, value, size);
    }

    @Override
    public long indexOf(long offset, byte value) {
        checkBounds(offset);
        long index = address() + offset;
        return Memory.search(index, value) - address();
    }

    @Override
    public long indexOf(long offset, int value) {
        checkBounds(offset);
        long index = address() + offset;
        return Memory.search(index, value) - address();
    }

    @Override
    public long indexOf(long offset, byte value, long maxLength) {
        checkBounds(offset);
        long index = address() + offset;
        return Memory.search(index, value, maxLength) - address();
    }

    @Override
    public long indexOf(long offset, int value, long maxLength) {
        checkBounds(offset);
        long index = address() + offset;
        return Memory.search(index, value, maxLength) - address();
    }

    @Override
    public long indexOf(long offset, Object valueArray, int valueArrayOffset, int valueSize) {
        checkBounds(offset, valueSize);
        long index = address() + offset;
        return Memory.search(array(), index, valueArray, valueArrayOffset, valueSize, size()) - address();
    }

    @Override
    public long indexOf(long offset, Object valueArray, int valueArrayOffset, int valueSize, long maxLength) {
        checkBounds(offset, valueSize);
        long index = address() + offset;
        return Memory.search(array(), index, valueArray, valueArrayOffset, valueSize, maxLength) - address();
    }

    @Override
    public boolean isNil() {
        return address() == 0;
    }

}
