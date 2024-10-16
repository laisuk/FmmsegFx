package org.example.demofx;

import java.util.Arrays;
import java.util.List;

import opencc.OpenccWrapper;

public class ZhoConverter {
    static {
        System.loadLibrary("OpenccWrapper");
    }

    private static final List<String> configList = Arrays.asList(
            "s2t", "t2s", "s2tw", "tw2s", "s2twp", "tw2sp", "s2hk", "hk2s", "t2tw", "t2twp", "t2hk", "tw2t", "tw2tp",
            "hk2t", "t2jp",
            "jp2t");

    public static String convert(String input, String config, boolean punctuation) {
        if (!configList.contains(config)) {
            return input;
        }
//        System.loadLibrary("ZhoWrapper");
        OpenccWrapper wrapper = new OpenccWrapper();
        long instance = wrapper.getInstance();
        String results = wrapper.convert(instance, input, config, punctuation);
        wrapper.releaseInstance(instance);
        return results;
    }

    public static int zhoCheck(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
//        System.loadLibrary("ZhoWrapper");
        OpenccWrapper wrapper = new OpenccWrapper();
        long instance = wrapper.getInstance();
        int code = wrapper.zhoCheck(instance, text);
        wrapper.releaseInstance(instance);
        return code;
    } // zhoCheck

}
