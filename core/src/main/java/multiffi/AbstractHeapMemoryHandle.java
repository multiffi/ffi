package multiffi;

public abstract class AbstractHeapMemoryHandle extends MemoryHandle {

    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public long address() {
        return 0;
    }

    @Override
    public boolean isBounded() {
        return true;
    }

    @Override
    public boolean hasArray() {
        return true;
    }

    @Override
    public boolean getBoolean(long offset) {
        checkBounds(offset, 1);
        return Allocator.getBoolean(array(), offset);
    }

    @Override
    public byte getInt8(long offset) {
        checkBounds(offset, 1);
        return Allocator.getInt8(array(), offset);
    }

    @Override
    public short getInt16(long offset) {
        checkBounds(offset, 2);
        return Allocator.getInt16(array(), offset);
    }

    @Override
    public char getUTF16(long offset) {
        checkBounds(offset, 2);
        return Allocator.getUTF16(array(), offset);
    }

    @Override
    public int getInt32(long offset) {
        checkBounds(offset, 4);
        return Allocator.getInt32(array(), offset);
    }

    @Override
    public long getInt64(long offset) {
        checkBounds(offset, 8);
        return Allocator.getInt64(array(), offset);
    }

    @Override
    public float getFloat(long offset) {
        checkBounds(offset, 4);
        return Allocator.getFloat(array(), offset);
    }

    @Override
    public double getDouble(long offset) {
        checkBounds(offset, 8);
        return Allocator.getDouble(array(), offset);
    }

    @Override
    public void setBoolean(long offset, boolean value) {
        checkBounds(offset, 1);
        Allocator.setBoolean(array(), offset, value);
    }

    @Override
    public void setInt8(long offset, byte value) {
        checkBounds(offset, 1);
        Allocator.setInt8(array(), offset, value);
    }

    @Override
    public void setInt16(long offset, short value) {
        checkBounds(offset, 2);
        Allocator.setInt16(array(), offset, value);
    }

    @Override
    public void setUTF16(long offset, char value) {
        checkBounds(offset, 2);
        Allocator.setUTF16(array(), offset, value);
    }

    @Override
    public void setInt32(long offset, int value) {
        checkBounds(offset, 4);
        Allocator.setInt32(array(), offset, value);
    }

    @Override
    public void setInt64(long offset, long value) {
        checkBounds(offset, 8);
        Allocator.setInt64(array(), offset, value);
    }

    @Override
    public void setFloat(long offset, float value) {
        checkBounds(offset, 4);
        Allocator.setFloat(array(), offset, value);
    }

    @Override
    public void setDouble(long offset, double value) {
        checkBounds(offset, 8);
        Allocator.setDouble(array(), offset, value);
    }

    @Override
    public void transferTo(long srcOffset, MemoryHandle destMemoryHandle, long destOffset, long size) {
        checkBounds(srcOffset, size);
        destMemoryHandle.checkBounds(destOffset, size);
        if (destMemoryHandle.hasArray()) Allocator.copy(destMemoryHandle.array(), destOffset, array(), srcOffset, size);
        else Allocator.copy(destMemoryHandle.address() + destOffset, array(), srcOffset, size);
    }

    @Override
    public void transferTo(MemoryHandle destMemoryHandle, long size) {
        transferTo(0, destMemoryHandle, 0, size);
    }

    @Override
    public void transferFrom(long destOffset, MemoryHandle srcMemoryHandle, long srcOffset, long size) {
        checkBounds(destOffset, size);
        srcMemoryHandle.checkBounds(srcOffset, size);
        if (srcMemoryHandle.hasArray()) Allocator.copy(array(), destOffset, srcMemoryHandle.array(), srcOffset, size);
        else Allocator.copy(array(), destOffset, Util.unsignedAddExact(srcMemoryHandle.address(), srcOffset), size);
    }

    @Override
    public void transferFrom(MemoryHandle srcMemoryHandle, long size) {
        transferFrom(0, srcMemoryHandle, 0, size);
    }

    @Override
    public void checkBounds(long offset) throws IndexOutOfBoundsException {
        if (offset < 0 || offset >= size()) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(offset));
    }

    @Override
    public void checkBounds(long offset, long size) throws IndexOutOfBoundsException {
        long index = Util.unsignedAddExact(offset, size);
        if (index < 0 || index > size()) throw new ArrayIndexOutOfBoundsException("Array index out of range: " + Long.toUnsignedString(index));
    }

    @Override
    public boolean inBounds(long offset) {
        return offset >= 0 && offset < size();
    }

    @Override
    public boolean inBounds(long offset, long size) {
        long index = Util.unsignedAddExact(offset, size);
        return index >= 0 && index < size();
    }

    @Override
    public void fill(long offset, byte value, long size) {
        Allocator.fill(array(), offset, value, size);
    }

    @Override
    public void fill(long offset, int value, long size) {
        Allocator.fill(array(), offset, value, size);
    }

    @Override
    public long indexOf(long offset, byte value) {
        checkBounds(offset);
        long index = offset + arrayOffset();
        return Allocator.search(array(), index, value) - arrayOffset();
    }

    @Override
    public long indexOf(long offset, int value) {
        checkBounds(offset);
        long index = offset + arrayOffset();
        return Allocator.search(array(), index, value) - arrayOffset();
    }

    @Override
    public long indexOf(long offset, byte value, long maxLength) {
        checkBounds(offset);
        long index = offset + arrayOffset();
        return Allocator.search(array(), index, value, maxLength) - arrayOffset();
    }

    @Override
    public long indexOf(long offset, int value, long maxLength) {
        checkBounds(offset);
        long index = offset + arrayOffset();
        return Allocator.search(array(), index, value, maxLength) - arrayOffset();
    }

    @Override
    public long indexOf(long offset, Object valueArray, int valueArrayOffset, int valueSize) {
        checkBounds(offset, valueSize);
        long index = offset + arrayOffset();
        return Allocator.search(array(), index, valueArray, valueArrayOffset, valueSize) - arrayOffset();
    }

    @Override
    public long indexOf(long offset, Object valueArray, int valueArrayOffset, int valueSize, long maxLength) {
        checkBounds(offset, valueSize);
        long index = offset + arrayOffset();
        return Allocator.search(array(), index, valueArray, valueArrayOffset, valueSize, maxLength) - arrayOffset();
    }

}
