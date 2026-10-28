package com.minpaku.item;

/**
 * アイテムからクライアントの画面を開くための橋渡し。
 * サーバーにクライアント専用のクラスを読み込ませないよう、クライアント側で中身を差し込む。
 */
public final class ClientBridge {
    public static Runnable openSettings = () -> {
    };
    public static Runnable openSabotage = () -> {
    };

    private ClientBridge() {
    }
}
