package com.minpaku.client;

/**
 * 演出用のキーフレーム。
 * キーとキーのあいだを「速さがつながる」3次曲線（単調エルミート）でつなぐので、
 * キーの所で動きが止まらず、行き過ぎ（はね返り）もしない。最初と最後はゆっくり。
 */
public final class Keyframes {
    private Keyframes() {
    }

    /** keys = {時間, 値, 時間, 値, ...} */
    public static float track(float t, float... keys) {
        int n = keys.length / 2;
        if (n == 0) {
            return 0f;
        }
        if (n == 1 || t <= keys[0]) {
            return keys[1];
        }
        if (t >= keys[(n - 1) * 2]) {
            return keys[(n - 1) * 2 + 1];
        }
        int k = 0;
        while (k < n - 2 && t > keys[(k + 1) * 2]) {
            k++;
        }
        float t0 = keys[k * 2];
        float v0 = keys[k * 2 + 1];
        float t1 = keys[(k + 1) * 2];
        float v1 = keys[(k + 1) * 2 + 1];
        float h = Math.max(1e-4f, t1 - t0);
        float m0 = slope(keys, n, k);
        float m1 = slope(keys, n, k + 1);
        float s = (t - t0) / h;
        float s2 = s * s;
        float s3 = s2 * s;
        float h00 = 2 * s3 - 3 * s2 + 1;
        float h10 = s3 - 2 * s2 + s;
        float h01 = -2 * s3 + 3 * s2;
        float h11 = s3 - s2;
        return h00 * v0 + h10 * h * m0 + h01 * v1 + h11 * h * m1;
    }

    /** k 番目のキーでの傾き（両はしは 0 ＝ゆっくり始まりゆっくり終わる） */
    private static float slope(float[] keys, int n, int k) {
        if (k <= 0 || k >= n - 1) {
            return 0f;
        }
        float dPrev = secant(keys, k - 1);
        float dNext = secant(keys, k);
        if (dPrev * dNext <= 0f) {
            return 0f;    // 向きが変わる所では止める（行き過ぎない）
        }
        float m = (dPrev + dNext) / 2f;
        float lim = 3f * Math.min(Math.abs(dPrev), Math.abs(dNext));
        return Math.abs(m) > lim ? Math.signum(m) * lim : m;
    }

    private static float secant(float[] keys, int k) {
        float dt = keys[(k + 1) * 2] - keys[k * 2];
        return dt <= 1e-4f ? 0f : (keys[(k + 1) * 2 + 1] - keys[k * 2 + 1]) / dt;
    }
}
