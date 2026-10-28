package com.minpaku.client;

import net.minecraft.util.Mth;

/** 演出用のキーフレーム */
final class Keyframes {
    private Keyframes() {
    }

    /** なめらかにつなぐキーフレーム。keys = {時間, 値, 時間, 値, ...} */
    static float track(float t, float... keys) {
        if (t <= keys[0]) {
            return keys[1];
        }
        for (int i = 0; i + 3 < keys.length; i += 2) {
            float t0 = keys[i];
            float t1 = keys[i + 2];
            if (t <= t1) {
                float f = (t - t0) / Math.max(0.0001f, t1 - t0);
                f = f * f * (3 - 2 * f);
                return Mth.lerp(f, keys[i + 1], keys[i + 3]);
            }
        }
        return keys[keys.length - 1];
    }
}
