package io.github.multiffi.ffi;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.NativeLibrary;
import com.sun.jna.SymbolProvider;
import multiffi.ffi.CallOption;
import multiffi.ffi.FunctionOptionVisitor;
import multiffi.ffi.ForeignType;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
import multiffi.ffi.StandardCallOption;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class JNAASMRuntime {

    private JNAASMRuntime() {
        throw new AssertionError("No io.github.multiffi.ffi.JNAASMRuntime instances for you!");
    }

    public static NativeLibrary getNativeLibrary(Map<String, Long> functionMap) {
        return NativeLibrary.getInstance(null, Collections.singletonMap(Library.OPTION_SYMBOL_PROVIDER,
                (SymbolProvider) (handle, name, parent) -> functionMap.get(name)));
    }

    public static NativeLibrary getStdCallNativeLibrary(Map<String, Long> functionMap) {
        Map<String, Object> optionMap = new HashMap<>(2);
        optionMap.put(Library.OPTION_SYMBOL_PROVIDER, (SymbolProvider) (handle, name, parent) -> functionMap.get(name));
        optionMap.put(Library.OPTION_CALLING_CONVENTION, Function.ALT_CONVENTION);
        return NativeLibrary.getInstance(null, Collections.unmodifiableMap(optionMap));
    }

    private static final Method isHiddenMethod;
    private static final Method isSealedMethod;
    static {
        Method method;
        try {
            method = Class.class.getDeclaredMethod("isHidden");
        } catch (NoSuchMethodException e) {
            method = null;
        }
        isHiddenMethod = method;
        try {
            method = Class.class.getDeclaredMethod("isSealed");
        } catch (NoSuchMethodException e) {
            method = null;
        }
        isSealedMethod = method;
    }
    private static boolean isHidden(Class<?> clazz) {
        if (isHiddenMethod == null) return false;
        else {
            try {
                return (boolean) JNAUtil.invoke(clazz, isHiddenMethod);
            }
            catch (Throwable e) {
                return false;
            }
        }
    }
    private static boolean isSealed(Class<?> clazz) {
        if (isSealedMethod == null) return false;
        else {
            try {
                return (boolean) JNAUtil.invoke(clazz, isSealedMethod);
            }
            catch (Throwable e) {
                return false;
            }
        }
    }
    private static void checkProxyClasses(ClassLoader classLoader, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            if (!clazz.isInterface()) throw new IllegalArgumentException(clazz.getName() + " is not an interface");
            if (isHidden(clazz)) throw new IllegalArgumentException(clazz.getName() + " is a hidden interface");
            if (isSealed(clazz)) throw new IllegalArgumentException(clazz.getName() + " is a sealed interface");
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
        else if (classes.length > 65535) throw new IllegalArgumentException("interface limit exceeded: " + classes.length);
        checkProxyClasses(classLoader, classes);
        String proxyName = "multiffi.ffi.jna.Proxy$" + nextSerialNumber.getAndIncrement();
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

        Map<String, Long> directMethodMap = new HashMap<>();
        for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                if (method.isDefault() || method.getDeclaringClass() == Object.class || Modifier.isStatic(method.getModifiers())) continue;
                String methodName = method.getName();
                String methodFieldName = "function" + Integer.toHexString(method.hashCode());
                CallOption[] options = functionOptionVisitor.visitCallOptions(method);
                boolean dyncall = false;
                boolean stdcall = false;
                boolean saveErrno = false;
                if (options != null) {
                    for (CallOption option : options) {
                        if (option.equals(StandardCallOption.DYNCALL)) dyncall = true;
                        else if (option.equals(StandardCallOption.SAVE_ERRNO)) saveErrno = true;
                        else if (option.equals(StandardCallOption.STDCALL)) stdcall = true;
                        else if (!option.equals(StandardCallOption.TRIVIAL) && !option.equals(StandardCallOption.CRITICAL))
                            throw new IllegalArgumentException(option + " not supported");
                    }
                    if (!JNAUtil.STDCALL_SUPPORTED) stdcall = false;
                }

                int firstVarArgIndex = functionOptionVisitor.visitFirstVarArgIndex(method);

                ForeignType[] parameterForeignTypes = functionOptionVisitor.visitParameterTypes(method);
                if (parameterForeignTypes == null || parameterForeignTypes.length == 1 && parameterForeignTypes[0] == null)
                    parameterForeignTypes = Util.EMPTY_FOREIGN_TYPE_ARRAY;
                else parameterForeignTypes = parameterForeignTypes.clone();
                firstVarArgIndex = firstVarArgIndex >= 0 ? firstVarArgIndex : (dyncall ? parameterForeignTypes.length : -1);

                ForeignType returnForeignType = functionOptionVisitor.visitReturnType(method);
                boolean addReturnMemoryParameter = returnForeignType != null && returnForeignType.isCompound();
                Class<?>[] parameterTypes = method.getParameterTypes();
                Class<?> returnType = method.getReturnType();
                if (parameterForeignTypes.length + (addReturnMemoryParameter ? 1 : 0) + (dyncall ? 1 : 0) != parameterTypes.length)
                    throw new ArrayIndexOutOfBoundsException("length mismatch");
                if (addReturnMemoryParameter && parameterTypes[0] != MemoryHandle.class)
                    throw new IllegalArgumentException("Illegal mapping type; expected class MemoryHandle");
                if (dyncall && !parameterTypes[parameterTypes.length - 1].isArray())
                    throw new IllegalArgumentException("Last argument must be array as variadic arguments");

                boolean hasCompound = addReturnMemoryParameter;
                for (int i = 0; i < parameterForeignTypes.length; i ++) {
                    Util.checkType(Objects.requireNonNull(parameterForeignTypes[i]), parameterTypes[i + (addReturnMemoryParameter ? 1 : 0)]);
                    if (!hasCompound && parameterForeignTypes[i].isCompound()) hasCompound = true;
                }

                if (dyncall || stdcall || hasCompound || firstVarArgIndex != -1) {
                    classWriter.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                            methodFieldName, "Lmultiffi/ffi/FunctionHandle;", null, null).visitEnd();

                    visitLdcInsn(classInit, parameterForeignTypes.length);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "multiffi/ffi/ForeignType");
                    classInit.visitVarInsn(Opcodes.ASTORE, 1);
                    for (int i = 0; i < parameterForeignTypes.length; i ++) {
                        ForeignType parameterForeignType = parameterForeignTypes[i];
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
                        else dumpForeignType(classInit, parameterForeignType);
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    int optionCount = 0;
                    if (dyncall) optionCount ++;
                    if (stdcall) optionCount ++;
                    if (saveErrno) optionCount ++;
                    visitLdcInsn(classInit, optionCount);
                    classInit.visitTypeInsn(Opcodes.ANEWARRAY, "multiffi/ffi/CallOption");
                    classInit.visitVarInsn(Opcodes.ASTORE, 2);
                    int optionIndex = 0;
                    if (dyncall) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        visitLdcInsn(classInit, optionIndex ++);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/StandardCallOption",
                                "DYNCALL", "Lmultiffi/ffi/StandardCallOption;");
                        classInit.visitInsn(Opcodes.AASTORE);
                    }
                    if (stdcall) {
                        classInit.visitVarInsn(Opcodes.ALOAD, 2);
                        visitLdcInsn(classInit, optionIndex ++);
                        classInit.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/StandardCallOption",
                                "STDCALL", "Lmultiffi/ffi/StandardCallOption;");
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

                    classInit.visitTypeInsn(Opcodes.NEW, "io/github/multiffi/ffi/JNAFunctionHandle");
                    classInit.visitInsn(Opcodes.DUP);
                    visitLdcInsn(classInit, functionOptionVisitor.visitAddress(method));
                    visitLdcInsn(classInit, firstVarArgIndex);

                    if (returnForeignType != null && returnForeignType.isCompound()) classInit.visitVarInsn(Opcodes.ALOAD, 3);
                    else dumpForeignType(classInit, returnForeignType);
                    classInit.visitVarInsn(Opcodes.ALOAD, 1);
                    classInit.visitVarInsn(Opcodes.ALOAD, 2);
                    classInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "io/github/multiffi/ffi/JNAFunctionHandle", "<init>",
                            "(JILmultiffi/ffi/ForeignType;[Lmultiffi/ffi/ForeignType;[Lmultiffi/ffi/CallOption;)V", false);
                    classInit.visitFieldInsn(Opcodes.PUTSTATIC, proxyInternalName, methodFieldName,
                            "Lmultiffi/ffi/FunctionHandle;");

                    int methodMaxLocals = parameterTypes.length + 1;
                    for (Class<?> parameterType : parameterTypes) {
                        if (parameterType == long.class || parameterType == double.class) methodMaxLocals ++;
                    }
                    MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, methodName, Type.getMethodDescriptor(method),
                            null, null);

                    methodVisitor.visitCode();

                    visitLdcInsn(methodVisitor, parameterTypes.length);
                    methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                    methodVisitor.visitVarInsn(Opcodes.ASTORE, methodMaxLocals);

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
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, methodMaxLocals + 1);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, methodMaxLocals);
                            visitLdcInsn(methodVisitor, i);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, methodMaxLocals + 1);
                        }
                        else {
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, methodMaxLocals);
                            visitLdcInsn(methodVisitor, i);
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1 + index ++);
                        }
                        methodVisitor.visitInsn(Opcodes.AASTORE);
                    }

                    methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, proxyInternalName, methodFieldName, "Lmultiffi/ffi/FunctionHandle;");
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, methodMaxLocals);
                    if (returnForeignType == ScalarType.BOOLEAN)
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
                    String methodDescriptor = Type.getMethodDescriptor(method);
                    directMethodMap.put(methodFieldName, functionOptionVisitor.visitAddress(method));

                    classWriter.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_NATIVE,
                            methodFieldName, methodDescriptor, null, null).visitEnd();

                    MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, methodName, methodDescriptor,
                            null, null);
                    methodVisitor.visitCode();

                    Label startLabel, endLabel, handlerLabel;
                    if (saveErrno) {
                        startLabel = new Label();
                        endLabel = new Label();
                        handlerLabel = new Label();
                        methodVisitor.visitLabel(startLabel);
                    }
                    else startLabel = endLabel = handlerLabel = null;

                    int index = 0;
                    for (Class<?> parameterType : parameterTypes) {
                        dumpLoadOpcode(methodVisitor, parameterType, 1 + index ++);
                        if (parameterType == long.class || parameterType == double.class) index ++;
                    }
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, proxyInternalName, methodFieldName, methodDescriptor, false);
                    if (saveErrno) {
                        methodVisitor.visitVarInsn(Opcodes.ASTORE, parameterTypes.length);
                        methodVisitor.visitLabel(endLabel);
                        methodVisitor.visitLabel(handlerLabel);
                        methodVisitor.visitTryCatchBlock(startLabel, endLabel, handlerLabel, "java/lang/Throwable");
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/multiffi/ffi/JNALastErrno",
                                "dump", "()V", false);
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, parameterTypes.length);
                    }

                    dumpReturnOpcode(methodVisitor, returnType);
                    methodVisitor.visitMaxs(0, 0);
                    methodVisitor.visitEnd();
                }
            }
        }

        if (!directMethodMap.isEmpty()) {
            classInit.visitTypeInsn(Opcodes.NEW, "java/util/HashMap");
            classInit.visitInsn(Opcodes.DUP);
            visitLdcInsn(classInit, directMethodMap.size());
            classInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "(I)V", false);
            classInit.visitVarInsn(Opcodes.ASTORE, 1);

            for (Map.Entry<String, Long> entry : directMethodMap.entrySet()) {
                visitLdcInsn(classInit, entry.getValue());
                classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                classInit.visitVarInsn(Opcodes.ASTORE, 2);

                classInit.visitVarInsn(Opcodes.ALOAD, 1);
                visitLdcInsn(classInit, entry.getKey());
                classInit.visitVarInsn(Opcodes.ALOAD, 2);

                classInit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/HashMap",
                        "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
            }

            classInit.visitVarInsn(Opcodes.ALOAD, 1);
            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/multiffi/ffi/JNAASMRuntime", "getNativeLibrary",
                    "(Ljava/util/Map;)Lcom/sun/jna/NativeLibrary;", false);
            classInit.visitVarInsn(Opcodes.ASTORE, 1);
            visitLdcInsn(classInit, Type.getType("L" + proxyInternalName + ";"));
            classInit.visitVarInsn(Opcodes.ALOAD, 1);
            classInit.visitMethodInsn(Opcodes.INVOKESTATIC, "com/sun/jna/Native",
                    "register", "(Ljava/lang/Class;Lcom/sun/jna/NativeLibrary;)V", false);
        }

        classInit.visitInsn(Opcodes.RETURN);
        classInit.visitMaxs(0, 0);
        classInit.visitEnd();

        classWriter.visitEnd();
        try {
            return JNAUtil.newInstance(JNAUtil.defineClass(classLoader, proxyName, classWriter.toByteArray()).getConstructor());
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static void visitLdcInsn(MethodVisitor methodVisitor, Object value) {
        if (value instanceof Long) {
            long lVal = (long) value;
            if (lVal == 0) methodVisitor.visitInsn(Opcodes.LCONST_0);
            else if (lVal == 1) methodVisitor.visitInsn(Opcodes.LCONST_1);
            else methodVisitor.visitLdcInsn(value);
        }
        else if (value instanceof Float) {
            float fVal = (float) value;
            if (fVal == 0) methodVisitor.visitInsn(Opcodes.FCONST_0);
            else if (fVal == 1) methodVisitor.visitInsn(Opcodes.FCONST_1);
            else methodVisitor.visitLdcInsn(value);
        }
        else if (value instanceof Double) {
            double dVal = (double) value;
            if (dVal == 0) methodVisitor.visitInsn(Opcodes.DCONST_0);
            else if (dVal == 1) methodVisitor.visitInsn(Opcodes.DCONST_1);
            else methodVisitor.visitLdcInsn(value);
        }
        else if (value instanceof Number) {
            int iVal = ((Number) value).intValue();
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
                    if (iVal >= Byte.MIN_VALUE && iVal <= Byte.MAX_VALUE) methodVisitor.visitIntInsn(Opcodes.BIPUSH, iVal);
                    else if (iVal >= Short.MIN_VALUE && iVal <= Short.MAX_VALUE) methodVisitor.visitIntInsn(Opcodes.SIPUSH, iVal);
                    else methodVisitor.visitLdcInsn(value);
                    break;
            }
        }
        else if (value == null) methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        else methodVisitor.visitLdcInsn(value);
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

    private static void dumpForeignType(MethodVisitor methodVisitor, ForeignType type) {
        String name;
        if (type == ScalarType.INT8) name = "INT8";
        else if (type == ScalarType.CHAR) name = "CHAR";
        else if (type == ScalarType.INT16) name = "INT16";
        else if (type == ScalarType.INT32) name = "INT32";
        else if (type == ScalarType.INT64) name = "INT64";
        else if (type == ScalarType.WCHAR) name = "WCHAR";
        else if (type == ScalarType.SHORT) name = "SHORT";
        else if (type == ScalarType.INT) name = "INT";
        else if (type == ScalarType.LONG) name = "LONG";
        else if (type == ScalarType.SIZE) name = "SIZE";
        else if (type == ScalarType.FLOAT) name = "FLOAT";
        else if (type == ScalarType.DOUBLE) name = "DOUBLE";
        else if (type == ScalarType.BOOLEAN) name = "BOOLEAN";
        else if (type == ScalarType.UTF16) name = "UTF16";
        else if (type == ScalarType.ADDRESS) name = "ADDRESS";
        else throw new IllegalArgumentException("Unsupported type");
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "multiffi/ffi/ScalarType", name, "Lmultiffi/ffi/ScalarType;");
    }

}
