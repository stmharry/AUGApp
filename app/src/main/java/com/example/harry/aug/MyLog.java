package com.example.harry.aug;

import android.util.Log;

/**
 * Created by harry on 9/17/15.
 */
public class MyLog {
    private static final int MAX_LENGTH = 50;

    private static void Array(String TAG, String name, float[] x) {
        Array(TAG, name, x, 1);
    }

    private static void Array(String TAG, String name, float[] x, int skip) {
        Array(TAG, name, x, skip, x.length);
    }

    private static void Array(String TAG, String name, float[] x, int skip, int length) {
        int maxIndexLength = (int) Math.floor(Math.log10(length));
        String format = "%s[%" + maxIndexLength + "d]: %sx (%.2e)\n";

        for(int i = 0; i < length; i+= skip) {
            String str = String.format(format, name, i, new String(new char[(int)(x[i] * MAX_LENGTH)]).replace("\0", " "), x[i]);
            Log.d(TAG, str);
        }
    }
}
