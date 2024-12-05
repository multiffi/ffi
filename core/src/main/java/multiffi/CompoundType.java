package multiffi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CompoundType extends ForeignType {

    private static final CompoundType EMPTY = new CompoundType(Collections.emptyList(), 0, 0);
    public static CompoundType ofEmpty() {
        return EMPTY;
    }

    public static CompoundType of(ForeignType[] types, long[] offsets, long[] repetitions, long padding) {
        if (types.length != offsets.length) throw new IndexOutOfBoundsException("Array length mismatch");
        if (offsets.length != repetitions.length) throw new IndexOutOfBoundsException("Array length mismatch");
        int length = offsets.length;
        if (length == 0) return new CompoundType(Collections.emptyList(), 0, padding);
        else {
            List<Member> list = new ArrayList<>(length);
            for (int i = 0; i < length; i ++) {
                list.add(new Member(types[i], offsets[i], repetitions[i]));
            }
            Member last = list.get(length - 1);
            return new CompoundType(Collections.unmodifiableList(list), -1,
                    unsignedAddExact(last.getOffset(), unsignedAddExact(last.size(), padding)));
        }
    }

    public static CompoundType of(ForeignType[] types, long[] offsets, long padding) {
        long[] repetitions = new long[types.length];
        Arrays.fill(repetitions, 1);
        return of(types, offsets, repetitions, padding);
    }

    public static CompoundType of(ForeignType[] types, int typesOffset,
                                  long[] offsets, int offsetsOffset,
                                  long[] repetitions, int repetitionsOffset,
                                  int length, long padding) {
        if (length == 0) return new CompoundType(Collections.emptyList(), -1, padding);
        else {
            List<Member> list = new ArrayList<>(length);
            for (int i = 0; i < length; i ++) {
                list.add(new Member(types[typesOffset + i], offsets[offsetsOffset + i], repetitions[repetitionsOffset + i]));
            }
            Member last = list.get(length - 1);
            return new CompoundType(Collections.unmodifiableList(list), -1,
                    unsignedAddExact(last.getOffset(), unsignedAddExact(last.size(), padding)));
        }
    }

    public static CompoundType of(ForeignType[] types, int typesOffset,
                                  long[] offsets, int offsetsOffset,
                                  int length, long padding) {
        long[] repetitions = new long[length];
        Arrays.fill(repetitions, 1);
        return of(types, typesOffset, offsets, offsetsOffset, repetitions, 0, length, padding);
    }

    public static CompoundType ofStruct(ForeignType[] types, int typesOffset,
                                        long[] repetitions, int repetitionsOffset, int length) {
        if (length == 0) return EMPTY;
        else {
            List<Member> list = new ArrayList<>(length);
            long offset = 0;
            ForeignType type = types[typesOffset];
            Member first = new Member(type, offset, repetitions[repetitionsOffset]);
            long alignment = first.getType().size();
            list.add(first);
            offset = unsignedAddExact(offset, first.size());
            for (int i = 1; i < length; i ++) {
                long size = unsignedMin(Foreign.addressSize(), align(types[i + typesOffset]));
                long left = Long.remainderUnsigned(offset, size);
                if (Long.compareUnsigned(left, 0) > 0) offset = unsignedAddExact(offset, size - left);
                Member member = new Member(types[i + typesOffset], offset, repetitions[i + repetitionsOffset]);
                alignment = Math.max(alignment, size);
                list.add(member);
                offset = unsignedAddExact(offset, member.size());
            }
            if (length != 1) {
                long left = Long.remainderUnsigned(list.get(list.size() - 1).getType().size(), alignment);
                if (left != 0) offset = unsignedAddExact(offset, alignment - left);
                return new CompoundType(Collections.unmodifiableList(list), 0, offset);
            }
            else return new CompoundType(Collections.unmodifiableList(list), 0, offset);
        }
    }

    private static long align(ForeignType type) {
        if (type instanceof CompoundType) {
            long size = 0;
            for (Member member : ((CompoundType) type).members) {
                size = unsignedMax(size, align(member.getType()));
            }
            return size;
        }
        else return type.size();
    }

    public static CompoundType ofStruct(ForeignType[] types, int typesOffset, int length) {
        long[] repetitions = new long[length];
        Arrays.fill(repetitions, 1);
        return ofStruct(types, typesOffset, repetitions, 0, length);
    }

    public static CompoundType ofStruct(ForeignType[] types, long[] repetitions) {
        if (types.length != repetitions.length) throw new IndexOutOfBoundsException("Array length mismatch");
        return ofStruct(types, 0, repetitions, 0, types.length);
    }

    public static CompoundType ofStruct(ForeignType... types) {
        long[] repetitions = new long[types.length];
        Arrays.fill(repetitions, 1);
        return ofStruct(types, repetitions);
    }

    public static CompoundType ofUnion(ForeignType[] types, int typesOffset,
                                       long[] repetitions, int repetitionsOffset, int length) {
        if (length == 0) return EMPTY;
        else {
            long alignment = 0;
            long size = 0;
            List<Member> list = new ArrayList<>();
            for (int i = 0; i < length; i ++) {
                Member member = new Member(types[i + typesOffset], 0, repetitions[i + repetitionsOffset]);
                list.add(member);
                alignment = unsignedMax(alignment, member.getType().size());
                size = unsignedMax(size, member.size());
            }
            if (Long.remainderUnsigned(size, alignment) != 0)
                size = unsignedMultiplyExact(size / (alignment + 1), alignment);
            return new CompoundType(Collections.unmodifiableList(list), 1, size);
        }
    }

    public static CompoundType ofUnion(ForeignType[] types, int typesOffset, int length) {
        long[] memberRepetitions = new long[length];
        Arrays.fill(memberRepetitions, 1);
        return ofUnion(types, typesOffset, memberRepetitions, 0, length);
    }

    public static CompoundType ofUnion(ForeignType[] types, long[] repetitions) {
        if (types.length != repetitions.length) throw new IndexOutOfBoundsException("Array length mismatch");
        return ofUnion(types, 0, repetitions, 0, types.length);
    }

    public static CompoundType ofUnion(ForeignType... types) {
        long[] memberRepetitions = new long[types.length];
        Arrays.fill(memberRepetitions, 1);
        return ofUnion(types, memberRepetitions);
    }

    public static CompoundType ofArray(ForeignType type, long repetition) {
        if (repetition == 0) return EMPTY;
        else return new CompoundType(Collections.singletonList(new Member(type, 0, repetition)), 2,
                unsignedMultiplyExact(type.size(), repetition));
    }
    
    private final List<Member> members;
    private final long size;
    private final int kind; // -1 = auto, 0 = struct, 1 = union, 2 = array

    private CompoundType(List<Member> members, int kind, long size) {
        this.members = members;
        this.size = size;
        if (kind == -1) {
            kind = 0;
            for (Member member : members) {
                if (member.getOffset() != 0) {
                    kind = 1;
                    break;
                }
            }
        }
        this.kind = kind;
    }

    @Override
    public Class<?> carrier() {
        return MemoryBlock.class;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public boolean isStruct() {
        return kind == 0;
    }

    @Override
    public boolean isUnion() {
        return kind == 1;
    }

    @Override
    public boolean isArray() {
        return kind == 2;
    }

    @Override
    public boolean isScalar() {
        return false;
    }

    private static final Member[] EMPTY_MEMBER_ARRAY = new Member[0];
    @Override
    public Member[] getMembers() {
        return members.toArray(EMPTY_MEMBER_ARRAY);
    }

    @Override
    public Member getMember(int index) throws IndexOutOfBoundsException {
        return members.get(index);
    }

    @Override
    public ForeignType getComponentType() {
        return isArray() ? members.get(0).getType() : null;
    }

    private static long unsignedMin(long a, long b) {
        if (a == 0) return a;
        else if (b == 0) return b;
        else {
            int s1 = Long.signum(a);
            int s2 = Long.signum(b);
            if (s1 == s2) return Math.min(a, b);
            else if (s1 < s2) return b;
            else return a;
        }
    }

    private static long unsignedMax(long a, long b) {
        if (a == 0) return b;
        else if (b == 0) return a;
        else {
            int s1 = Long.signum(a);
            int s2 = Long.signum(b);
            if (s1 == s2) return Math.max(a, b);
            else if (s1 < s2) return a;
            else return b;
        }
    }

    private static long unsignedAddExact(long x, long y) {
        long sum = x + y;
        if (Long.compareUnsigned(x, sum) > 0 && Long.compareUnsigned(y, sum) > 0) throw new ArithmeticException("long overflow");
        return sum;
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
