package io.github.multiffi;

import multiffi.CallOption;
import multiffi.ErrnoException;
import multiffi.Foreign;
import multiffi.ForeignType;
import multiffi.FunctionPointer;
import multiffi.MemoryBlock;
import multiffi.ScalarType;
import multiffi.StandardCallOption;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.github.multiffi.FFMForeignProvider.ERRNO_THREAD_LOCAL;
import static io.github.multiffi.FFMForeignProvider.IS_WINDOWS;
import static io.github.multiffi.FFMForeignProvider.toMemoryLayout;

public class FFMFunctionPointer extends FunctionPointer {

    private static final Linker LINKER = Linker.nativeLinker();

    private final long address;
    private final List<ForeignType> parameterTypes;
    private final ForeignType returnType;
    private final MethodHandle methodHandle;
    private final boolean saveErrno;
    private final boolean throwErrno;

    private static final Linker.Option[] EMPTY_OPTION_ARRAY = new Linker.Option[0];
    public FFMFunctionPointer(long address, int firstVarArg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        this.address = address;
        boolean saveErrno = false;
        boolean throwErrno = false;
        boolean critical = false;
        Set<Linker.Option> linkerOptions = new HashSet<>(3);
        for (CallOption option : options) {
            switch (option) {
                case StandardCallOption.THROW_ERRNO:
                    throwErrno = true;
                case StandardCallOption.SAVE_ERRNO:
                    saveErrno = true;
                    linkerOptions.add(Linker.Option.captureCallState(IS_WINDOWS ? "GetLastError" : "errno"));
                    break;
                case ExtendedCallOption.CRITICAL:
                    critical = true;
                    linkerOptions.add(Linker.Option.critical(true));
                    break;
                case StandardCallOption.STDCALL:
                    break;
                default:
                    throw new IllegalArgumentException(option + " not supported");
            }
        }
        if (firstVarArg >= 0) linkerOptions.add(Linker.Option.firstVariadicArg(firstVarArg));
        this.saveErrno = saveErrno;
        this.throwErrno = throwErrno;
        if (saveErrno && critical) throw new IllegalArgumentException("SAVE_ERRNO or THROW_ERRNO can not be combined with CRITICAL");
        MemoryLayout returnLayout = returnType == ForeignType.VOID ? null : toMemoryLayout(returnType);
        MemoryLayout[] parameterLayouts = new MemoryLayout[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i ++) {
            parameterLayouts[i] = toMemoryLayout(parameterTypes[i]);
        }
        boolean addReturnMemoryParameter = returnType != null && returnType.isCompound();
        List<ForeignType> parameterTypeList = new ArrayList<>(parameterTypes.length + (addReturnMemoryParameter ? 1 : 0));
        parameterTypeList.addAll(Arrays.asList(parameterTypes));
        if (addReturnMemoryParameter) parameterTypeList.addFirst(returnType);
        this.returnType = returnType;
        this.parameterTypes = Collections.unmodifiableList(parameterTypeList);
        this.methodHandle = LINKER.downcallHandle(MemorySegment.ofAddress(address), returnType == ForeignType.VOID ?
                FunctionDescriptor.ofVoid(parameterLayouts) : FunctionDescriptor.of(returnLayout, parameterLayouts),
                linkerOptions.toArray(EMPTY_OPTION_ARRAY));
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
    public void invokeVoid(Object... args) {
        invokeScalar(args);
    }

    @Override
    public boolean invokeBoolean(Object... args) {
        return (boolean) invokeScalar(args);
    }

    @Override
    public byte invokeInt8(Object... args) {
        return (byte) invokeScalar(args);
    }

    @Override
    public char invokeUTF16(Object... args) {
        return (char) invokeScalar(args);
    }

    @Override
    public short invokeInt16(Object... args) {
        return (short) invokeScalar(args);
    }

    @Override
    public int invokeInt32(Object... args) {
        return (int) invokeScalar(args);
    }

    @Override
    public long invokeInt64(Object... args) {
        return (long) invokeScalar(args);
    }

    @Override
    public float invokeFloat(Object... args) {
        return (float) invokeScalar(args);
    }

    @Override
    public double invokeDouble(Object... args) {
        return (double) invokeScalar(args);
    }

    @Override
    public long invokeAddress(Object... args) {
        return (long) invokeScalar(args);
    }

    private void checkReplace(Object... args) {
        if (args.length != parameterTypes.size()) throw new IndexOutOfBoundsException("Array length mismatch");
        for (int i = 0; i < args.length; i ++) {
            Object arg = args[i];
            Objects.requireNonNull(arg);
            ForeignType type = parameterTypes.get(i);
            if (type == ScalarType.INT8) {
                if (arg instanceof MemoryBlock memoryBlock) args[i] = memoryBlock.getInt8(0);
            }
            else if (type == ScalarType.INT16) {
                if (arg instanceof MemoryBlock memoryBlock) args[i] = memoryBlock.getInt16(0);
            }
            else if (type == ScalarType.INT32) {
                if (arg instanceof MemoryBlock memoryBlock) args[i] = memoryBlock.getInt32(0);
            }
            else if (type == ScalarType.INT64) {
                if (arg instanceof MemoryBlock memoryBlock) args[i] = memoryBlock.getInt64(0);
            }
            else if (type == ScalarType.FLOAT) {
                if (arg instanceof MemoryBlock memoryBlock) args[i] = memoryBlock.getFloat(0);
            }
            else if (type == ScalarType.DOUBLE) {
                if (arg instanceof MemoryBlock memoryBlock) args[i] = memoryBlock.getDouble(0);
            }
            else if (type == ScalarType.BOOLEAN) {
                if (arg instanceof MemoryBlock memoryBlock) args[i] = memoryBlock.getBoolean(0);
            }
            else if (type == ScalarType.UTF16) {
                if (arg instanceof MemoryBlock memoryBlock) args[i] = memoryBlock.getUTF16(0);
            }
            else if (arg instanceof MemoryBlock memoryBlock) {
                MemorySegment memorySegment;
                if (memoryBlock.isDirect()) memorySegment = MemorySegment.ofAddress(memoryBlock.address())
                        .reinterpret(memoryBlock.isBounded() ? memoryBlock.size() : Foreign.addressSize() == 8 ? Long.MAX_VALUE : Integer.MAX_VALUE);
                else {
                    Object array = memoryBlock.array();
                    memorySegment = switch (array) {
                        case byte[] byteArray -> MemorySegment.ofArray(byteArray);
                        case short[] shortArray -> MemorySegment.ofArray(shortArray);
                        case int[] intArray -> MemorySegment.ofArray(intArray);
                        case long[] longArray -> MemorySegment.ofArray(longArray);
                        case float[] floatArray -> MemorySegment.ofArray(floatArray);
                        case double[] doubleArray -> MemorySegment.ofArray(doubleArray);
                        case char[] charArray -> MemorySegment.ofArray(charArray);
                        case null, default -> throw new IllegalStateException("Unexpected exception");
                    };
                }
                args[i] = memorySegment;
            }
        }
    }

    @Override
    public MemoryBlock invokeCompound(Object... args) {
        MemoryBlock result = (MemoryBlock) args[0];
        checkReplace(args);
        try {
            MemorySegment compound = (MemorySegment) args[0];
            Object[] arguments = new Object[args.length + 1];
            arguments[0] = ERRNO_THREAD_LOCAL.get();
            arguments[1] = SegmentAllocator.slicingAllocator(compound);
            System.arraycopy(args, 1, arguments, 2, args.length - 1);
            if (saveErrno) {
                methodHandle.invokeWithArguments(arguments);
                if (throwErrno) throw new ErrnoException();
            }
            else methodHandle.invokeWithArguments(arguments);
            return result;
        } catch (ClassCastException | WrongMethodTypeException e) {
            throw new IllegalArgumentException(e);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    private Object invokeScalar(Object... args) {
        checkReplace(args);
        try {
            if (!saveErrno) return methodHandle.invokeWithArguments(args);
            Object result;
            Object[] arguments = new Object[args.length + 1];
            arguments[0] = ERRNO_THREAD_LOCAL.get();
            System.arraycopy(args, 0, arguments, 1, args.length);
            result = methodHandle.invokeWithArguments(arguments);
            if (throwErrno) throw new ErrnoException();
            return result;
        } catch (ClassCastException | WrongMethodTypeException e) {
            throw new IllegalArgumentException(e);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object invoke(Object... args) {
        if (returnType == null || returnType.isScalar()) return invokeScalar(args);
        else return invokeCompound(args);
    }

}
