package multiffi;

public abstract class ForeignType {

    public static final ForeignType VOID = null;

    public abstract long size();
    public abstract Class<?> carrier();

    public abstract boolean isStruct();
    public abstract boolean isUnion();
    public abstract boolean isArray();
    public abstract boolean isScalar();
    public boolean isCompound() {
        return !isScalar();
    }

    public abstract Member getMember(int index) throws IndexOutOfBoundsException;
    public abstract Member[] getMembers();
    public abstract ForeignType getComponentType();

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ForeignType)) return false;

        ForeignType that = (ForeignType) object;
        return size() == that.size();
    }

    @Override
    public int hashCode() {
        long value = size();
        return (int) (value ^ (value >>> 32));
    }

    @Override
    public String toString() {
        String kind;
        if (isStruct()) kind = "struct:";
        else if (isUnion()) kind = "union:";
        else if (isArray()) kind = "array:";
        else kind = carrier() + ":";
        return kind + Long.toUnsignedString(size());
    }

}
