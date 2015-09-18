package com.example.harry.aug;

/**
 * Created by harry on 9/14/15.
 */
public class FFT { // TODO: merge FFT from Analyzer
    private int n;
    private int m;
    private int k;
    private float[] cos;
    private float[] sin;

    public FFT(int n) {
        this.m = (n == 0)? 0 : (32 - Integer.numberOfLeadingZeros(n - 1));
        this.n = n = 1 << m;
        this.k = n / 2 + 1;

        cos = new float[n / 2];
        sin = new float[n / 2];
        for (int i = 0; i < n / 2; i++) {
            cos[i] = (float) (Math.cos(-2 * Math.PI * i / n));
            sin[i] = (float) (Math.sin(-2 * Math.PI * i / n));
        }
    }

    public int getN() {
        return n;
    }

    public int getM() {
        return m;
    }

    public int getK() {
        return k;
    }

    private void reverse(float[] x, float[] y) {
        // TODO: bit reversal can only be disabled when compacting properly (please do this!)
        int i, j, n1, n2;
        float t1;

        j = 0;
        n2 = n / 2;
        for (i = 1; i < n - 1; i++) {
            n1 = n2;
            while (j >= n1) {
                j = j - n1;
                n1 = n1 / 2;
            }
            j = j + n1;

            if (i < j) {
                t1 = x[i];
                x[i] = x[j];
                x[j] = t1;
                t1 = y[i];
                y[i] = y[j];
                y[j] = t1;
            }
        }
    }

    public void fft(float[] x, float[] y, boolean reverse) {
        if ((x.length != n) || (y.length != n)) {
            throw new RuntimeException();
        }

        int i, j, k, n1, n2, a;
        float c, s, t1, t2;

        if(reverse) {
            reverse(x, y);
        }

        // FFT
        n1 = 0;
        n2 = 1;

        for (i = 0; i < m; i++) {
            n1 = n2;
            n2 = n2 + n2;
            a = 0;

            for (j = 0; j < n1; j++) {
                c = cos[a];
                s = sin[a];
                a += 1 << (m - i - 1);

                for (k = j; k < n; k = k + n2) {
                    t1 = c * x[k + n1] - s * y[k + n1];
                    t2 = s * x[k + n1] + c * y[k + n1];
                    x[k + n1] = x[k] - t1;
                    y[k + n1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }
    }

    public void ifft(float[] x, float[] y, boolean reverse) {
        if ((x.length != n) || (y.length != n)) {
            throw new RuntimeException();
        }

        int i, j, k, n1, n2, a;
        float c, s, t1, t2;

        if(reverse) {
            reverse(x, y);
        }

        // FFT
        n1 = 0;
        n2 = 1;

        for (i = 0; i < m; i++) {
            n1 = n2;
            n2 = n2 + n2;
            a = 0;

            for (j = 0; j < n1; j++) {
                c = cos[a];
                s = sin[a];
                a += 1 << (m - i - 1);

                for (k = j; k < n; k = k + n2) {
                    t1 = c * x[k + n1] + s * y[k + n1];
                    t2 = s * x[k + n1] - c * y[k + n1];
                    x[k + n1] = x[k] - t1;
                    y[k + n1] = y[k] + t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] - t2;
                }
            }
        }
    }

    public void scale(float[] x, float[] y) {
        if ((x.length != n) || (y.length != n)) {
            throw new RuntimeException();
        }

        for (int i = 0; i < n; i++) {
            x[i] /= n;
            y[i] /= n;
        }
    }
}