package multiffi.ffi;

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
            List<CompoundElement> list = new ArrayList<>(length);
            for (int i = 0; i < length; i ++) {
                list.add(new CompoundElement(types[i], offsets[i], repetitions[i]));
            }
            CompoundElement last = list.get(length - 1);
            return new CompoundType(Collections.unmodifiableList(list), -1,
                    Util.unsignedAddExact(last.getOffset(), Util.unsignedAddExact(last.size(), padding)));
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
            List<CompoundElement> list = new ArrayList<>(length);
            for (int i = 0; i < length; i ++) {
                list.add(new CompoundElement(types[typesOffset + i], offsets[offsetsOffset + i], repetitions[repetitionsOffset + i]));
            }
            CompoundElement last = list.get(length - 1);
            return new CompoundType(Collections.unmodifiableList(list), -1,
                    Util.unsignedAddExact(last.getOffset(), Util.unsignedAddExact(last.size(), padding)));
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
            List<CompoundElement> list = new ArrayList<>(length);
            long offset = 0;
            ForeignType type = types[typesOffset];
            CompoundElement first = new CompoundElement(type, offset, repetitions[repetitionsOffset]);
            long alignment = first.getType().size();
            list.add(first);
            offset = Util.unsignedAddExact(offset, first.size());
            for (int i = 1; i < length; i ++) {
                long size = Util.unsignedMin(Foreign.addressSize(), align(types[i + typesOffset]));
                long left = Long.remainderUnsigned(offset, size);
                if (Long.compareUnsigned(left, 0) > 0) offset = Util.unsignedAddExact(offset, size - left);
                CompoundElement compoundElement = new CompoundElement(types[i + typesOffset], offset, repetitions[i + repetitionsOffset]);
                alignment = Math.max(alignment, size);
                list.add(compoundElement);
                offset = Util.unsignedAddExact(offset, compoundElement.size());
            }
            if (length != 1) {
                long left = Long.remainderUnsigned(list.get(list.size() - 1).getType().size(), alignment);
                if (left != 0) offset = Util.unsignedAddExact(offset, alignment - left);
                return new CompoundType(Collections.unmodifiableList(list), 0, offset);
            }
            else return new CompoundType(Collections.unmodifiableList(list), 0, offset);
        }
    }

    private static long align(ForeignType type) {
        if (type instanceof CompoundType) {
            long size = 0;
            for (CompoundElement compoundElement : ((CompoundType) type).compoundElements) {
                size = Util.unsignedMax(size, align(compoundElement.getType()));
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
            List<CompoundElement> list = new ArrayList<>();
            for (int i = 0; i < length; i ++) {
                CompoundElement compoundElement = new CompoundElement(types[i + typesOffset], 0, repetitions[i + repetitionsOffset]);
                list.add(compoundElement);
                alignment = Util.unsignedMax(alignment, compoundElement.getType().size());
                size = Util.unsignedMax(size, compoundElement.size());
            }
            if (Long.remainderUnsigned(size, alignment) != 0)
                size = Util.unsignedMultiplyExact(size / (alignment + 1), alignment);
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
        else return new CompoundType(Collections.singletonList(new CompoundElement(type, 0, repetition)), 2,
                Util.unsignedMultiplyExact(type.size(), repetition));
    }
    
    private final List<CompoundElement> compoundElements;
    private final long size;
    private final int kind; // -1 = auto, 0 = struct, 1 = union, 2 = array

    private CompoundType(List<CompoundElement> compoundElements, int kind, long size) {
        this.compoundElements = compoundElements;
        this.size = size;
        if (kind == -1) {
            kind = 0;
            for (CompoundElement compoundElement : compoundElements) {
                if (compoundElement.getOffset() != 0) {
                    kind = 1;
                    break;
                }
            }
        }
        this.kind = kind;
    }

    @Override
    public Class<?> carrier() {
        return MemoryHandle.class;
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

    @Override
    public CompoundElement[] getElements() {
        return compoundElements.toArray(Util.EMPTY_COMPOUND_ELEMENT_ARRAY);
    }

    @Override
    public CompoundElement getElement(int index) throws IndexOutOfBoundsException {
        return compoundElements.get(index);
    }

    @Override
    public ForeignType getComponentType() {
        return isArray() ? compoundElements.get(0).getType() : null;
    }

}
