package org.example.demofx;

import java.util.Arrays;
import java.util.List;

public class ZhoWrapper {
    static {
        System.loadLibrary("opencc_fmmseg_capi"); // Load the DLL
    }

    // Native methods
    private native long opencc_new();

    private native String opencc_convert(long instance, String config, String input, boolean punctuation);

//    private native boolean opencc_get_parallel(long instance);

//    private native void opencc_set_parallel(long instance, boolean is_parallel);

    private native int opencc_zho_check(long instance, String input);

    private native void opencc_free(long instance);

//    private native void opencc_string_free(String ptr);

    private final long instance;

    private static final List<String> configList = Arrays.asList(
            "s2t", "t2s", "s2tw", "tw2s", "s2twp", "tw2sp", "s2hk", "hk2s", "t2tw", "t2twp", "t2hk", "tw2t", "tw2tp",
            "hk2t", "t2jp",
            "jp2t");

    // Constructor
    public ZhoWrapper() {
        instance = opencc_new();
    }

    // Getter for instance
    public long getInstance() {
        return instance;
    }

    public String convert(long instance, String input, String config,  boolean punctuation) {
        if (input.isEmpty()) {
            return "";
        }
        if (!configList.contains(config)) {
            config = "s2t";
        }

        return opencc_convert(instance, config, input, punctuation);
    }

//    public boolean isParallel(long instance) {
//        return opencc_get_parallel(instance);
//    }

//    public void setParallel(long instance, boolean isParallel) {
//        opencc_set_parallel(instance, isParallel);
//    }

    public int zho_check(long instance, String input) {
        return opencc_zho_check(instance, input);
    }

    public void releaseInstance(long instance) {
        if (instance != 0) {
            opencc_free(instance);
            // Reset the instance variable after freeing
        }
    }
}
