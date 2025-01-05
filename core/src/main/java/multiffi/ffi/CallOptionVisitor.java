package multiffi.ffi;

import java.lang.reflect.Method;

public interface CallOptionVisitor {

    default long visitAddress(Method method) {
        return Foreign.getSymbolAddress(method.getName());
    }
    default int visitFirstVarArgIndex(Method method) {
        return -1;
    }
    ForeignType visitReturnType(Method method);
    ForeignType[] visitParameterTypes(Method method);
    default CallOption[] visitCallOptions(Method method) {
        return Util.EMPTY_CALL_OPTION_ARRAY;
    }

}
