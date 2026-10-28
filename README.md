# 民泊人狼

古民家の民泊を舞台にした、Among Us 風のマイクラ人狼 mod（Fabric 1.21.1）。

## 遊び方（かんたん）
0. 基本マップを使うなら `/minpaku build`（または管理人の鍵の画面右上のボタン）で、目の前に古民家民泊が建つ
1. クリエイティブタブ「民泊人狼」から、マップ用のブロックを置く
   - 玄関マット（スタート地点）、呼び鈴（緊急ボタン）、床下点検口（ベント）、配電盤、ガスの元栓、ボイラー
   - タスクのブロック（配線盤・予約台帳・布団・かまど・風呂釜・薪割り台・畑・ほこり）
2. 「管理人の鍵」を右クリック → 設定を決めて「ゲーム開始」（OPのみ）
   - コマンドでも可: `/minpaku start` `/minpaku stop` `/minpaku key`

## 開発
- `tools/blender/minpaku_models.py` … ブロック・アイテムの3DモデルをBlenderで作って書き出す（assets/minpaku/mesh/）
  - 書き出したJSONを `.minecraft/config/minpaku/mesh/` に入れて F3+T で、jarを作り直さずに見た目を試せる
- `tools/models/gen_models.py` … （旧）箱で作ったモデル。今は壊したときの粒のテクスチャだけ使用
- `tools/models/gen_textures.py` … オリジナルのテクスチャを作る
- 役職の追加: `game/Role.java`、設定の追加: `game/GameSettings.java`、タスクの追加: `game/MarkerType.java`
