package multiffi.ffi;

import io.github.multiffi.ffi.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompoundType extends ForeignType {

    private static final CompoundType EMPTY = new CompoundType(Collections.emptyList(), 0, 0);
    public static CompoundType ofEmpty() {
        return EMPTY;
    }

    public static CompoundType of(ForeignType[] types, long[] offsets, long[] repetitions, long padding) {
        int length = types.length;
        if (length != offsets.length) throw new ArrayIndexOutOfBoundsException("length mismatch");
        if (length != repetitions.length) throw new ArrayIndexOutOfBoundsException("length mismatch");
        if (length == 0) return new CompoundType(Collections.emptyList(), 0, padding);
        else {
            List<CompoundElement> list = new ArrayList<>(length);
            for (int i = 0; i < length; i ++) {
                list.add(new CompoundElement(types[i], offsets[i], repetitions[i]));
            }
            CompoundElement last = list.get(length - 1);
            return new CompoundType(Collections.unmodifiableList(list), -1,
                    Util.unsignedAddExact(last.offset(), Util.unsignedAddExact(last.size(), padding)));
        }
    }

    public static CompoundType of(ForeignType[] types, long[] offsets, long padding) {
        int length = types.length;
        if (length != offsets.length) throw new ArrayIndexOutOfBoundsException("length mismatch");
        if (length == 0) return new CompoundType(Collections.emptyList(), 0, padding);
        else {
            List<CompoundElement> list = new ArrayList<>(length);
            for (int i = 0; i < length; i ++) {
                list.add(new CompoundElement(types[i], offsets[i], 1));
            }
            CompoundElement last = list.get(length - 1);
            return new CompoundType(Collections.unmodifiableList(list), -1,
                    Util.unsignedAddExact(last.offset(), Util.unsignedAddExact(last.size(), padding)));
        }
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
                    Util.unsignedAddExact(last.offset(), Util.unsignedAddExact(last.size(), padding)));
        }
    }

    public static CompoundType of(ForeignType[] types, int typesOffset,
                                  long[] offsets, int offsetsOffset,
                                  int length, long padding) {
        if (length == 0) return new CompoundType(Collections.emptyList(), -1, padding);
        else {
            List<CompoundElement> list = new ArrayList<>(length);
            for (int i = 0; i < length; i ++) {
                list.add(new CompoundElement(types[typesOffset + i], offsets[offsetsOffset + i], 1));
            }
            CompoundElement last = list.get(length - 1);
            return new CompoundType(Collections.unmodifiableList(list), -1,
                    Util.unsignedAddExact(last.offset(), Util.unsignedAddExact(last.size(), padding)));
        }
    }

    private static long alignSizeOf(ForeignType type) {
        if (type instanceof CompoundType) {
            long size = 0;
            for (CompoundElement compoundElement : ((CompoundType) type).compoundElements) {
                size = Util.unsignedMax(size, alignSizeOf(compoundElement.getType()));
            }
            return size;
        }
        else return type.size();
    }

    public static CompoundType ofStruct(ForeignType[] types, int typesOffset,
                                        long[] repetitions, int repetitionsOffset, long[] typeAligns, int typeAlignsOffset,
                                        int length, long packAlign) {
        if (length == 0) return EMPTY;
        else {
            if (packAlign == 0 || (packAlign & (packAlign - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
            long offset = 0;
            List<CompoundElement> list = new ArrayList<>(length);
            CompoundElement compoundElement = new CompoundElement(types[typesOffset], offset, repetitions[repetitionsOffset]);
            list.add(compoundElement);
            offset = Util.unsignedAddExact(offset, compoundElement.size());
            for (int i = 1; i < length; i ++) {
                ForeignType type = types[typesOffset + i];
                long typeAlignment = typeAligns[typeAlignsOffset + i];
                if (typeAlignment == 0 || (typeAlignment & (typeAlignment - 1)) != 0)
                    throw new IllegalArgumentException("alignment must be a power-of-two value");
                long alignmentRequirement = Util.unsignedMin(Util.unsignedMax(alignSizeOf(type), typeAlignment), packAlign);
                long remainder = Long.remainderUnsigned(offset, alignmentRequirement);
                if (remainder != 0) offset = Util.unsignedAddExact(offset, alignmentRequirement - remainder);
                compoundElement = new CompoundElement(type, offset, repetitions[i + repetitionsOffset]);
                list.add(compoundElement);
                offset = Util.unsignedAddExact(offset, compoundElement.size());
            }
            return new CompoundType(Collections.unmodifiableList(list), 0, offset);
        }
    }

    public static CompoundType ofStruct(ForeignType[] types, int typesOffset, int length, long packAlign) {
        if (length == 0) return EMPTY;
        else {
            if (packAlign == 0 || (packAlign & (packAlign - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
            long offset = 0;
            List<CompoundElement> list = new ArrayList<>(length);
            CompoundElement compoundElement = new CompoundElement(types[typesOffset], offset, 1);
            list.add(compoundElement);
            offset = Util.unsignedAddExact(offset, compoundElement.size());
            for (int i = 1; i < length; i ++) {
                ForeignType type = types[typesOffset + i];
                long alignmentRequirement = Util.unsignedMin(alignSizeOf(type), packAlign);
                long remainder = Long.remainderUnsigned(offset, alignmentRequirement);
                if (remainder != 0) offset = Util.unsignedAddExact(offset, alignmentRequirement - remainder);
                compoundElement = new CompoundElement(type, offset, 1);
                list.add(compoundElement);
                offset = Util.unsignedAddExact(offset, compoundElement.size());
            }
            return new CompoundType(Collections.unmodifiableList(list), 0, offset);
        }
    }

    public static CompoundType ofStruct(ForeignType[] types, long[] repetitions, long[] typeAligns, long packAlign) {
        int length = types.length;
        if (length != repetitions.length) throw new ArrayIndexOutOfBoundsException("length mismatch");
        if (length != typeAligns.length) throw new ArrayIndexOutOfBoundsException("length mismatch");
        return ofStruct(types, 0, repetitions, 0, typeAligns, 0, length, packAlign);
    }

    public static CompoundType ofStruct(ForeignType[] types, long packAlign) {
        return ofStruct(types, 0, types.length, packAlign);
    }

    public static CompoundType ofStruct(ForeignType... types) {
        if (types == null || types.length == 0) return EMPTY;
        else return ofStruct(types, 0, types.length, Foreign.alignmentSize());
    }

    public static CompoundType ofUnion(ForeignType[] types, int typesOffset, long[] repetitions, int repetitionsOffset, long[] typeAligns, int typeAlignsOffset, int length, long packAlign) {
        if (length == 0) return EMPTY;
        else {
            if (packAlign == 0 || (packAlign & (packAlign - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
            long size = 0;
            List<CompoundElement> list = new ArrayList<>();
            for (int i = 0; i < length; i ++) {
                ForeignType type = types[typesOffset + i];
                long typeAlignment = typeAligns[typeAlignsOffset + i];
                if (typeAlignment == 0 || (typeAlignment & (typeAlignment - 1)) != 0)
                    throw new IllegalArgumentException("alignment must be a power-of-two value");
                CompoundElement compoundElement = new CompoundElement(type, 0, repetitions[repetitionsOffset + i]);
                list.add(compoundElement);
                packAlign = Util.unsignedMax(packAlign, Util.unsignedMax(alignSizeOf(type), typeAlignment));
                size = Util.unsignedMax(size, compoundElement.size());
            }
            if (Long.remainderUnsigned(size, packAlign) != 0) size = Util.unsignedMultiplyExact(size / (packAlign + 1), packAlign);
            return new CompoundType(Collections.unmodifiableList(list), 1, size);
        }
    }

    public static CompoundType ofUnion(ForeignType[] types, int typesOffset, int length, long packAlign) {
        if (length == 0) return EMPTY;
        else {
            if (packAlign == 0 || (packAlign & (packAlign - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
            long size = 0;
            List<CompoundElement> list = new ArrayList<>();
            for (int i = 0; i < length; i ++) {
                ForeignType type = types[typesOffset + i];
                CompoundElement compoundElement = new CompoundElement(type, 0, 1);
                list.add(compoundElement);
                packAlign = Util.unsignedMax(packAlign, alignSizeOf(type));
                size = Util.unsignedMax(size, compoundElement.size());
            }
            if (Long.remainderUnsigned(size, packAlign) != 0) size = Util.unsignedMultiplyExact(size / (packAlign + 1), packAlign);
            return new CompoundType(Collections.unmodifiableList(list), 1, size);
        }
    }

    public static CompoundType ofUnion(ForeignType[] types, long[] repetitions, long[] typeAligns, long packAlign) {
        int length = types.length;
        if (length != repetitions.length) throw new ArrayIndexOutOfBoundsException("length mismatch");
        if (length != typeAligns.length) throw new ArrayIndexOutOfBoundsException("length mismatch");
        return ofUnion(types, 0, repetitions, 0, typeAligns, 0, length, packAlign);
    }

    public static CompoundType ofUnion(ForeignType... types) {
        if (types == null || types.length == 0) return EMPTY;
        else return ofUnion(types, 0, types.length, Foreign.alignmentSize());
    }

    public static CompoundType ofArray(ForeignType type, long repetition, long typeAlign) {
        if (repetition == 0) return EMPTY;
        else {
            if (typeAlign == 0 || (typeAlign & (typeAlign - 1)) != 0) throw new IllegalArgumentException("alignment must be a power-of-two value");
            return new CompoundType(Collections.singletonList(new CompoundElement(type, 0, repetition)),
                    2, Util.unsignedMultiplyExact(Util.unsignedMax(type.size(), typeAlign), repetition));
        }
    }

    public static CompoundType ofArray(ForeignType type, long repetition) {
        if (repetition == 0) return EMPTY;
        else return new CompoundType(Collections.singletonList(new CompoundElement(type, 0, repetition)),
                2, Util.unsignedMultiplyExact(type.size(), repetition));
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
                if (compoundElement.offset() != 0) {
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
    public boolean isNumeric() {
        return false;
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
