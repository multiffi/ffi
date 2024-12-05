package multiffi;

public abstract class AbstractDirectMemoryBlock extends MemoryBlock {

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
        return Allocator.getBoolean(address() + offset);
    }

    @Override
    public byte getInt8(long offset) {
        checkBounds(offset, 1);
        return Allocator.getInt8(address() + offset);
    }

    @Override
    public short getInt16(long offset) {
        checkBounds(offset, 2);
        return Allocator.getInt16(address() + offset);
    }

    @Override
    public char getUTF16(long offset) {
        checkBounds(offset, 2);
        return Allocator.getUTF16(address() + offset);
    }

    @Override
    public int getInt32(long offset) {
        checkBounds(offset, 4);
        return Allocator.getInt32(address() + offset);
    }

    @Override
    public long getInt64(long offset) {
        checkBounds(offset, 8);
        return Allocator.getInt64(address() + offset);
    }

    @Override
    public float getFloat(long offset) {
        checkBounds(offset, 4);
        return Allocator.getFloat(address() + offset);
    }

    @Override
    public double getDouble(long offset) {
        checkBounds(offset, 8);
        return Allocator.getDouble(address() + offset);
    }

    @Override
    public void setBoolean(long offset, boolean value) {
        checkBounds(offset, 1);
        Allocator.setBoolean(address() + offset, value);
    }

    @Override
    public void setInt8(long offset, byte value) {
        checkBounds(offset, 1);
        Allocator.setInt8(address() + offset, value);
    }

    @Override
    public void setInt16(long offset, short value) {
        checkBounds(offset, 2);
        Allocator.setInt16(address() + offset, value);
    }

    @Override
    public void setUTF16(long offset, char value) {
        checkBounds(offset, 2);
        Allocator.setUTF16(address() + offset, value);
    }

    @Override
    public void setInt32(long offset, int value) {
        checkBounds(offset, 4);
        Allocator.setInt32(address() + offset, value);
    }

    @Override
    public void setInt64(long offset, long value) {
        checkBounds(offset, 8);
        Allocator.setInt64(address() + offset, value);
    }

    @Override
    public void setFloat(long offset, float value) {
        checkBounds(offset, 4);
        Allocator.setFloat(address() + offset, value);
    }

    @Override
    public void setDouble(long offset, double value) {
        checkBounds(offset, 8);
        Allocator.setDouble(address() + offset, value);
    }

    @Override
    public void transferTo(long srcOffset, MemoryBlock destMemoryBlock, long destOffset, long size) {
        checkBounds(srcOffset, size);
        destMemoryBlock.checkBounds(destOffset, size);
        if (destMemoryBlock.isDirect()) Allocator.copy(destMemoryBlock.address() + destOffset, address() + srcOffset, size);
        else Allocator.copy(destMemoryBlock.array(), destOffset, address() + srcOffset, size);
    }

    @Override
    public void transferTo(MemoryBlock destMemoryBlock, long size) {
        transferTo(0, destMemoryBlock, 0, size);
    }

    @Override
    public void transferFrom(long destOffset, MemoryBlock srcMemoryBlock, long srcOffset, long size) {
        checkBounds(destOffset, size);
        srcMemoryBlock.checkBounds(srcOffset, size);
        if (srcMemoryBlock.isDirect()) Allocator.copy(address() + destOffset, srcMemoryBlock.address() + srcOffset, size);
        else Allocator.copy(address() + destOffset, srcMemoryBlock.array(), srcOffset, size);
    }

    @Override
    public void transferFrom(MemoryBlock srcMemoryBlock, long size) {
        transferFrom(0, srcMemoryBlock, 0, size);
    }

    @Override
    public void checkBounds(long offset) throws IndexOutOfBoundsException {
        if (Long.compareUnsigned(offset, size()) >= 0) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(offset));
    }

    @Override
    public void checkBounds(long offset, long size) throws IndexOutOfBoundsException {
        long index = unsignedAddExact(offset, size);
        if (Long.compareUnsigned(index, size()) > 0) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(index));
    }

    @Override
    public boolean inBounds(long offset) {
        return Long.compareUnsigned(offset, size()) < 0;
    }

    @Override
    public boolean inBounds(long offset, long size) {
        long index = unsignedAddExact(offset, size);
        return Long.compareUnsigned(index, size()) < 0;
    }

    @Override
    public void fill(long offset, byte value, long size) {
        Allocator.fill(address() + offset, value, size);
    }

    @Override
    public void fill(long offset, int value, long size) {
        Allocator.fill(address() + offset, value, size);
    }

    @Override
    public long indexOf(long offset, byte value) {
        checkBounds(offset);
        long index = address() + offset;
        return Allocator.search(index, value) - address();
    }

    @Override
    public long indexOf(long offset, int value) {
        checkBounds(offset);
        long index = address() + offset;
        return Allocator.search(index, value) - address();
    }

    @Override
    public long indexOf(long offset, byte value, long maxLength) {
        checkBounds(offset);
        long index = address() + offset;
        return Allocator.search(index, value, maxLength) - address();
    }

    @Override
    public long indexOf(long offset, int value, long maxLength) {
        checkBounds(offset);
        long index = address() + offset;
        return Allocator.search(index, value, maxLength) - address();
    }

    @Override
    public long indexOf(long offset, Object valueArray, int valueArrayOffset, int valueSize) {
        checkBounds(offset, valueSize);
        long index = address() + offset;
        return Allocator.search(array(), index, valueArray, valueArrayOffset, valueSize, size()) - address();
    }

    @Override
    public long indexOf(long offset, Object valueArray, int valueArrayOffset, int valueSize, long maxLength) {
        checkBounds(offset, valueSize);
        long index = address() + offset;
        return Allocator.search(array(), index, valueArray, valueArrayOffset, valueSize, maxLength) - address();
    }

    @Override
    public boolean isNullPointer() {
        return address() == 0;
    }

    private static long unsignedAddExact(long x, long y) {
        long sum = x + y;
        if (Long.compareUnsigned(x, sum) > 0) throw new ArithmeticException("long overflow");
        return sum;
    }

}
