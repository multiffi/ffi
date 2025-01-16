package io.github.multiffi.ffi;

import multiffi.ffi.CallOption;
import multiffi.ffi.Foreign;
import multiffi.ffi.ForeignType;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
import multiffi.ffi.FunctionOptionVisitor;
import multiffi.ffi.StandardCallOption;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class FFMASMRuntime {

    private FFMASMRuntime() {
        throw new AssertionError("No io.github.multiffi.ffi.FFMASMRuntime instances for you!");
    }

    private static void checkProxyClasses(ClassLoader classLoader, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            if (!clazz.isInterface()) throw new IllegalArgumentException(clazz.getName() + " is not an interface");
            if (clazz.isHidden()) throw new IllegalArgumentException(clazz.getName() + " is a hidden interface");
            if (clazz.isSealed()) throw new IllegalArgumentException(clazz.getName() + " is a sealed interface");
            checkVisible(classLoader, clazz);
            for (Method method : clazz.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    for (Class<?> parameterType : method.getParameterTypes()) {
                        while (parameterType.isArray()) parameterType = parameterType.getComponentType();
                        if (!parameterType.isPrimitive()) checkVisible(classLoader, parameterType);
                    }
                    for (Class<?> exceptionType : method.getExceptionTypes()) {
                        while (exceptionType.isArray()) exceptionType = exceptionType.getComponentType();
                        if (!exceptionType.isPrimitive()) checkVisible(classLoader, exceptionType);
                    }
                    Class<?> returnType = method.getReturnType();
                    while (returnType.isArray()) returnType = returnType.getComponentType();
                    if (!returnType.isPrimitive()) checkVisible(classLoader, returnType);
                }
            }
        }
    }

    private static void checkVisible(ClassLoader classLoader, Class<?> clazz) {
        Class<?> type = null;
        try {
            type = Class.forName(clazz.getName(), false, classLoader);
        }
        catch (ClassNotFoundException ignored) {
        }
        if (type != clazz) throw new IllegalArgumentException(clazz.getName() +
                " referenced from a method is not visible from class loader: " + classLoader);
    }

    private static final AtomicLong nextSerialNumber = new AtomicLong();
    public static Object generateProxy(ClassLoader classLoader, Class<?>[] classes, FunctionOptionVisitor functionOptionVisitor) {
        if (classes == null || classes.length == 0) return null;
        else if (classes.length > 65535)
            throw new IllegalArgumentException("interface limit exceeded: " + classes.length);
        checkProxyClasses(classLoader, classes);

        String proxyName = "multiffi.ffi.ffm.Proxy$" + nextSerialNumber.getAndIncrement();
        String proxyInternalName = proxyName.replace('.', '/');
        String[] classInternalNames = new String[classes.length];
        for (int i = 0; i < classes.length; i ++) {
            classInternalNames[i] = Type.getInternalName(classes[i]);
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PRIVATE | Opcodes.ACC_SUPER,
                proxyInternalName, null, "java/lang/Object", classInternalNames);

        MethodVisitor objectInit = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        objectInit.visitCode();
        objectInit.visitVarInsn(Opcodes.ALOAD, 0);
        objectInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        objectInit.visitInsn(Opcodes.RETURN);
        objectInit.visitMaxs(0, 0);
        objectInit.visitEnd();

        MethodVisitor classInit = classWriter.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "<clinit>", "()V", null, null);
        classInit.visitCode();

        for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                if (method.isDefault() || method.getDeclaringClass() == Object.class || Modifier.isStatic(method.getModifiers())) continue;
                String methodName = method.getName();
                String methodFieldName = "function" + Integer.toHexString(method.hashCode());
                boolean dyncall = false;
                boolean saveErrno = false;
                boolean critical = false;
                boolean trivial = false;
                CallOption[] options = functionOptionVisitor.visitCallOptions(method);
                if (options != null) {
                    for (CallOption option : options) {
                        switch (option) {
                            case StandardCallOption.DYNCALL:
                                dyncall = true;
                                break;
                            case StandardCallOption.SAVE_ERRNO:
                                saveErrno = true;
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
                }
                int firstVarArgIndex = functionOptionVisitor.visitFirstVarArgIndex(method);
                long address = functionOptionVisitor.visitAddress(method);

                classWriter.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                        methodFieldName, dyncall ? "Lmultiffi/ffi/FunctionHandle;" : "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();
                MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, methodName, Type.getMethodDescriptor(method),
                        null, null);
                methodVisitor.visitCode();
                ForeignType[] parameterForeignTypes = functionOptionVisitor.visitParameterTypes(method);
                if (parameterForeignTypes == null || parameterForeignTypes.length == 1 && parameterForeignTypes[0] == null)
                    parameterForeignTypes = Util.EMPTY_FOREIGN_TYPE_ARRAY;
                else parameterForeignTypes = parameterForeignTypes.clone();
                ForeignType returnForeignType = functionOptionVisitor.visitReturnType(method);
                boolean addReturnMemoryParameter = returnForeignType != null && returnForeignType.isCompound();
                if (dyncall) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length - 1 != parameterForeignTypes.length + (addReturnMemoryParameter ? 1 : 0))
                        throw new ArrayIndexOutOfBoundsException("length mismatch");
                    if (!parameterTypes[parameterTypes.length - 1].isArray())
                        throw new IllegalArgumentException("Last argument must be array as variadic arguments");
                    Class<?> returnType = method.getReturnType();

                    visitLdcInsn(classInit, parameterForeignTypes.length);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "multiffi/ffi/ForeignType");
                    classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    for (int i = 0; i < parameterForeignTypes.length; i ++) {
                        ForeignType parameterForeignType = parameterForeignTypes[i];
                        Class<?> parameterType = parameterTypes[(addReturnMemoryParameter ? 1 : 0) + i];
                        if (parameterForeignType.isCompound()) {
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/ScalarType", "INT8", "Lmultiffi/ffi/ScalarType;");
                            visitLdcInsn(classInit, parameterForeignType.size());
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "multiffi/ffi/CompoundType",
                                    "ofArray", "(Lmultiffi/ffi/ForeignType;J)Lmultiffi/ffi/CompoundType;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                        }
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        visitLdcInsn(classInit, i);

                        if (parameterForeignType.isCompound()) classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        else dumpForeignType(classInit, parameterType, parameterForeignType);
                        classInit.visitInsn(Opcodes.AASTORE);
                    }

                    int optionCount = 1;
                    if (critical) optionCount ++;
                    if (trivial) optionCount ++;
                    if (saveErrno) optionCount ++;
                    visitLdcInsn(classInit, optionCount);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "multiffi/ffi/CallOption");
                    classInit.visitVarInsn(Opcodes.ASTORE, 2);
                    classInit.visitVarInsn(Opcodes.ALOAD, 2);
                    classInit.visitInsn(Opcodes.ICONST_0);
                    classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/StandardCallOption",
                            "DYNCALL", "Lmultiffi/ffi/StandardCallOption;");
                    classInit.visitInsn(Opcodes.AASTORE);
                    int optionIndex = 1;
                    if (critical) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        visitLdcInsn(classInit, optionIndex ++);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/StandardCallOption",
                                "CRITICAL", "Lmultiffi/ffi/StandardCallOption;");
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    if (trivial) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        visitLdcInsn(classInit, optionIndex ++);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/StandardCallOption",
                                "TRIVIAL", "Lmultiffi/ffi/StandardCallOption;");
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    if (saveErrno) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        visitLdcInsn(classInit, optionIndex);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/StandardCallOption",
                                "SAVE_ERRNO", "Lmultiffi/ffi/StandardCallOption;");
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    if (returnForeignType != null && returnForeignType.isCompound()) {
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/ScalarType", "INT8", "Lmultiffi/ffi/ScalarType;");
                        visitLdcInsn(classInit, returnForeignType.size());
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "multiffi/ffi/CompoundType",
                                "ofArray", "(Lmultiffi/ffi/ForeignType;J)Lmultiffi/ffi/CompoundType;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 3);
                    }

                    classInit.visitTypeInsn(Opcodes.NEW, "io/github/multiffi/ffi/FFMFunctionHandle");
                    classInit.visitInsn(Opcodes.DUP);
                    visitLdcInsn(classInit, address);
                    visitLdcInsn(classInit, firstVarArgIndex);

                    if (returnForeignType != null && returnForeignType.isCompound()) classInit.visitVarInsn(Opcodes.ALOAD, 3);
                    else dumpForeignType(classInit, returnType, returnForeignType);
                    classInit.visitVarInsn(Opcodes.ALOAD, 1);
                    classInit.visitVarInsn(Opcodes.ALOAD, 2);
                    classInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "io/github/multiffi/ffi/FFMFunctionHandle", "<init>",
                            "(JILmultiffi/ffi/ForeignType;[Lmultiffi/ffi/ForeignType;[Lmultiffi/ffi/CallOption;)V", false);
                    classInit.visitFieldInsn(Opcodes.PUTSTATIC, proxyInternalName, methodFieldName,
                            "Lmultiffi/ffi/FunctionHandle;");

                    int storeIndex = parameterTypes.length + 1;
                    for (Class<?> parameterType : parameterTypes) {
                        if (parameterType == long.class || parameterType == double.class) storeIndex ++;
                    }
                    visitLdcInsn(methodVisitor, parameterTypes.length);
                    methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                    methodVisitor.visitVarInsn(Opcodes.ASTORE, storeIndex);

                    int index = 0;
                    for (int i = 0; i < parameterTypes.length; i ++) {
                        Class<?> parameterType = parameterTypes[i];
                        if (parameterType.isPrimitive()) {
                            dumpLoadOpcode(methodVisitor, parameterType, 1 + index ++);
                            if (parameterType == boolean.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf",
                                        "(Z)Ljava/lang/Boolean;", false);
                            else if (parameterType == byte.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf",
                                        "(B)Ljava/lang/Byte;", false);
                            else if (parameterType == char.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf",
                                        "(C)Ljava/lang/Character;", false);
                            else if (parameterType == short.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf",
                                        "(S)Ljava/lang/Short;", false);
                            else if (parameterType == int.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf",
                                        "(I)Ljava/lang/Integer;", false);
                            else if (parameterType == long.class) {
                                index ++;
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf",
                                        "(J)Ljava/lang/Long;", false);
                            }
                            else if (parameterType == float.class)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf",
                                        "(F)Ljava/lang/Float;", false);
                            else if (parameterType == double.class) {
                                index ++;
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf",
                                        "(D)Ljava/lang/Double;", false);
                            }
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, storeIndex + 1);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, storeIndex);
                            visitLdcInsn(methodVisitor, i);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, storeIndex + 1);
                        }
                        else {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, storeIndex);
                            visitLdcInsn(methodVisitor, i);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1 + index ++);
                        }
                        methodVisitor.visitInsn(Opcodes.AASTORE);
                    }

                    methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, proxyInternalName, methodFieldName, "Lmultiffi/ffi/FunctionHandle;");
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, storeIndex);
                    if (returnForeignType == null)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeVoid",
                                "([Ljava/lang/Object;)V", false);
                    else if (returnForeignType == ScalarType.BOOLEAN)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeBoolean",
                                "([Ljava/lang/Object;)Z", false);
                    else if (returnForeignType == ScalarType.INT8)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeInt8",
                                "([Ljava/lang/Object;)B", false);
                    else if (returnForeignType == ScalarType.CHAR)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeChar",
                                "([Ljava/lang/Object;)B", false);
                    else if (returnForeignType == ScalarType.UTF16)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeUTF16",
                                "([Ljava/lang/Object;)C", false);
                    else if (returnForeignType == ScalarType.INT16)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeInt16",
                                "([Ljava/lang/Object;)S", false);
                    else if (returnForeignType == ScalarType.WCHAR)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeWChar",
                                "([Ljava/lang/Object;)I", false);
                    else if (returnForeignType == ScalarType.INT32)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeInt32",
                                "([Ljava/lang/Object;)I", false);
                    else if (returnForeignType == ScalarType.INT64)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeInt64",
                                "([Ljava/lang/Object;)J", false);
                    else if (returnForeignType == ScalarType.SHORT)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeShort",
                                "([Ljava/lang/Object;)J", false);
                    else if (returnForeignType == ScalarType.INT)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeInt",
                                "([Ljava/lang/Object;)J", false);
                    else if (returnForeignType == ScalarType.LONG)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeLong",
                                "([Ljava/lang/Object;)J", false);
                    else if (returnForeignType == ScalarType.ADDRESS)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeAddress",
                                "([Ljava/lang/Object;)J", false);
                    else if (returnForeignType == ScalarType.FLOAT)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeFloat",
                                "([Ljava/lang/Object;)F", false);
                    else if (returnForeignType == ScalarType.DOUBLE)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeDouble",
                                "([Ljava/lang/Object;)D", false);
                    else methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "multiffi/ffi/FunctionHandle", "invokeCompound",
                                "([Ljava/lang/Object;)Lmultiffi/ffi/MemoryHandle;", false);
                    dumpReturnOpcode(methodVisitor, returnType);
                    methodVisitor.visitMaxs(0, 0);
                    methodVisitor.visitEnd();
                }
                else {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length != parameterForeignTypes.length + (addReturnMemoryParameter ? 1 : 0))
                        throw new ArrayIndexOutOfBoundsException("length mismatch");
                    Class<?> returnType = method.getReturnType();

                    int methodMaxLocals = parameterTypes.length + 1;
                    for (Class<?> parameterType : parameterTypes) {
                        if (parameterType == long.class || parameterType == double.class) methodMaxLocals ++;
                    }

                    if (addReturnMemoryParameter) {
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/multiffi/ffi/FFMMethodFilters",
                                "handleToSegment", "(Lmultiffi/ffi/MemoryHandle;)Ljava/lang/foreign/MemorySegment;", false);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/SegmentAllocator",
                                "slicingAllocator", "(Ljava/lang/foreign/MemorySegment;)Ljava/lang/foreign/SegmentAllocator;", true);
                        methodVisitor.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    if (saveErrno) {
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/multiffi/ffi/FFMLastErrno", "segment",
                                "()Ljava/lang/foreign/MemorySegment;", false);
                        methodVisitor.visitVarInsn(Opcodes.ASTORE, methodMaxLocals);
                    }
                    methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, proxyInternalName, methodFieldName, "Ljava/lang/invoke/MethodHandle;");
                    if (addReturnMemoryParameter) methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                    if (saveErrno) methodVisitor.visitVarInsn(Opcodes.ALOAD, methodMaxLocals);

                    visitLdcInsn(classInit, address);
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemorySegment", "ofAddress",
                            "(J)Ljava/lang/foreign/MemorySegment;", true);
                    classInit.visitVarInsn(Opcodes.ASTORE, 1);

                    visitLdcInsn(classInit, parameterForeignTypes.length);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/foreign/MemoryLayout");
                    classInit.visitVarInsn(Opcodes.ASTORE, 2);
                    int index = addReturnMemoryParameter ? 1 : 0;
                    for (int i = 0; i < parameterForeignTypes.length; i ++) {
                        ForeignType parameterForeignType = parameterForeignTypes[i];
                        Class<?> parameterType = parameterTypes[i + (addReturnMemoryParameter ? 1 : 0)];
                        if (parameterForeignType.isCompound()) {
                            if (MemoryHandle.class != parameterType) throw new IllegalArgumentException("Illegal mapping type; expected class MemoryHandle");
                            visitLdcInsn(classInit, parameterForeignType.size());
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_BYTE",
                                    "Ljava/lang/foreign/ValueLayout$OfByte;");
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemoryLayout",
                                    "sequenceLayout", "(JLjava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/SequenceLayout;", true);
                            classInit.visitVarInsn(Opcodes.ASTORE, 3);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/foreign/MemoryLayout");
                            classInit.visitVarInsn(Opcodes.ASTORE, 4);
                            classInit.visitVarInsn(Opcodes.ALOAD, 4);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitVarInsn(Opcodes.ALOAD, 3);
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 4);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemoryLayout",
                                    "structLayout", "([Ljava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/StructLayout;", true);
                            classInit.visitVarInsn(Opcodes.ASTORE, 3);
                        }
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        visitLdcInsn(classInit, i);
                        if (parameterForeignType.isCompound()) classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        else dumpMemoryLayout(classInit, parameterType, parameterForeignType);
                        classInit.visitInsn(Opcodes.AASTORE);
                        dumpLoadOpcode(methodVisitor, parameterType, 1 + index);
                        if (parameterType == long.class || parameterType == double.class) index ++;
                        index ++;
                    }
                    if (returnForeignType == null) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/FunctionDescriptor", "ofVoid",
                                "([Ljava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/FunctionDescriptor;",
                                true);
                    }
                    else {
                        if (returnForeignType.isCompound()) {
                            if (MemoryHandle.class != returnType) throw new IllegalArgumentException("Illegal mapping type; expected class MemoryHandle");
                            visitLdcInsn(classInit, returnForeignType.size());
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_BYTE",
                                    "Ljava/lang/foreign/ValueLayout$OfByte;");
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemoryLayout",
                                    "sequenceLayout", "(JLjava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/SequenceLayout;", true);
                            classInit.visitVarInsn(Opcodes.ASTORE, 4);
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/foreign/MemoryLayout");
                            classInit.visitVarInsn(Opcodes.ASTORE, 5);
                            classInit.visitVarInsn(Opcodes.ALOAD, 5);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitVarInsn(Opcodes.ALOAD, 4);
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 5);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/MemoryLayout",
                                    "structLayout", "([Ljava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/StructLayout;", true);
                        }
                        else dumpMemoryLayout(classInit, returnType, returnForeignType);
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/FunctionDescriptor", "of",
                                "(Ljava/lang/foreign/MemoryLayout;[Ljava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/FunctionDescriptor;",
                                true);
                    }
                    classInit.visitVarInsn(Opcodes.ASTORE, 2);
                    classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker", "nativeLinker",
                            "()Ljava/lang/foreign/Linker;", true);
                    int linkerOptionsLength = 0;
                    if (saveErrno) linkerOptionsLength ++;
                    if (critical) linkerOptionsLength ++;
                    if (firstVarArgIndex != -1) linkerOptionsLength ++;
                    visitLdcInsn(classInit, linkerOptionsLength);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/foreign/Linker$Option");
                    classInit.visitVarInsn(Opcodes.ASTORE, 3);
                    int linkerOptionsIndex = 0;
                    if (saveErrno) {
                        classInit.visitInsn(Opcodes.ICONST_1);
                        classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
                        classInit.visitVarInsn(Opcodes.ASTORE, 4);
                        classInit.visitVarInsn(Opcodes.ALOAD, 4);
                        classInit.visitInsn(Opcodes.ICONST_0);
                        visitLdcInsn(classInit, FFMLastErrno.name());
                        classInit.visitInsn(Opcodes.AASTORE);
                        classInit.visitVarInsn(Opcodes.ALOAD, 4);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker$Option",
                                "captureCallState", "([Ljava/lang/String;)Ljava/lang/foreign/Linker$Option;", true);
                        classInit.visitVarInsn(Opcodes.ASTORE, 4);
                        classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        visitLdcInsn(classInit, linkerOptionsIndex ++);
                        classInit.visitVarInsn(Opcodes.ALOAD, 4);
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    if (critical) {
                        visitLdcInsn(classInit, !trivial);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker$Option",
                                "critical", "(Z)Ljava/lang/foreign/Linker$Option;", true);
                        classInit.visitVarInsn(Opcodes.ASTORE, 4);
                        classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        visitLdcInsn(classInit, linkerOptionsIndex ++);
                        classInit.visitVarInsn(Opcodes.ALOAD, 4);
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    if (firstVarArgIndex != -1) {
                        visitLdcInsn(classInit, firstVarArgIndex);
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/foreign/Linker$Option",
                                "firstVariadicArg", "(I)Ljava/lang/foreign/Linker$Option;", true);
                        classInit.visitVarInsn(Opcodes.ASTORE, 4);
                        classInit.visitVarInsn(Opcodes.ALOAD, 3);
                        visitLdcInsn(classInit, linkerOptionsIndex);
                        classInit.visitVarInsn(Opcodes.ALOAD, 4);
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    classInit.visitVarInsn(Opcodes.ALOAD, 1);
                    classInit.visitVarInsn(Opcodes.ALOAD, 2);
                    classInit.visitVarInsn(Opcodes.ALOAD, 3);
                    classInit.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/foreign/Linker", "downcallHandle",
                            "(Ljava/lang/foreign/MemorySegment;Ljava/lang/foreign/FunctionDescriptor;[Ljava/lang/foreign/Linker$Option;)Ljava/lang/invoke/MethodHandle;",
                            true);
                    classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    for (int i = 0; i < parameterForeignTypes.length; i ++) {
                        ForeignType parameterForeignType = parameterForeignTypes[i];
                        if (parameterForeignType == ScalarType.SHORT && Foreign.shortSize() == 2) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "SHORT_TO_INT16",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            visitLdcInsn(classInit, i + (addReturnMemoryParameter ? 1 : 0) + (saveErrno ? 1 : 0));
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.INT && Foreign.intSize() == 4) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT_TO_INT32",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            visitLdcInsn(classInit, i + (addReturnMemoryParameter ? 1 : 0) + (saveErrno ? 1 : 0));
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.LONG && Foreign.longSize() == 4) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "LONG_TO_INT32",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            visitLdcInsn(classInit, i + (addReturnMemoryParameter ? 1 : 0) + (saveErrno ? 1 : 0));
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.SIZE && Foreign.diffSize() == 4) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "ADDRESS_TO_INT32",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            visitLdcInsn(classInit, i + (addReturnMemoryParameter ? 1 : 0) + (saveErrno ? 1 : 0));
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.ADDRESS) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT64_TO_SEGMENT",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            visitLdcInsn(classInit, i + (addReturnMemoryParameter ? 1 : 0) + (saveErrno ? 1 : 0));
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.WCHAR && Foreign.wcharSize() == 2) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "WCHAR_TO_UTF16",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            visitLdcInsn(classInit, i + (addReturnMemoryParameter ? 1 : 0) + (saveErrno ? 1 : 0));
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType == ScalarType.BOOLEAN) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters",
                                    Foreign.addressSize() == 4L ? "BOOLEAN_TO_INT32" : "BOOLEAN_TO_INT64",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            visitLdcInsn(classInit, i + (addReturnMemoryParameter ? 1 : 0) + (saveErrno ? 1 : 0));
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                        else if (parameterForeignType.isCompound()) {
                            classInit.visitInsn(Opcodes.ICONST_1);
                            classInit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/invoke/MethodHandle");
                            classInit.visitVarInsn(Opcodes.ASTORE, 2);
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitInsn(Opcodes.ICONST_0);
                            classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "HANDLE_TO_SEGMENT",
                                    "Ljava/lang/invoke/MethodHandle;");
                            classInit.visitInsn(Opcodes.AASTORE);
                            classInit.visitVarInsn(Opcodes.ALOAD, 1);
                            visitLdcInsn(classInit, i + (addReturnMemoryParameter ? 1 : 0) + (saveErrno ? 1 : 0));
                            classInit.visitVarInsn(Opcodes.ALOAD, 2);
                            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterArguments",
                                    "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                            classInit.visitVarInsn(Opcodes.ASTORE, 1);
                        }
                    }
                    if (returnForeignType == ScalarType.SHORT && Foreign.shortSize() == 2) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT16_TO_INT64",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.INT && Foreign.intSize() == 4) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT32_TO_INT64",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.LONG && Foreign.longSize() == 4) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT32_TO_INT64",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.SIZE && Foreign.diffSize() == 4) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "INT32_TO_INT64",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.ADDRESS) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "SEGMENT_TO_INT64",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.WCHAR && Foreign.shortSize() == 2) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "UTF16_TO_INT32",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType == ScalarType.BOOLEAN) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters",
                                Foreign.addressSize() == 4L ? "INT32_TO_BOOLEAN" : "INT64_TO_BOOLEAN",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    else if (returnForeignType != null && returnForeignType.isCompound()) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 1);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "io/github/multiffi/ffi/FFMMethodFilters", "SEGMENT_TO_HANDLE",
                                "Ljava/lang/invoke/MethodHandle;");
                        classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "filterReturnValue",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/MethodHandle;", false);
                        classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    }
                    classInit.visitVarInsn(Opcodes.ALOAD, 1);
                    classInit.visitFieldInsn(Opcodes.PUTSTATIC, proxyInternalName, methodFieldName,
                            "Ljava/lang/invoke/MethodHandle;");
                    methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                            getMethodDescriptor(method, saveErrno, addReturnMemoryParameter), false);
                    dumpReturnOpcode(methodVisitor, returnType);
                    methodVisitor.visitMaxs(0, 0);
                    methodVisitor.visitEnd();
                }
            }
        }
        classInit.visitInsn(Opcodes.RETURN);
        classInit.visitMaxs(0, 0);
        classInit.visitEnd();

        classWriter.visitEnd();
        try {
            return FFMUtil.IMPL_LOOKUP.findConstructor(FFMUtil.defineClass(classLoader, proxyName, classWriter.toByteArray()),
                    MethodType.methodType(void.class)).invoke();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static void visitLdcInsn(MethodVisitor methodVisitor, Object value) {
        switch (value) {
            case Long lVal -> {
                if (lVal == 0) methodVisitor.visitInsn(Opcodes.LCONST_0);
                else if (lVal == 1) methodVisitor.visitInsn(Opcodes.LCONST_1);
                else methodVisitor.visitLdcInsn(value);
            }
            case Float fVal -> {
                if (fVal == 0) methodVisitor.visitInsn(Opcodes.FCONST_0);
                else if (fVal == 1) methodVisitor.visitInsn(Opcodes.FCONST_1);
                else methodVisitor.visitLdcInsn(value);
            }
            case Double dVal -> {
                if (dVal == 0) methodVisitor.visitInsn(Opcodes.DCONST_0);
                else if (dVal == 1) methodVisitor.visitInsn(Opcodes.DCONST_1);
                else methodVisitor.visitLdcInsn(value);
            }
            case Number number -> {
                int iVal = number.intValue();
                switch (iVal) {
                    case 0:
                        methodVisitor.visitInsn(Opcodes.ICONST_0);
                        break;
                    case 1:
                        methodVisitor.visitInsn(Opcodes.ICONST_1);
                        break;
                    case 2:
                        methodVisitor.visitInsn(Opcodes.ICONST_2);
                        break;
                    case 3:
                        methodVisitor.visitInsn(Opcodes.ICONST_3);
                        break;
                    case 4:
                        methodVisitor.visitInsn(Opcodes.ICONST_4);
                        break;
                    case 5:
                        methodVisitor.visitInsn(Opcodes.ICONST_5);
                        break;
                    case -1:
                        methodVisitor.visitInsn(Opcodes.ICONST_M1);
                        break;
                    default:
                        if (iVal >= Byte.MIN_VALUE && iVal <= Byte.MAX_VALUE)
                            methodVisitor.visitIntInsn(Opcodes.BIPUSH, iVal);
                        else if (iVal >= Short.MIN_VALUE && iVal <= Short.MAX_VALUE)
                            methodVisitor.visitIntInsn(Opcodes.SIPUSH, iVal);
                        else methodVisitor.visitLdcInsn(value);
                        break;
                }
            }
            case null -> methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            default -> methodVisitor.visitLdcInsn(value);
        }
    }

    private static void dumpMemoryLayout(MethodVisitor methodVisitor, Class<?> clazz, ForeignType type) {
        if (type == ScalarType.INT8 || type == ScalarType.CHAR) {
            if (clazz != byte.class) throw new IllegalArgumentException("Illegal mapping type; expected byte");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_BYTE",
                    "Ljava/lang/foreign/ValueLayout$OfByte;");
        }
        else if (type == ScalarType.INT16) {
            if (clazz != short.class) throw new IllegalArgumentException("Illegal mapping type; expected short");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_SHORT",
                    "Ljava/lang/foreign/ValueLayout$OfShort;");
        }
        else if (type == ScalarType.INT32) {
            if (clazz != int.class) throw new IllegalArgumentException("Illegal mapping type; expected int");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                    "Ljava/lang/foreign/ValueLayout$OfInt;");
        }
        else if (type == ScalarType.INT64) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                    "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.WCHAR) {
            if (clazz != int.class) throw new IllegalArgumentException("Illegal mapping type; expected int");
            if (Foreign.wcharSize() == 2)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_CHAR",
                        "Ljava/lang/foreign/ValueLayout$OfChar;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                    "Ljava/lang/foreign/ValueLayout$OfInt;");
        }
        else if (type == ScalarType.SHORT) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            if (Foreign.shortSize() == 2)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_SHORT",
                        "Ljava/lang/foreign/ValueLayout$OfShort;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                    "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.INT) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            if (Foreign.intSize() == 4)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                        "Ljava/lang/foreign/ValueLayout$OfInt;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                    "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.LONG) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            if (Foreign.longSize() == 4)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                        "Ljava/lang/foreign/ValueLayout$OfInt;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                    "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.SIZE) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            if (Foreign.diffSize() == 4)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                        "Ljava/lang/foreign/ValueLayout$OfInt;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                    "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.FLOAT) {
            if (clazz != float.class) throw new IllegalArgumentException("Illegal mapping type; expected float");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_FLOAT",
                    "Ljava/lang/foreign/ValueLayout$OfFloat;");
        }
        else if (type == ScalarType.DOUBLE) {
            if (clazz != double.class) throw new IllegalArgumentException("Illegal mapping type; expected double");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_DOUBLE",
                    "Ljava/lang/foreign/ValueLayout$OfDouble;");
        }
        else if (type == ScalarType.BOOLEAN) {
            if (clazz != boolean.class) throw new IllegalArgumentException("Illegal mapping type; expected boolean");
            if (Foreign.addressSize() == 4)
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT",
                        "Ljava/lang/foreign/ValueLayout$OfInt;");
            else methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_LONG",
                    "Ljava/lang/foreign/ValueLayout$OfLong;");
        }
        else if (type == ScalarType.UTF16) {
            if (clazz != char.class) throw new IllegalArgumentException("Illegal mapping type; expected char");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_CHAR",
                    "Ljava/lang/foreign/ValueLayout$OfChar;");
        }
        else if (type == ScalarType.ADDRESS) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/foreign/ValueLayout", "ADDRESS",
                    "Ljava/lang/foreign/AddressLayout;");
        }
    }

    private static void dumpLoadOpcode(MethodVisitor methodVisitor, Class<?> clazz, int index) {
        int opcode;
        if (clazz == boolean.class) opcode = Opcodes.ILOAD;
        else if (clazz == char.class) opcode = Opcodes.ILOAD;
        else if (clazz == byte.class) opcode = Opcodes.ILOAD;
        else if (clazz == short.class) opcode = Opcodes.ILOAD;
        else if (clazz == int.class) opcode = Opcodes.ILOAD;
        else if (clazz == long.class) opcode = Opcodes.LLOAD;
        else if (clazz == float.class) opcode = Opcodes.FLOAD;
        else if (clazz == double.class) opcode = Opcodes.DLOAD;
        else opcode = Opcodes.ALOAD;
        methodVisitor.visitVarInsn(opcode, index);
    }

    private static void dumpReturnOpcode(MethodVisitor methodVisitor, Class<?> clazz) {
        int opcode;
        if (clazz == void.class) opcode = Opcodes.RETURN;
        else if (clazz == boolean.class) opcode = Opcodes.IRETURN;
        else if (clazz == char.class) opcode = Opcodes.IRETURN;
        else if (clazz == byte.class) opcode = Opcodes.IRETURN;
        else if (clazz == short.class) opcode = Opcodes.IRETURN;
        else if (clazz == int.class) opcode = Opcodes.IRETURN;
        else if (clazz == long.class) opcode = Opcodes.LRETURN;
        else if (clazz == float.class) opcode = Opcodes.FRETURN;
        else if (clazz == double.class) opcode = Opcodes.DRETURN;
        else opcode = Opcodes.ARETURN;
        methodVisitor.visitInsn(opcode);
    }

    private static void dumpForeignType(MethodVisitor methodVisitor, Class<?> clazz, ForeignType type) {
        Objects.requireNonNull(type);
        String name;
        if (type == ScalarType.INT8) {
            if (clazz != byte.class) throw new IllegalArgumentException("Illegal mapping type; expected byte");
            name = "INT8";
        }
        else if (type == ScalarType.CHAR) {
            if (clazz != byte.class) throw new IllegalArgumentException("Illegal mapping type; expected byte");
            name = "CHAR";
        }
        else if (type == ScalarType.INT16) {
            if (clazz != short.class) throw new IllegalArgumentException("Illegal mapping type; expected short");
            name = "INT16";
        }
        else if (type == ScalarType.INT32) {
            if (clazz != int.class) throw new IllegalArgumentException("Illegal mapping type; expected int");
            name = "INT32";
        }
        else if (type == ScalarType.INT64) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            name = "INT64";
        }
        else if (type == ScalarType.WCHAR) {
            if (clazz != int.class) throw new IllegalArgumentException("Illegal mapping type; expected int");
            name = "WCHAR";
        }
        else if (type == ScalarType.SHORT) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            name = "SHORT";
        }
        else if (type == ScalarType.INT) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            name = "INT";
        }
        else if (type == ScalarType.LONG) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            name = "LONG";
        }
        else if (type == ScalarType.SIZE) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            name = "SIZE";
        }
        else if (type == ScalarType.FLOAT) {
            if (clazz != float.class) throw new IllegalArgumentException("Illegal mapping type; expected float");
            name = "FLOAT";
        }
        else if (type == ScalarType.DOUBLE) {
            if (clazz != double.class) throw new IllegalArgumentException("Illegal mapping type; expected double");
            name = "DOUBLE";
        }
        else if (type == ScalarType.BOOLEAN) {
            if (clazz != boolean.class) throw new IllegalArgumentException("Illegal mapping type; expected boolean");
            name = "BOOLEAN";
        }
        else if (type == ScalarType.UTF16) {
            if (clazz != char.class) throw new IllegalArgumentException("Illegal mapping type; expected char");
            name = "UTF16";
        }
        else if (type == ScalarType.ADDRESS) {
            if (clazz != long.class) throw new IllegalArgumentException("Illegal mapping type; expected long");
            name = "ADDRESS";
        }
        else throw new IllegalArgumentException("Unsupported type");
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/ScalarType", name, "Lmultiffi/ffi/ScalarType;");
    }

    private static String getMethodDescriptor(Method method, boolean saveErrno, boolean addReturnMemoryParameter) {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        if (addReturnMemoryParameter) appendDescriptor(SegmentAllocator.class, builder);
        if (saveErrno) appendDescriptor(MemorySegment.class, builder);
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = (addReturnMemoryParameter ? 1 : 0); i < parameterTypes.length; i ++) {
            appendDescriptor(parameterTypes[i], builder);
        }
        builder.append(')');
        appendDescriptor(method.getReturnType(), builder);
        return builder.toString();
    }

    private static void appendDescriptor(Class<?> clazz, StringBuilder stringBuilder) {
        while (clazz.isArray()) {
            stringBuilder.append('[');
            clazz = clazz.getComponentType();
        }
        if (clazz.isPrimitive()) {
            char descriptor;
            if (clazz == int.class) descriptor = 'I';
            else if (clazz == void.class) descriptor = 'V';
            else if (clazz == boolean.class) descriptor = 'Z';
            else if (clazz == byte.class) descriptor = 'B';
            else if (clazz == char.class) descriptor = 'C';
            else if (clazz == short.class) descriptor = 'S';
            else if (clazz == double.class) descriptor = 'D';
            else if (clazz == float.class) descriptor = 'F';
            else if (clazz == long.class) descriptor = 'J';
            else throw new IllegalArgumentException("Unsupported type");
            stringBuilder.append(descriptor);
        }
        else stringBuilder.append('L').append(Type.getInternalName(clazz)).append(';');
    }

    private static final Map<Integer, FFMFunctionHandle.Invoker> INVOKERS = new HashMap<>(255);
    private static final String[] INVOKER_EXCEPTIONS = new String[] { "java/lang/Throwable" };
    private static final String[] INVOKER_INTERFACES = new String[] { "io/github/multiffi/ffi/FFMFunctionHandle$Invoker" };
    public static FFMFunctionHandle.Invoker generateInvoker(int parameterCount) {
        if (parameterCount > 255) throw new IllegalArgumentException("parameter limit exceeded: " + parameterCount);
        if (!INVOKERS.containsKey(parameterCount)) synchronized (INVOKERS) {
            if (!INVOKERS.containsKey(parameterCount)) {
                ClassLoader classLoader = FFMFunctionHandle.Invoker.class.getClassLoader();
                String proxyName = "io.github.multiffi.FFMFunctionHandle$Invoker$" + parameterCount;
                String proxyInternalName = proxyName.replace('.', '/');

                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

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
                for (int i = 0; i < parameterCount; i ++) {
                    invoke.visitVarInsn(Opcodes.ALOAD, 2);
                    invoke.visitIntInsn(Opcodes.SIPUSH, i);
                    invoke.visitInsn(Opcodes.AALOAD);
                    invoke.visitVarInsn(Opcodes.ASTORE, 3 + i);
                }
                invoke.visitVarInsn(Opcodes.ALOAD, 1);
                for (int i = 0; i < parameterCount; i ++) {
                    invoke.visitVarInsn(Opcodes.ALOAD, 3 + i);
                }
                invoke.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke",
                        "(" + "Ljava/lang/Object;".repeat(parameterCount) + ")Ljava/lang/Object;", false);
                invoke.visitInsn(Opcodes.ARETURN);
                invoke.visitMaxs(0, 0);
                invoke.visitEnd();

                classWriter.visitEnd();
                try {
                    INVOKERS.put(parameterCount, (FFMFunctionHandle.Invoker) FFMUtil.IMPL_LOOKUP
                            .findConstructor(FFMUtil.defineClass(classLoader, proxyName, classWriter.toByteArray()),
                                    MethodType.methodType(void.class)).invoke());
                } catch (RuntimeException | Error ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        return INVOKERS.get(parameterCount);
    }

}
