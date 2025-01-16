package io.github.multiffi.ffi;

import multiffi.ffi.CallOption;
import multiffi.ffi.ForeignType;
import multiffi.ffi.FunctionHandle;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
import multiffi.ffi.StandardCallOption;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class FFMFunctionHandle extends FunctionHandle {

    private final long address;
    private final int firstVarArgIndex;
    private final List<ForeignType> parameterTypes;
    private final ForeignType returnType;
    private final Function<Object[], Object> invokeFunction;
    private final boolean dyncall;
    private final boolean critical;
    private final boolean trivial;
    private final boolean saveErrno;

    public FFMFunctionHandle(long address, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        this.address = address;
        boolean dyncall = false;
        boolean saveErrno = false;
        boolean critical = false;
        boolean trivial = false;
        Set<Linker.Option> linkerOptionSet;
        if (options == null) linkerOptionSet = null;
        else {
            linkerOptionSet = new HashSet<>(3);
            for (CallOption option : options) {
                switch (option) {
                    case StandardCallOption.DYNCALL:
                        dyncall = true;
                        break;
                    case StandardCallOption.SAVE_ERRNO:
                        saveErrno = true;
                        linkerOptionSet.add(Linker.Option.captureCallState(FFMLastErrno.name()));
                        break;
                    case StandardCallOption.TRIVIAL:
                        trivial = true;
                    case StandardCallOption.CRITICAL:
                        critical = true;
                        break;
                    case StandardCallOption.STDCALL:
                        break;
                    default:
                        throw new IllegalArgumentException(option + " not supported");
                }
            }
            if (saveErrno && critical) critical = trivial = false;
            if (critical) linkerOptionSet.add(Linker.Option.critical(!trivial));
        }
        this.dyncall = dyncall;
        this.critical = critical;
        this.trivial = trivial;
        this.saveErrno = saveErrno;
        MemoryLayout returnLayout = returnType == null ? null : FFMUtil.toMemoryLayout(returnType);
        MemoryLayout[] parameterLayouts;
        if (parameterTypes == null || parameterTypes.length == 1 && parameterTypes[0] == null) parameterLayouts = FFMUtil.EMPTY_MEMORY_LAYOUT_ARRAY;
        else {
            parameterLayouts = new MemoryLayout[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i ++) {
                parameterLayouts[i] = FFMUtil.toMemoryLayout(parameterTypes[i]);
            }
        }
        int parameterCount = parameterLayouts.length;
        boolean addReturnMemoryParameter = returnType != null && returnType.isCompound();
        this.returnType = returnType;
        if (parameterCount == 0) this.parameterTypes = addReturnMemoryParameter ? Collections.singletonList(returnType) : Collections.emptyList();
        else {
            List<ForeignType> list = new ArrayList<>(parameterCount + (addReturnMemoryParameter ? 1 : 0));
            if (addReturnMemoryParameter) list.add(returnType);
            list.addAll(Arrays.asList(parameterTypes));
            this.parameterTypes = Collections.unmodifiableList(list);
        }
        this.firstVarArgIndex = firstVarArgIndex >= 0 ? Math.min(firstVarArgIndex, parameterCount) : (dyncall ? parameterCount : -1);
        if (linkerOptionSet != null && this.firstVarArgIndex >= 0) linkerOptionSet.add(Linker.Option.firstVariadicArg(this.firstVarArgIndex));
        Linker.Option[] linkerOptions = linkerOptionSet == null ? FFMUtil.EMPTY_LINKER_OPTION_ARRAY : linkerOptionSet.toArray(FFMUtil.EMPTY_LINKER_OPTION_ARRAY);
        if (dyncall) {
            if (saveErrno) invokeFunction = args -> {
                if (args == null || args.length != this.parameterTypes.size() + 1) throw new ArrayIndexOutOfBoundsException("length mismatch");
                int fixedArgsLength = args.length - 1;
                Object varargs = args[fixedArgsLength];
                if (!varargs.getClass().isArray()) throw new IllegalArgumentException("Last argument must be array as variadic arguments");
                int varargsLength = Array.getLength(varargs);
                Object[] arguments = new Object[args.length + varargsLength];
                System.arraycopy(args, 0, arguments, 1, fixedArgsLength);
                if (varargs instanceof Object[]) System.arraycopy(varargs, 0, arguments, args.length, varargsLength);
                else {
                    int offset = args.length;
                    for (int i = 0; i < varargsLength; i ++) {
                        arguments[offset + i] = Array.get(varargs, i);
                    }
                }
                arguments[addReturnMemoryParameter ? 1 : 0] = FFMLastErrno.segment();
                MemoryLayout[] newParameterLayouts = new MemoryLayout[parameterCount + varargsLength];
                System.arraycopy(parameterLayouts, 0, newParameterLayouts, 0, parameterCount);
                for (int i = 0; i < varargsLength; i ++) {
                    Object vararg = arguments[parameterCount + i];
                    if (vararg instanceof Boolean) newParameterLayouts[parameterCount + i] = FFMUtil.BOOL;
                    else if (vararg instanceof Byte) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_BYTE;
                    else if (vararg instanceof Character) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_CHAR;
                    else if (vararg instanceof Short) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_SHORT;
                    else if (vararg instanceof Integer) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_INT;
                    else if (vararg instanceof Long) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_LONG;
                    else if (vararg instanceof Float) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_FLOAT;
                    else if (vararg instanceof Double) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_DOUBLE;
                    else if (vararg instanceof MemoryHandle memoryHandle) {
                        long size = memoryHandle.size();
                        if (size < 0) {
                            long remaining = size - Long.MAX_VALUE;
                            newParameterLayouts[parameterCount + i] =
                                    MemoryLayout.structLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, ValueLayout.JAVA_BYTE),
                                            MemoryLayout.sequenceLayout(remaining, ValueLayout.JAVA_BYTE));
                        }
                        else newParameterLayouts[parameterCount + i] =
                                MemoryLayout.structLayout(MemoryLayout.sequenceLayout(size, ValueLayout.JAVA_BYTE));
                    }
                    else throw new IllegalArgumentException("Illegal argument: " + vararg);
                }
                MethodHandle methodHandle = FFMUtil.LINKER.downcallHandle(MemorySegment.ofAddress(address), returnType == null ?
                                FunctionDescriptor.ofVoid(newParameterLayouts) : FunctionDescriptor.of(returnLayout, newParameterLayouts), linkerOptions);
                for (int i = (addReturnMemoryParameter ? 1 : 0); i < this.parameterTypes.size(); i ++) {
                    ForeignType parameterType = this.parameterTypes.get(i);
                    if (parameterType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortArgument(methodHandle, i + 1, false);
                    else if (parameterType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntArgument(methodHandle, i + 1, false);
                    else if (parameterType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongArgument(methodHandle, i + 1, false);
                    else if (parameterType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterSizeArgument(methodHandle, i + 1, false);
                    else if (parameterType == ScalarType.ADDRESS) methodHandle = MethodHandles
                            .filterArguments(methodHandle, i + 1, FFMMethodFilters.INT64_TO_SEGMENT);
                    else if (parameterType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharArgument(methodHandle, i + 1, false);
                    else if (parameterType == ScalarType.BOOLEAN) methodHandle = FFMMethodFilters.filterBooleanArgument(methodHandle, i + 1, false);
                    else if (parameterType.isCompound()) methodHandle = MethodHandles
                            .filterArguments(methodHandle, i + 1, FFMMethodFilters.HANDLE_TO_SEGMENT);
                }
                if (returnType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortReturnValue(methodHandle, false);
                else if (returnType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntReturnValue(methodHandle, false);
                else if (returnType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongReturnValue(methodHandle, false);
                else if (returnType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterSizeReturnValue(methodHandle, false);
                else if (returnType == ScalarType.ADDRESS) methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMMethodFilters.SEGMENT_TO_INT64);
                else if (returnType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharReturnValue(methodHandle, false);
                else if (returnType == ScalarType.BOOLEAN) methodHandle = FFMMethodFilters.filterBooleanReturnValue(methodHandle, false);
                Invoker invoker = FFMASMRuntime.generateInvoker(arguments.length);
                Object result;
                try {
                    if (addReturnMemoryParameter) {
                        result = args[0];
                        arguments[0] = SegmentAllocator.slicingAllocator(FFMMethodFilters.handleToSegment((MemoryHandle) result));
                        invoker.invoke(methodHandle, arguments);
                    }
                    else result = invoker.invoke(methodHandle, arguments);
                } catch (ClassCastException | WrongMethodTypeException e) {
                    throw new IllegalArgumentException(e);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
                return result;
            };
            else invokeFunction = args -> {
                if (args == null || args.length != this.parameterTypes.size() + 1) throw new ArrayIndexOutOfBoundsException("length mismatch");
                int fixedArgsLength = args.length - 1;
                Object varargs = args[fixedArgsLength];
                if (!varargs.getClass().isArray()) throw new IllegalArgumentException("Last argument must be array as variadic arguments");
                int varargsLength = Array.getLength(varargs);
                Object[] arguments = new Object[fixedArgsLength + varargsLength];
                System.arraycopy(args, 0, arguments, 0, fixedArgsLength);
                if (varargs instanceof Object[]) System.arraycopy(varargs, 0, arguments, fixedArgsLength, varargsLength);
                else {
                    int offset = fixedArgsLength;
                    for (int i = 0; i < varargsLength; i ++) {
                        arguments[offset + i] = Array.get(varargs, i);
                    }
                }
                MemoryLayout[] newParameterLayouts = new MemoryLayout[parameterCount + varargsLength];
                System.arraycopy(parameterLayouts, 0, newParameterLayouts, 0, parameterCount);
                for (int i = 0; i < varargsLength; i ++) {
                    Object vararg = arguments[parameterCount + i];
                    if (vararg instanceof Boolean) newParameterLayouts[parameterCount + i] = FFMUtil.BOOL;
                    else if (vararg instanceof Byte) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_BYTE;
                    else if (vararg instanceof Character) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_CHAR;
                    else if (vararg instanceof Short) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_SHORT;
                    else if (vararg instanceof Integer) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_INT;
                    else if (vararg instanceof Long) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_LONG;
                    else if (vararg instanceof Float) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_FLOAT;
                    else if (vararg instanceof Double) newParameterLayouts[parameterCount + i] = ValueLayout.JAVA_DOUBLE;
                    else if (vararg instanceof MemoryHandle memoryHandle) {
                        long size = memoryHandle.size();
                        if (size < 0) {
                            long remaining = size - Long.MAX_VALUE;
                            newParameterLayouts[parameterCount + i] =
                                    MemoryLayout.structLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, ValueLayout.JAVA_BYTE),
                                            MemoryLayout.sequenceLayout(remaining, ValueLayout.JAVA_BYTE));
                        }
                        else newParameterLayouts[parameterCount + i] =
                                MemoryLayout.structLayout(MemoryLayout.sequenceLayout(size, ValueLayout.JAVA_BYTE));
                    }
                    else throw new IllegalArgumentException("Illegal argument: " + vararg);
                }
                MethodHandle methodHandle = FFMUtil.LINKER.downcallHandle(MemorySegment.ofAddress(address), returnType == null ?
                                FunctionDescriptor.ofVoid(newParameterLayouts) : FunctionDescriptor.of(returnLayout, newParameterLayouts), linkerOptions);
                for (int i = (addReturnMemoryParameter ? 1 : 0); i < this.parameterTypes.size(); i ++) {
                    ForeignType parameterType = this.parameterTypes.get(i);
                    if (parameterType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortArgument(methodHandle, i, false);
                    else if (parameterType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntArgument(methodHandle, i, false);
                    else if (parameterType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongArgument(methodHandle, i, false);
                    else if (parameterType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterSizeArgument(methodHandle, i, false);
                    else if (parameterType == ScalarType.ADDRESS) methodHandle = MethodHandles
                            .filterArguments(methodHandle, i, FFMMethodFilters.INT64_TO_SEGMENT);
                    else if (parameterType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharArgument(methodHandle, i, false);
                    else if (parameterType == ScalarType.BOOLEAN) methodHandle = FFMMethodFilters.filterBooleanArgument(methodHandle, i, false);
                    else if (parameterType.isCompound()) methodHandle = MethodHandles
                            .filterArguments(methodHandle, i, FFMMethodFilters.HANDLE_TO_SEGMENT);
                }
                if (returnType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortReturnValue(methodHandle, false);
                else if (returnType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntReturnValue(methodHandle, false);
                else if (returnType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongReturnValue(methodHandle, false);
                else if (returnType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterSizeReturnValue(methodHandle, false);
                else if (returnType == ScalarType.ADDRESS) methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMMethodFilters.SEGMENT_TO_INT64);
                else if (returnType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharReturnValue(methodHandle, false);
                else if (returnType == ScalarType.BOOLEAN) methodHandle = FFMMethodFilters.filterBooleanReturnValue(methodHandle, false);
                Invoker invoker = FFMASMRuntime.generateInvoker(arguments.length);
                Object result;
                try {
                    if (addReturnMemoryParameter) {
                        result = args[0];
                        arguments[0] = SegmentAllocator.slicingAllocator(FFMMethodFilters.handleToSegment((MemoryHandle) result));
                        invoker.invoke(methodHandle, arguments);
                    }
                    else result = invoker.invoke(methodHandle, arguments);
                } catch (ClassCastException | WrongMethodTypeException e) {
                    throw new IllegalArgumentException(e);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
                return result;
            };
        }
        else {
            MethodHandle methodHandle;
            if (parameterCount == 0) methodHandle = FFMUtil.LINKER.downcallHandle(MemorySegment.ofAddress(address), returnType == null ?
                            FunctionDescriptor.ofVoid() : FunctionDescriptor.of(returnLayout), linkerOptions);
            else methodHandle = FFMUtil.LINKER.downcallHandle(MemorySegment.ofAddress(address), returnType == null ?
                            FunctionDescriptor.ofVoid(parameterLayouts) : FunctionDescriptor.of(returnLayout, parameterLayouts), linkerOptions);
            for (int i = (addReturnMemoryParameter ? 1 : 0); i < this.parameterTypes.size(); i ++) {
                ForeignType parameterType = this.parameterTypes.get(i);
                if (parameterType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterSizeArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType == ScalarType.ADDRESS) methodHandle = MethodHandles
                        .filterArguments(methodHandle, i + (saveErrno ? 1 : 0), FFMMethodFilters.INT64_TO_SEGMENT);
                else if (parameterType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType == ScalarType.BOOLEAN) methodHandle = FFMMethodFilters.filterBooleanArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType.isCompound()) methodHandle = MethodHandles
                        .filterArguments(methodHandle, i + (saveErrno ? 1 : 0), FFMMethodFilters.HANDLE_TO_SEGMENT);
            }
            if (returnType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortReturnValue(methodHandle, false);
            else if (returnType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntReturnValue(methodHandle, false);
            else if (returnType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongReturnValue(methodHandle, false);
            else if (returnType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterSizeReturnValue(methodHandle, false);
            else if (returnType == ScalarType.ADDRESS) methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMMethodFilters.SEGMENT_TO_INT64);
            else if (returnType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharReturnValue(methodHandle, false);
            else if (returnType == ScalarType.BOOLEAN) methodHandle = FFMMethodFilters.filterBooleanReturnValue(methodHandle, false);
            Invoker invoker = FFMASMRuntime.generateInvoker(this.parameterTypes.size() + (saveErrno ? 1 : 0));
            MethodHandle function = methodHandle;
            if (addReturnMemoryParameter) {
                if (saveErrno) {
                    if (parameterCount == 0) invokeFunction = args -> {
                        if (args == null || args.length != 1) throw new ArrayIndexOutOfBoundsException("length mismatch");
                        MemoryHandle result = (MemoryHandle) args[0];
                        try {
                            invoker.invoke(function, SegmentAllocator.slicingAllocator(FFMMethodFilters.handleToSegment(result)), FFMLastErrno.segment());
                            return result;
                        } catch (ClassCastException | WrongMethodTypeException e) {
                            throw new IllegalArgumentException(e);
                        } catch (RuntimeException | Error e) {
                            throw e;
                        } catch (Throwable e) {
                            throw new IllegalStateException(e);
                        }
                    };
                    else invokeFunction = args -> {
                        if (args == null || args.length != parameterCount + 1) throw new ArrayIndexOutOfBoundsException("length mismatch");
                        MemoryHandle result = (MemoryHandle) args[0];
                        try {
                            Object[] arguments = new Object[parameterCount + 2];
                            arguments[0] = SegmentAllocator.slicingAllocator(FFMMethodFilters.handleToSegment(result));
                            arguments[1] = FFMLastErrno.segment();
                            System.arraycopy(args, 1, arguments, 2, parameterCount);
                            invoker.invoke(function, arguments);
                            return result;
                        } catch (ClassCastException | WrongMethodTypeException e) {
                            throw new IllegalArgumentException(e);
                        } catch (RuntimeException | Error e) {
                            throw e;
                        } catch (Throwable e) {
                            throw new IllegalStateException(e);
                        }
                    };
                }
                else if (parameterCount == 0) invokeFunction = args -> {
                    if (args == null || args.length != 1) throw new ArrayIndexOutOfBoundsException("length mismatch");
                    MemoryHandle result = (MemoryHandle) args[0];
                    try {
                        invoker.invoke(function, SegmentAllocator.slicingAllocator(FFMMethodFilters.handleToSegment(result)));
                        return result;
                    } catch (ClassCastException | WrongMethodTypeException e) {
                        throw new IllegalArgumentException(e);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
                else invokeFunction = args -> {
                    if (args == null || args.length != parameterCount + 1) throw new ArrayIndexOutOfBoundsException("length mismatch");
                    MemoryHandle result = (MemoryHandle) args[0];
                    try {
                        Object[] arguments = args.clone();
                        arguments[0] = SegmentAllocator.slicingAllocator(FFMMethodFilters.handleToSegment(result));
                        invoker.invoke(function, arguments);
                        return result;
                    } catch (ClassCastException | WrongMethodTypeException e) {
                        throw new IllegalArgumentException(e);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
            }
            else if (saveErrno) {
                if (parameterCount == 0) invokeFunction = args -> {
                    if (args != null && args.length != 0) throw new ArrayIndexOutOfBoundsException("length mismatch");
                    try {
                        return invoker.invoke(function, FFMLastErrno.segment());
                    } catch (ClassCastException | WrongMethodTypeException e) {
                        throw new IllegalArgumentException(e);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
                else invokeFunction = args -> {
                    if (args == null || args.length != parameterCount) throw new ArrayIndexOutOfBoundsException("length mismatch");
                    try {
                        Object[] arguments = new Object[parameterCount + 1];
                        arguments[0] = FFMLastErrno.segment();
                        System.arraycopy(args, 0, arguments, 1, parameterCount);
                        return invoker.invoke(function, arguments);
                    } catch (ClassCastException | WrongMethodTypeException e) {
                        throw new IllegalArgumentException(e);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
            }
            else {
                if (parameterCount == 0) invokeFunction = args -> {
                    if (args != null && args.length != 0) throw new ArrayIndexOutOfBoundsException("length mismatch");
                    try {
                        return invoker.invoke(function);
                    } catch (ClassCastException | WrongMethodTypeException e) {
                        throw new IllegalArgumentException(e);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
                else invokeFunction = args -> {
                    if (args == null || args.length != parameterCount) throw new ArrayIndexOutOfBoundsException("length mismatch");
                    try {
                        return invoker.invoke(function, args.clone());
                    } catch (ClassCastException | WrongMethodTypeException e) {
                        throw new IllegalArgumentException(e);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
            }
        }
    }

    public interface Invoker {
        Object invoke(MethodHandle methodHandle, Object... args) throws Throwable;
    }

    public static Invoker getInvoker(int parameterCount) {
        if (FFMUtil.PROXY_INTRINSICS) return FFMASMRuntime.generateInvoker(parameterCount);
        else return MethodHandle::invokeWithArguments;
    }

    @Override
    public long address() {
        return address;
    }

    @Override
    public List<ForeignType> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public ForeignType getReturnType() {
        return returnType;
    }

    @Override
    public int getFirstVarArgIndex() {
        return firstVarArgIndex;
    }

    @Override
    public boolean isDynCall() {
        return dyncall;
    }

    @Override
    public boolean isStdCall() {
        return false;
    }

    @Override
    public boolean isCritical() {
        return critical && !trivial;
    }

    @Override
    public boolean isTrivial() {
        return trivial;
    }

    @Override
    public boolean isSaveErrno() {
        return saveErrno;
    }

    @Override
    public void invokeVoid(Object... args) {
        invokeFunction.apply(args);
    }

    @Override
    public boolean invokeBoolean(Object... args) {
        return (boolean) invokeFunction.apply(args);
    }

    @Override
    public byte invokeInt8(Object... args) {
        return ((Number) invokeFunction.apply(args)).byteValue();
    }

    @Override
    public char invokeUTF16(Object... args) {
        return (char) invokeFunction.apply(args);
    }

    @Override
    public short invokeInt16(Object... args) {
        return ((Number) invokeFunction.apply(args)).shortValue();
    }

    @Override
    public int invokeInt32(Object... args) {
        return ((Number) invokeFunction.apply(args)).intValue();
    }

    @Override
    public long invokeInt64(Object... args) {
        return ((Number) invokeFunction.apply(args)).longValue();
    }

    @Override
    public float invokeFloat(Object... args) {
        return ((Number) invokeFunction.apply(args)).floatValue();
    }

    @Override
    public double invokeDouble(Object... args) {
        return ((Number) invokeFunction.apply(args)).doubleValue();
    }

    @Override
    public int invokeWChar(Object... args) {
        return ((Number) invokeFunction.apply(args)).intValue();
    }

    @Override
    public long invokeShort(Object... args) {
        return ((Number) invokeFunction.apply(args)).longValue();
    }

    @Override
    public long invokeInt(Object... args) {
        return ((Number) invokeFunction.apply(args)).longValue();
    }

    @Override
    public long invokeLong(Object... args) {
        return ((Number) invokeFunction.apply(args)).longValue();
    }

    @Override
    public long invokeSize(Object... args) {
        return ((Number) invokeFunction.apply(args)).longValue();
    }

    @Override
    public long invokeAddress(Object... args) {
        return ((Number) invokeFunction.apply(args)).longValue();
    }

    @Override
    public MemoryHandle invokeCompound(Object... args) {
        return (MemoryHandle) invokeFunction.apply(args);
    }

    @Override
    public Object invoke(Object... args) {
        return invokeFunction.apply(args);
    }

}
