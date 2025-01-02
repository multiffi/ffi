package io.github.multiffi;

import com.sun.jna.CallbackProxy;
import com.sun.jna.win32.StdCallLibrary;

public interface StdCallCallbackProxy extends StdCallLibrary.StdCallCallback, CallbackProxy {
}
