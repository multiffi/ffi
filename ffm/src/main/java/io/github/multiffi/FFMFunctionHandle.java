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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
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
    private final List<ForeignType> parameterTypes;
    private final ForeignType returnType;
    private final MethodHandle methodHandle;
    private final Function<Object[], Object> invokeFunction;
    private final boolean critical;
    private final boolean trivial;
    private final boolean saveErrno;

    private static final Linker.Option[] EMPTY_OPTION_ARRAY = new Linker.Option[0];
    public FFMFunctionHandle(long address, int firstVarArg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        this.address = address;
        boolean saveErrno = false;
        boolean critical = false;
        boolean trivial = false;
        Set<Linker.Option> linkerOptions = new HashSet<>(3);
        for (CallOption option : options) {
            switch (option) {
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
        this.critical = critical;
        this.trivial = trivial;
        this.saveErrno = saveErrno;
        if (firstVarArg >= 0) linkerOptions.add(Linker.Option.firstVariadicArg(firstVarArg));
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
        this.methodHandle = methodHandle;
        Invoker invoker = getInvoker(parameterTypeList.size() + (saveErrno ? 1 : 0));
        if (addReturnMemoryParameter) {
            if (saveErrno) invokeFunction = args -> {
                if (args.length != parameterTypeList.size()) throw new IndexOutOfBoundsException("Array length mismatch");
                MemoryHandle result = (MemoryHandle) args[0];
                try {
                    Object[] arguments = new Object[args.length + 1];
                    arguments[0] = SegmentAllocator.slicingAllocator((MemorySegment) FFMMethodFilters.HANDLE_TO_SEGMENT.invokeExact(result));
                    arguments[1] = FFMLastErrno.segment();
                    System.arraycopy(args, 1, arguments, 2, args.length - 1);
                    invoker.invoke(FFMFunctionHandle.this.methodHandle, arguments);
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
                    invoker.invoke(FFMFunctionHandle.this.methodHandle, args);
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
                    return invoker.invoke(FFMFunctionHandle.this.methodHandle, arguments);
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
                    return invoker.invoke(FFMFunctionHandle.this.methodHandle, args);
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

    public MethodHandle getMethodHandle() {
        return methodHandle;
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
