package multiffi;

import java.lang.reflect.Method;

public interface CallOptionVisitor {

    default long visitAddress(Method method) {
        return Foreign.getSymbol(method.getName());
    }
    default int visitFirstVariadicArgument(Method method) {
        return -1;
    }
    ForeignType visitReturnType(Method method);
    ForeignType[] visitParameterTypes(Method method);
    default CallOption[] visitCallOptions(Method method) {
        return Util.EMPTY_CALL_OPTION_ARRAY;
    }

}
