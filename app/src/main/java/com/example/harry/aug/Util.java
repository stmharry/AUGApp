package com.example.harry.aug;

import java.util.Arrays;

/**
 * Created by harry on 9/14/15.
 */
public class Util {
    public static int nextpow2(int x) {
        return (x == 0) ? 0 : (32 - Integer.numberOfLeadingZeros(x - 1));
    }

    public static float bpmToFreq(float x) {
        return (x / 60);
    }

    public static float freqToBPM(float x) {
        return (x * 60);
    }

    public static float smallestMagnitudeCoterminalAngle(float x) {
        return (float)(x - Math.round(x / (2 * Math.PI)) * (2 * Math.PI));
    }

    public static float radToDeg(float x) {
        return (float)(x / Math.PI * 180);
    }

    //

    public static float mean(float[] in) {
        return mean(in, 0);
    }

    public static float mean(float[] in, int offset) {
        return (sum(in, offset) / (in.length - offset));
    }

    public static float sum(float[] in) {
        return sum(in, 0);
    }

    public static float sum(float[] in, int offset) {
        int length = in.length;

        float sum = 0;
        for(int i = offset; i < length; i++) {
            sum += in[i];
        }
        return sum;
    }

    public static float sumOfSquared(float[] in) {
        float sum = 0;
        for(float num: in) {
            sum += num * num;
        }
        return sum;
    }

    public static float maxValue(float[] in, int offset) {
        float[] out = max(in, offset);
        return out[0];
    }

    public static int maxIndex(float[] in, int offset) {
        float[] out = max(in, offset);
        return (int) out[1];
    }

    public static float[] max(float[] in, int offset) {
        int length = in.length;

        float max = in[offset];
        int maxIndex = offset;
        for (int i = offset; i < length; i++) {
            if (in[i] > max) {
                max = in[i];
                maxIndex = i;
            }
        }
        return new float[]{max, maxIndex};
    }

    public static float minValue(float[] in, int offset) {
        float[] out = min(in, offset);
        return out[0];
    }

    public static int minIndex(float[] in, int offset) {
        float[] out = min(in, offset);
        return (int) out[1];
    }

    public static float[] min(float[] in, int offset) {
        int length = in.length;

        float min = in[offset];
        int minIndex = offset;
        for (int i = offset; i < length; i++) {
            if (in[i] < min) {
                min = in[i];
                minIndex = i;
            }
        }
        return new float[]{min, minIndex};
    }

    //

    public static float[] expand(float[] in, boolean pos) {
        int inLength = in.length;
        int outLength = 2 * (inLength - 1);

        float[] out = new float[outLength];

        for (int i = 0; i < inLength; i++) {
            out[i] = in[i];
        }

        for (int i = inLength; i < outLength; i++) {
            out[i] = pos ? in[outLength - i] : -in[outLength - i];
        }

        return out;
    }

    public static float[] compact(float[] in) {
        return Arrays.copyOf(in, in.length / 2 + 1);
    }

    public static float[] downSample(float[] in, int ratio) {
        if (ratio == 1) {
            return in;
        }

        int outSize = in.length / ratio;
        float[] out = new float[outSize];
        for (int i = 0; i < outSize; i++) {
            out[i] = in[i * ratio];
        }
        return out;
    }

    //

    public static void center(float[] in, int length) {
        float mean = sum(in) / length;

        for(int i = 0; i < length; i++) {
            in[i] = in[i] - mean;
        }
    }

    public static void normalize(float[] in) {
        int length = in.length;

        float min = minValue(in, 0);
        float max = maxValue(in, 0);

        for(int i = 0; i < length; i++) {
            in[i] = (in[i] - min) / (max - min);
        }
    }

    public static void whiten(float[] in) {
        int length = in.length;

        float mean = sum(in) / length;
        float meanOfSquared = sumOfSquared(in) / length;
        float std = (float) Math.sqrt(meanOfSquared - mean * mean);

        for(int i = 0; i < length; i++) {
            in[i] = (in[i] - mean) / std;
        }
    }

    public static void cart2pol(float[] r, float[] t, float[] x, float[] y) {
        int length = x.length;

        for (int i = 0; i < length; i++) {
            r[i] = (float) (Math.sqrt(x[i] * x[i] + y[i] * y[i]));
            t[i] = (float) (Math.atan2(y[i], x[i]));
        }
    }

    public static void pol2cart(float[] x, float[] y, float[] r, float[] t) {
        int length = r.length;

        for (int i = 0; i < length; i++) {
            x[i] = r[i] * (float) (Math.cos(t[i]));
            y[i] = r[i] * (float) (Math.sin(t[i]));
        }
    }
}
