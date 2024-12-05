package multiffi;

public final class Member {

    private final ForeignType type;
    private final long offset;
    private final long repetition;
    private final long size;

    Member(ForeignType type, long offset, long repetition) {
        this.type = type;
        if (offset < 0) throw new IllegalArgumentException("Negative member offset");
        else this.offset = offset;
        this.repetition = repetition;
        this.size = unsignedMultiplyExact(type.size(), repetition);
    }

    public long size() {
        return size;
    }

    public ForeignType getType() {
        return type;
    }

    public long getOffset() {
        return offset;
    }

    public long getRepetition() {
        return repetition;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        Member that = (Member) object;

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

    private static long multiplyHigh(long x, long y) {
        // Use technique from section 8-2 of Henry S. Warren, Jr.,
        // Hacker's Delight (2nd ed.) (Addison Wesley, 2013), 173-174.
        long x1 = x >> 32;
        long x2 = x & 0xFFFFFFFFL;
        long y1 = y >> 32;
        long y2 = y & 0xFFFFFFFFL;

        long z2 = x2 * y2;
        long t = x1 * y2 + (z2 >>> 32);
        long z1 = t & 0xFFFFFFFFL;
        long z0 = t >> 32;
        z1 += x2 * y1;

        return x1 * y1 + z0 + (z1 >> 32);
    }

    private static long unsignedMultiplyHigh(long x, long y) {
        long result = multiplyHigh(x, y);
        result += (y & (x >> 63)); // equivalent to `if (x < 0) result += y;`
        result += (x & (y >> 63)); // equivalent to `if (y < 0) result += x;`
        return result;
    }

    private static long unsignedMultiplyExact(long x, long y) {
        long hi = unsignedMultiplyHigh(x, y);
        if (hi != 0) throw new ArithmeticException("long overflow");
        return x * y;
    }

}
