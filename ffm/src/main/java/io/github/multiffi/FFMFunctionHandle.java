package io.github.multiffi;

import multiffi.CallOption;
import multiffi.ForeignType;
import multiffi.FunctionHandle;
import multiffi.MemoryHandle;
import multiffi.ScalarType;
import multiffi.StandardCallOption;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FFMFunctionHandle extends FunctionHandle {

    private final long address;
    private final int firstVariadicArgumentIndex;
    private final List<ForeignType> parameterTypes;
    private final ForeignType returnType;
    private final Function<Object[], Object> invokeFunction;
    private final boolean dyncall;
    private final boolean critical;
    private final boolean trivial;
    private final boolean saveErrno;

    private static final Linker.Option[] EMPTY_OPTION_ARRAY = new Linker.Option[0];
    public FFMFunctionHandle(long address, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        this.address = address;
        boolean dyncall = false;
        boolean saveErrno = false;
        boolean critical = false;
        boolean trivial = false;
        Set<Linker.Option> linkerOptions = new HashSet<>(3);
        for (CallOption option : options) {
            switch (option) {
                case StandardCallOption.DYNCALL:
                    dyncall = true;
                    break;
                case StandardCallOption.SAVE_ERRNO:
                    saveErrno = true;
                    linkerOptions.add(Linker.Option.captureCallState(FFMLastErrno.name()));
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
        if (critical) linkerOptions.add(Linker.Option.critical(!trivial));
        this.dyncall = dyncall;
        this.critical = critical;
        this.trivial = trivial;
        this.saveErrno = saveErrno;
        MemoryLayout returnLayout = returnType == null ? null : FFMUtil.toMemoryLayout(returnType);
        MemoryLayout[] parameterLayouts = new MemoryLayout[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i ++) {
            parameterLayouts[i] = FFMUtil.toMemoryLayout(parameterTypes[i]);
        }
        boolean addReturnMemoryParameter = returnType != null && returnType.isCompound();
        List<ForeignType> parameterTypeList = new ArrayList<>(parameterTypes.length + (addReturnMemoryParameter ? 1 : 0));
        parameterTypeList.addAll(Arrays.asList(parameterTypes));
        if (addReturnMemoryParameter) parameterTypeList.addFirst(returnType);
        this.returnType = returnType;
        this.parameterTypes = Collections.unmodifiableList(parameterTypeList);
        this.firstVariadicArgumentIndex = firstVararg >= 0 ? firstVararg : (dyncall ? parameterTypeList.size() : -1);
        if (firstVariadicArgumentIndex >= 0) linkerOptions.add(Linker.Option.firstVariadicArg(firstVariadicArgumentIndex));
        if (dyncall) {
            invokeFunction = args -> {
                if (args.length != parameterTypeList.size() + 1) throw new IllegalArgumentException("Array length mismatch");
                if (!args[args.length - 1].getClass().isArray())
                    throw new IllegalArgumentException("Last argument must be array as variadic arguments");
                Object varargs = args[args.length - 1];
                int varargsLength = Array.getLength(varargs);
                Object[] arguments = new Object[args.length - (FFMFunctionHandle.this.saveErrno ? 0 : 1) + varargsLength];
                System.arraycopy(args, 0, arguments, FFMFunctionHandle.this.saveErrno ? 1 : 0, args.length - 1);
                if (varargs instanceof Object[]) {
                    System.arraycopy(varargs, 0, arguments, args.length - (FFMFunctionHandle.this.saveErrno ? 0 : 1), varargsLength);
                }
                else {
                    int offset = args.length - (FFMFunctionHandle.this.saveErrno ? 0 : 1);
                    for (int i = 0; i < varargsLength; i ++) {
                        arguments[offset + i] = Array.get(varargs, i);
                    }
                }
                if (FFMFunctionHandle.this.saveErrno) arguments[addReturnMemoryParameter ? 1 : 0] = FFMLastErrno.segment();
                MemoryLayout[] newParameterLayouts = new MemoryLayout[parameterLayouts.length + varargsLength];
                System.arraycopy(parameterLayouts, 0, newParameterLayouts, 0, parameterLayouts.length);
                for (int i = 0; i < varargsLength; i ++) {
                    Object vararg = Array.get(varargs, i);
                    if (vararg instanceof Boolean) newParameterLayouts[parameterLayouts.length + i] = ValueLayout.JAVA_BOOLEAN;
                    else if (vararg instanceof Byte) newParameterLayouts[parameterLayouts.length + i] = ValueLayout.JAVA_BYTE;
                    else if (vararg instanceof Character) newParameterLayouts[parameterLayouts.length + i] = ValueLayout.JAVA_CHAR;
                    else if (vararg instanceof Short) newParameterLayouts[parameterLayouts.length + i] = ValueLayout.JAVA_SHORT;
                    else if (vararg instanceof Integer) newParameterLayouts[parameterLayouts.length + i] = ValueLayout.JAVA_INT;
                    else if (vararg instanceof Long) newParameterLayouts[parameterLayouts.length + i] = ValueLayout.JAVA_LONG;
                    else if (vararg instanceof Float) newParameterLayouts[parameterLayouts.length + i] = ValueLayout.JAVA_FLOAT;
                    else if (vararg instanceof Double) newParameterLayouts[parameterLayouts.length + i] = ValueLayout.JAVA_DOUBLE;
                    else if (vararg instanceof MemoryHandle memoryHandle) {
                        long size = memoryHandle.size();
                        if (size < 0) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(size));
                        newParameterLayouts[parameterLayouts.length + i] =
                                MemoryLayout.structLayout(MemoryLayout.sequenceLayout(size, ValueLayout.JAVA_BYTE));
                    }
                    else throw new IllegalArgumentException("Illegal argument: " + vararg);
                }
                MethodHandle methodHandle = FFMUtil.ABIHolder.LINKER.downcallHandle(MemorySegment.ofAddress(address), returnType == null ?
                        FunctionDescriptor.ofVoid(newParameterLayouts) : FunctionDescriptor.of(returnLayout, newParameterLayouts),
                        linkerOptions.toArray(EMPTY_OPTION_ARRAY));
                for (int i = (addReturnMemoryParameter ? 1 : 0); i < parameterTypeList.size(); i ++) {
                    ForeignType parameterType = parameterTypeList.get(i);
                    if (parameterType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortArgument(methodHandle, i + (FFMFunctionHandle.this.saveErrno ? 1 : 0), false);
                    else if (parameterType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntArgument(methodHandle, i + (FFMFunctionHandle.this.saveErrno ? 1 : 0), false);
                    else if (parameterType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongArgument(methodHandle, i + (FFMFunctionHandle.this.saveErrno ? 1 : 0), false);
                    else if (parameterType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterAddressArgument(methodHandle, i + (FFMFunctionHandle.this.saveErrno ? 1 : 0), false);
                    else if (parameterType == ScalarType.ADDRESS) methodHandle = MethodHandles
                            .filterArguments(methodHandle, i + (FFMFunctionHandle.this.saveErrno ? 1 : 0), FFMMethodFilters.INT64_TO_SEGMENT);
                    else if (parameterType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharArgument(methodHandle, i + (FFMFunctionHandle.this.saveErrno ? 1 : 0), false);
                    else if (parameterType.isCompound()) methodHandle = MethodHandles
                            .filterArguments(methodHandle, i + (FFMFunctionHandle.this.saveErrno ? 1 : 0), FFMMethodFilters.HANDLE_TO_SEGMENT);
                }
                if (returnType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortReturnValue(methodHandle, false);
                else if (returnType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntReturnValue(methodHandle, false);
                else if (returnType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongReturnValue(methodHandle, false);
                else if (returnType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterAddressReturnValue(methodHandle, false);
                else if (returnType == ScalarType.ADDRESS) methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMMethodFilters.SEGMENT_TO_INT64);
                else if (returnType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharReturnValue(methodHandle, false);
                Invoker invoker = getInvoker(arguments.length);
                Object result;
                try {
                    if (addReturnMemoryParameter) {
                        result = args[0];
                        arguments[0] = SegmentAllocator
                                .slicingAllocator((MemorySegment) FFMMethodFilters.HANDLE_TO_SEGMENT
                                        .invokeExact((MemoryHandle) result));
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
            MethodHandle methodHandle = FFMUtil.ABIHolder.LINKER.downcallHandle(MemorySegment.ofAddress(address), returnType == null ?
                            FunctionDescriptor.ofVoid(parameterLayouts) : FunctionDescriptor.of(returnLayout, parameterLayouts),
                    linkerOptions.toArray(EMPTY_OPTION_ARRAY));
            for (int i = (addReturnMemoryParameter ? 1 : 0); i < parameterTypeList.size(); i ++) {
                ForeignType parameterType = parameterTypeList.get(i);
                if (parameterType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterAddressArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType == ScalarType.ADDRESS) methodHandle = MethodHandles
                        .filterArguments(methodHandle, i + (saveErrno ? 1 : 0), FFMMethodFilters.INT64_TO_SEGMENT);
                else if (parameterType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharArgument(methodHandle, i + (saveErrno ? 1 : 0), false);
                else if (parameterType.isCompound()) methodHandle = MethodHandles
                        .filterArguments(methodHandle, i + (saveErrno ? 1 : 0), FFMMethodFilters.HANDLE_TO_SEGMENT);
            }
            if (returnType == ScalarType.SHORT) methodHandle = FFMMethodFilters.filterShortReturnValue(methodHandle, false);
            else if (returnType == ScalarType.INT) methodHandle = FFMMethodFilters.filterIntReturnValue(methodHandle, false);
            else if (returnType == ScalarType.LONG) methodHandle = FFMMethodFilters.filterLongReturnValue(methodHandle, false);
            else if (returnType == ScalarType.SIZE) methodHandle = FFMMethodFilters.filterAddressReturnValue(methodHandle, false);
            else if (returnType == ScalarType.ADDRESS) methodHandle = MethodHandles.filterReturnValue(methodHandle, FFMMethodFilters.SEGMENT_TO_INT64);
            else if (returnType == ScalarType.WCHAR) methodHandle = FFMMethodFilters.filterWCharReturnValue(methodHandle, false);
            Invoker invoker = getInvoker(parameterTypeList.size() + (saveErrno ? 1 : 0));
            MethodHandle function = methodHandle;
            if (addReturnMemoryParameter) {
                if (saveErrno) invokeFunction = args -> {
                    if (args.length != parameterTypeList.size()) throw new IndexOutOfBoundsException("Array length mismatch");
                    MemoryHandle result = (MemoryHandle) args[0];
                    try {
                        Object[] arguments = new Object[args.length + 1];
                        arguments[0] = SegmentAllocator.slicingAllocator((MemorySegment) FFMMethodFilters.HANDLE_TO_SEGMENT.invokeExact(result));
                        arguments[1] = FFMLastErrno.segment();
                        System.arraycopy(args, 1, arguments, 2, args.length - 1);
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
                else invokeFunction = args -> {
                    if (args.length != parameterTypeList.size()) throw new IndexOutOfBoundsException("Array length mismatch");
                    MemoryHandle result = (MemoryHandle) args[0];
                    try {
                        args[0] = SegmentAllocator.slicingAllocator((MemorySegment) FFMMethodFilters.HANDLE_TO_SEGMENT.invokeExact(result));
                        invoker.invoke(function, args);
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
            else {
                if (saveErrno) invokeFunction = args -> {
                    if (args.length != parameterTypeList.size()) throw new IndexOutOfBoundsException("Array length mismatch");
                    try {
                        Object[] arguments = new Object[args.length + 1];
                        arguments[0] = FFMLastErrno.segment();
                        System.arraycopy(args, 0, arguments, 1, args.length);
                        return invoker.invoke(function, arguments);
                    } catch (ClassCastException | WrongMethodTypeException e) {
                        throw new IllegalArgumentException(e);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                };
                else invokeFunction = args -> {
                    if (args.length != parameterTypeList.size()) throw new IndexOutOfBoundsException("Array length mismatch");
                    try {
                        return invoker.invoke(function, args);
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

    private interface Invoker {
        Object invoke(MethodHandle methodHandle, Object... args) throws Throwable;
    }

    private static final Map<Integer, Invoker> INVOKERS = new HashMap<>(255);

    private static final String[] INVOKER_EXCEPTIONS = new String[] { "java/lang/Throwable" };
    private static final String[] INVOKER_INTERFACES = new String[] { "io/github/multiffi/FFMFunctionHandle$Invoker" };
    private static Invoker getInvoker(int length) {
        if (length > 255) throw new IllegalArgumentException("parameter limit exceeded: " + length);
        if (!INVOKERS.containsKey(length)) synchronized (INVOKERS) {
            if (!INVOKERS.containsKey(length)) {
                ClassLoader classLoader = Invoker.class.getClassLoader();
                String proxyName = "io.github.multiffi.FFMFunctionHandle$Invoker$" + length;
                String proxyInternalName = proxyName.replace('.', '/');

                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

                classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PRIVATE | Opcodes.ACC_SUPER,
                        proxyInternalName, null, "java/lang/Object", INVOKER_INTERFACES);

                MethodVisitor classInit = classWriter.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                        "<clinit>", "()V", null, null);
                classInit.visitCode();
                classInit.visitInsn(Opcodes.RETURN);
                classInit.visitMaxs(0, 0);
                classInit.visitEnd();

                MethodVisitor objectInit = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                objectInit.visitCode();
                objectInit.visitVarInsn(Opcodes.ALOAD, 0);
                objectInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                objectInit.visitInsn(Opcodes.RETURN);
                objectInit.visitMaxs(0, 0);
                objectInit.visitEnd();

                MethodVisitor invoke = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "invoke",
                        "(Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/Object;", null, INVOKER_EXCEPTIONS);
                invoke.visitCode();
                for (int i = 0; i < length; i ++) {
                    invoke.visitVarInsn(Opcodes.ALOAD, 2);
                    invoke.visitIntInsn(Opcodes.SIPUSH, i);
                    invoke.visitInsn(Opcodes.AALOAD);
                    invoke.visitVarInsn(Opcodes.ASTORE, 3 + i);
                }
                invoke.visitVarInsn(Opcodes.ALOAD, 1);
                for (int i = 0; i < length; i ++) {
                    invoke.visitVarInsn(Opcodes.ALOAD, 3 + i);
                }
                invoke.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke",
                        "(" + "Ljava/lang/Object;".repeat(length) + ")Ljava/lang/Object;", false);
                invoke.visitInsn(Opcodes.ARETURN);
                invoke.visitMaxs(0, 0);
                invoke.visitEnd();
                try {
                    INVOKERS.put(length, (Invoker) FFMUtil.UnsafeHolder.IMPL_LOOKUP
                            .findConstructor(FFMUtil.defineClass(classLoader, proxyName, classWriter.toByteArray()),
                                    MethodType.methodType(void.class)).invoke());
                } catch (RuntimeException | Error ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        return INVOKERS.get(length);
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
        return firstVariadicArgumentIndex;
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
        return (byte) invokeFunction.apply(args);
    }

    @Override
    public char invokeUTF16(Object... args) {
        return (char) invokeFunction.apply(args);
    }

    @Override
    public short invokeInt16(Object... args) {
        return (short) invokeFunction.apply(args);
    }

    @Override
    public int invokeInt32(Object... args) {
        return (int) invokeFunction.apply(args);
    }

    @Override
    public long invokeInt64(Object... args) {
        return (long) invokeFunction.apply(args);
    }

    @Override
    public float invokeFloat(Object... args) {
        return (float) invokeFunction.apply(args);
    }

    @Override
    public double invokeDouble(Object... args) {
        return (double) invokeFunction.apply(args);
    }

    @Override
    public int invokeWChar(Object... args) {
        return (int) invokeFunction.apply(args);
    }

    @Override
    public long invokeShort(Object... args) {
        return (long) invokeFunction.apply(args);
    }

    @Override
    public long invokeInt(Object... args) {
        return (long) invokeFunction.apply(args);
    }

    @Override
    public long invokeLong(Object... args) {
        return (long) invokeFunction.apply(args);
    }

    @Override
    public long invokeAddress(Object... args) {
        return (long) invokeFunction.apply(args);
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
