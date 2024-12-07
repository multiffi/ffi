package multiffi;

public final class Member {

    static final Member[] EMPTY_MEMBER_ARRAY = new Member[0];

    private final ForeignType type;
    private final long offset;
    private final long repetition;
    private final long size;

    Member(ForeignType type, long offset, long repetition) {
        this.type = type;
        if (offset < 0) throw new IllegalArgumentException("Negative member offset");
        else this.offset = offset;
        this.repetition = repetition;
        this.size = Util.unsignedMultiplyExact(type.size(), repetition);
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

}
