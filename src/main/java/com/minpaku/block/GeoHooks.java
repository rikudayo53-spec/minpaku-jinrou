package com.minpaku.block;

import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

/** 家具のアニメーションをどう流すか（クライアントが handler を差しかえる。サーバーでは止めたまま） */
public final class GeoHooks {
    private GeoHooks() {
    }

    public interface Handler {
        PlayState handle(MarkerBlockEntity be, AnimationState<MarkerBlockEntity> state, String kind);
    }

    public static Handler handler = (be, state, kind) -> PlayState.STOP;
}
